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

trait Visualizer extends MarketManager {
	
	implicit val as: ActorSystem
	val vizActor: ActorRef = as.actorOf(Props(new VisualizerActor))
	
	abstract override def subscribe(ag: ActorGroup, dr: DataRequest) {
		ag.add(vizActor)
		super.subscribe(ag, dr)
	}
	
	abstract override def order(ag: ActorGroup, or: OrderRequest) {
		ag.add(vizActor)
		super.order(ag, or)
	}
	
}

class VisualizerActor extends Actor {
	
	val chart = new Chart2D();
	chart.setUseAntialiasing(true)
	chart.getAxisY().setRangePolicy(new RangePolicyPadded(0.10))
	chart.getAxisY().setPaintGrid(true)
	chart.getAxisY().getAxisTitle().setTitle("Dollars (USD)")
	chart.getAxisX().setPaintGrid(true)
	chart.getAxisX().getAxisTitle().setTitle("Seconds Ago")
	chart.getAxisX().setFormatter(new LabelFormatterTimeAgo())
	
	val frame = new JFrame("Prices")
	frame.getContentPane().add(chart)
	frame.setSize(600, 400)
	frame.setVisible(true)
	frame.addWindowListener(new WindowAdapter() {
		override def windowClosing(e: WindowEvent) = System.exit(0)
	})
	
	val traces: mutable.Map[Int, ITrace2D] = mutable.HashMap.empty

	val colorSeed = new Random();
	colorSeed.setSeed(100);
	
	def randomColor = new Color(colorSeed.nextInt(255), colorSeed.nextInt(255), colorSeed.nextInt(255))
	
	def createNewTrace(id: Int): ITrace2D = {
		val ret = new Trace2DLtd(200)
		ret.setColor(randomColor)
		ret.setPhysicalUnits("Time", "Dollars ($)")
		chart.addTrace(ret)
		ret
	}
	
	def addPoint(id: Int, x: Double, y: Double) {
		(traces getOrElseUpdate(id, createNewTrace(id))).addPoint(x, y)
	}
	
	def receive = {
		case LastPrice(id: Int, price: Double, time: DateTime) => addPoint(id, time.getMillis(), price)
		case _ =>
	}
	
}

class RangePolicyPadded(val padding: Double) extends ARangePolicy {
	
	def getMax(chartmin: Double, chartmax: Double): Double = chartmax + padding
	
	def getMin(chartmin: Double, chartmax: Double): Double = chartmin - padding
	
}

class LabelFormatterTimeAgo extends ALabelFormatter {
	
	def now: Double = new DateTime().getMillis()
	
	val getMinimumValueShiftForChange: Double = 1000
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