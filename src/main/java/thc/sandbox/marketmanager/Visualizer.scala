package thc.sandbox.marketmanager

import akka.actor.ActorRef
import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.util.ActorGroup
import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import thc.sandbox.marketmanager.data.OrderRequest
import thc.sandbox.marketmanager.data.MarketDataType
import info.monitorenter.gui.chart.Chart2D
import collection._
import info.monitorenter.gui.chart.ITrace2D
import info.monitorenter.gui.chart.traces.Trace2DLtd
import java.awt.Color
import scala.util.Random
import thc.sandbox.marketmanager.data.LastPrice
import org.joda.time.DateTime
import javax.swing.JFrame
import info.monitorenter.gui.chart.rangepolicies.ARangePolicy
import info.monitorenter.gui.chart.IAxisLabelFormatter
import info.monitorenter.gui.chart.labelformatters.ALabelFormatter
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import thc.sandbox.marketmanager.data.SimpleRTStockRequest
import info.monitorenter.gui.chart.axis.AxisLinear
import info.monitorenter.gui.chart.IAxis
import info.monitorenter.gui.chart.axis.scalepolicy.AxisScalePolicyAutomaticBestFit
import info.monitorenter.gui.chart.axis.AAxis
import javax.swing.BoxLayout
import javax.swing.JPanel
import thc.sandbox.marketmanager.data.AskPrice
import thc.sandbox.marketmanager.data.BidPrice
import thc.sandbox.marketmanager.data.OrderFilled
import info.monitorenter.gui.chart.IPointPainter
import info.monitorenter.gui.chart.pointpainters.PointPainterDisc
import thc.sandbox.slf4s.Logger

trait Visualizer extends MarketManager {

	implicit val as: ActorSystem
	val vizActor: ActorRef = as.actorOf(Props(creator=new VisualizerActor(orderIdToStock, tickerIdToStock)))
	
	abstract override def subscribe(ag: ActorGroup, dr: DataRequest) {
		ag.add(vizActor)
		super.subscribe(ag, dr)
	}
	
	abstract override def order(ag: ActorGroup, or: OrderRequest): Int = {
		ag.add(vizActor)
		return super.order(ag, or)
	}
	
}

class VisualizerActor(val orderIdToStock: Map[Int, String],
					  val tickerIdToStock: Map[Int, String]) extends Actor with Logger {
		
	//last, ask, bid
	val traces: mutable.Map[String, (ITrace2D, ITrace2D, ITrace2D, ITrace2D)] = mutable.HashMap.empty

	def addLast(id: Int, x: Double, y: Double) {
		logger.debug(s"adding last price point: ($x, $y)")
		(traces getOrElseUpdate(tickerIdToStock(id), createNewTrace(id)))._1.addPoint(x, y)

	}
	def addAsk(id: Int, x: Double, y: Double) {
		logger.debug(s"adding ask price point: ($x, $y)")
		(traces getOrElseUpdate(tickerIdToStock(id), createNewTrace(id)))._2.addPoint(x, y)
	}
	def addBid(id: Int, x: Double, y: Double) {
		logger.debug(s"adding bid price point: ($x, $y)")
		(traces getOrElseUpdate(tickerIdToStock(id), createNewTrace(id)))._3.addPoint(x, y)
	}
	def addOrder(id: Int, x: Double, y: Double) {
		logger.error(s"adding order point: ($x, $y)")
		traces(orderIdToStock(id))._4.setVisible(true)
		traces(orderIdToStock(id))._4.addPoint(x, y)
	}
	
	def receive = {
		case LastPrice(id: Int, price: Double, time: DateTime) => addLast(id, time.getMillis(), price)
		case AskPrice(id: Int, price: Double, time: DateTime)  => addAsk(id, time.getMillis(), price)
		case BidPrice(id: Int, price: Double, time: DateTime)  => addBid(id, time.getMillis(), price)
		case OrderFilled(id: Int, filled: Int, avgFillPrice: Double, time: DateTime) => addOrder(id, time.getMillis(), avgFillPrice)
	}
	
	
	//swing stuff
	val panel = new JPanel();
	panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS))
	
	val frame = new JFrame("Prices")
	frame.setSize(600, 400)
	frame.setContentPane(panel);
	frame.addWindowListener(new WindowAdapter {
		override def windowClosing(e: WindowEvent) = System.exit(0)
	})

	def createNewTrace(id: Int): (ITrace2D, ITrace2D, ITrace2D, ITrace2D) = {
		val chart = new Chart2D();

		val last = new Trace2DLtd(200)
		chart.addTrace(last);
		last.setColor(Color.green)
		tickerIdToStock.get(id) foreach (s => last.setName(s + " last"))
		
		val ask = new Trace2DLtd(200)
		chart.addTrace(ask);
		ask.setColor(Color.red)
		tickerIdToStock.get(id) foreach (s => ask.setName(s + " ask"))
		
		val bid = new Trace2DLtd(200)
		chart.addTrace(bid);
		bid.setColor(Color.black)
		tickerIdToStock.get(id) foreach (s => bid.setName(s + " bid"))
		
		val orders = new Trace2DLtd(100)
		chart.addTrace(orders);
		orders.setColor(Color.blue)
		orders.setPointHighlighter(new PointPainterDisc(5))
		orders.setVisible(false)
		tickerIdToStock.get(id) foreach (s => orders.setName(s + " orders"))
		
		chart.setUseAntialiasing(true)
		chart.getAxisX().setPaintGrid(true)
		chart.getAxisX().getAxisTitle().setTitle("Seconds Ago")
		chart.getAxisX().setFormatter(new LabelFormatterTimeAgo())
		chart.getAxisX().setRangePolicy(new RangePolicyFixed(180*1000))

		chart.getAxisY().setPaintGrid(true)
		chart.getAxisY().getAxisTitle().setTitle("Dollars".reverse) //dont ask
		chart.getAxisY().setRangePolicy(new RangePolicyPadded(0.05))
		
		panel.add("test", chart);
		frame.pack()
		frame.setSize(600, panel.getComponentCount()*200)
		frame.setVisible(true);
		(last, ask, bid, orders)
	}
	

	
}

class RangePolicyFixed(val frame: Double) extends ARangePolicy {
	
	def getMax(chartmin: Double, chartmax: Double): Double = chartmax
	
	def getMin(chartmin: Double, chartmax: Double): Double = chartmax - frame
}

class RangePolicyPadded(val padding: Double) extends ARangePolicy {
	
	def getMax(chartmin: Double, chartmax: Double): Double = chartmax + padding
	
	def getMin(chartmin: Double, chartmax: Double): Double = chartmin - padding
	
}

class LabelFormatterTimeAgo extends ALabelFormatter {
	
	def now: Double = new DateTime().getMillis()
	
	val getMinimumValueShiftForChange: Double = 100
	
	var lastFormatted: Double = 0
	
	def getNextEvenValue(point: Double, ceiling: Boolean): Double = {
		val p: Double = point - (point % 10000)
		if(ceiling)	p + 10000
		else		p
				
	}
	
	def format(point: Double): String = {
		lastFormatted = point
		f"${(now - point)/1000}%2.0f"
	}
	
	def parse(str: String): Number = lastFormatted
}