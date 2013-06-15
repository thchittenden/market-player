package thc.sandbox.marketmanager

import java.awt.Color
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import scala.collection._

import org.joda.time.DateTime

import info.monitorenter.gui.chart.Chart2D
import info.monitorenter.gui.chart.IAxis
import info.monitorenter.gui.chart.ITrace2D
import info.monitorenter.gui.chart.labelformatters.ALabelFormatter
import info.monitorenter.gui.chart.rangepolicies.ARangePolicy
import info.monitorenter.gui.chart.traces.Trace2DLtd
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JPanel
import thc.sandbox.marketmanager.data.DataRequest

trait Visualizer extends MarketManager {


	
}

class VisualizerActor(subscriptions: Map[Int, DataRequest]) {
		
	//last, ask, bid
	val traces: mutable.Map[Int, (ITrace2D, ITrace2D, ITrace2D)] = mutable.HashMap.empty

	def addLast(id: Int, x: Double, y: Double) {
		(traces getOrElseUpdate(id, createNewTrace(id)))._1.addPoint(x, y)
	}
	def addAsk(id: Int, x: Double, y: Double) {
		(traces getOrElseUpdate(id, createNewTrace(id)))._2.addPoint(x, y)
	}
	def addBid(id: Int, x: Double, y: Double) {
		(traces getOrElseUpdate(id, createNewTrace(id)))._3.addPoint(x, y)
	}
	
//	def receive = {
//		case LastPrice(id: Int, price: Double, time: DateTime) => addLast(id, time.getMillis(), price)
//		case AskPrice(id: Int, price: Double, time: DateTime)  => addAsk(id, time.getMillis(), price)
//		case BidPrice(id: Int, price: Double, time: DateTime)  => addBid(id, time.getMillis(), price)
//	}
	
	
	//swing stuff
	val panel = new JPanel();
	panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS))
	
	val frame = new JFrame("Prices")
	frame.setSize(600, 400)
	frame.setContentPane(panel);
	frame.addWindowListener(new WindowAdapter() {
		override def windowClosing(e: WindowEvent) = System.exit(0)
	})

	def createNewTrace(id: Int): (ITrace2D, ITrace2D, ITrace2D) = {
		//TODO make window display range valid for ALL traces
		val last = new Trace2DLtd(200)
		last.setColor(Color.green)
		last.setPhysicalUnits("Time", "Dollars ($)")
		subscriptions.get(id) foreach (dr => last.setName(dr.symbol + " last"))
		
		val ask = new Trace2DLtd(200)
		ask.setColor(Color.red)
		ask.setPhysicalUnits("Time", "Dollars ($)")
		subscriptions.get(id) foreach (dr => ask.setName(dr.symbol + " ask"))
		
		val bid = new Trace2DLtd(200)
		bid.setColor(Color.black)
		bid.setPhysicalUnits("Time", "Dollars ($)")
		subscriptions.get(id) foreach (dr => bid.setName(dr.symbol + " bid"))

		val chart = new Chart2D();
		chart.addTrace(last);
		chart.addTrace(ask);
		chart.addTrace(bid);
		chart.setUseAntialiasing(true)
		chart.getAxisX().setPaintGrid(true)
		chart.getAxisX().getAxisTitle().setTitle("Seconds Ago")
		chart.getAxisX().setFormatter(new LabelFormatterTimeAgo())

		chart.getAxisY().setPaintGrid(true)
		chart.getAxisY().getAxisTitle().setTitle("Dollars".reverse); //dont ask
		chart.getAxisY().setRangePolicy(new RangePolicyPadded(0.05));
		
		panel.add("test", chart);
		frame.pack()
		frame.setSize(600, panel.getComponentCount()*200)
		frame.setVisible(true);
		(last, ask, bid)
	}
	

	
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
		val p: Double = point - (point % 1000)
		if(ceiling)	p + 1000
		else		p
				
	}
	
	def format(point: Double): String = {
		lastFormatted = point
		f"${(now - point)/1000}%2.0f"
	}
	
	def parse(str: String): Number = lastFormatted
}