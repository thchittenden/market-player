package thc.sandbox.marketmanager.visualizer.swing

import scala.collection._
import scala.swing._
import info.monitorenter.gui.chart.Chart2D
import info.monitorenter.gui.chart.ITrace2D
import thc.sandbox.marketmanager.visualizer.LabelFormatterTimeAgo
import thc.sandbox.marketmanager.visualizer.RangePolicyFixedLookback
import thc.sandbox.marketmanager.visualizer.RangePolicyPadded
import info.monitorenter.gui.chart.traces.Trace2DLtd
import java.awt.Color
import info.monitorenter.gui.chart.rangepolicies.ARangePolicy
import thc.sandbox.marketmanager.data.ReceiveType
import thc.sandbox.marketmanager.data.AskPrice
import thc.sandbox.marketmanager.data.LastPrice
import thc.sandbox.marketmanager.data.BidPrice
import thc.sandbox.marketmanager.data.BidSize
import thc.sandbox.marketmanager.data.AskSize
import thc.sandbox.marketmanager.data.LastSize
import thc.sandbox.marketmanager.data.Stochastic
import thc.sandbox.marketmanager.data.ClosingPrice
import thc.sandbox.marketmanager.data.OrderCancelled
import thc.sandbox.marketmanager.data.OrderFilled
import thc.sandbox.marketmanager.data.OrderStatus
import thc.sandbox.marketmanager.visualizer.swing.SwingChart._
import info.monitorenter.gui.chart.TracePoint2D
import info.monitorenter.gui.chart.ITracePoint2D
import thc.sandbox.marketmanager.visualizer.RangePolicyFixed

/**
 * Helper methods for matching data to charts/traces
 */
object SwingChart {
	import Color._

	sealed trait ChartType
	case object MainChart extends ChartType
	case object VolumeChart extends ChartType
	case object CalcChart extends ChartType


	sealed trait TraceType {
		val name: String
		val color: Color
	}
	case object BidPriceTrace  extends TraceType { val name = "Bid Price"; val color = black }
	case object AskPriceTrace  extends TraceType { val name = "Ask Price"; val color = red }
	case object LastPriceTrace extends TraceType { val name = "Last Price"; val color = green }
	case object AskSizeTrace extends TraceType { val name = "Ask Size"; val color = red }
	case object BidSizeTrace extends TraceType { val name = "Bid Size"; val color = black }
	case object LastSizeTrace extends TraceType { val name = "Last Size"; val color = green }
	case object OrdersTrace extends TraceType { val name = "Orders"; val color = blue }
	case object StochasticTrace extends TraceType { val name = "Stochastic"; val color = blue }
	
	def getPoint(rt: ReceiveType): ITracePoint2D = rt match {
		case BidPrice(_, price, time) 		=> new TracePoint2D(time.getMillis(), price)
		case AskPrice(_, price, time)  		=> new TracePoint2D(time.getMillis(), price)
		case LastPrice(_, price, time) 		=> new TracePoint2D(time.getMillis(), price)
		case ClosingPrice(_, price, time) 	=> new TracePoint2D(time.getMillis(), price)
		case BidSize(_, size, time)    		=> new TracePoint2D(time.getMillis(), (size*100).floor)
		case AskSize(_, size, time)    		=> new TracePoint2D(time.getMillis(), (size*100).floor)
		case LastSize(_, size, time)   		=> new TracePoint2D(time.getMillis(), (size*100).floor)
		
		case Stochastic(value, time)   		=> new TracePoint2D(time.getMillis(), value)
		
		case OrderFilled(_, _, pps, time)    => new TracePoint2D(time.getMillis(), pps)
		case OrderStatus(_, _, pps, time)    => new TracePoint2D(time.getMillis(), pps)
		case OrderCancelled(_, _, pps, time) => new TracePoint2D(time.getMillis(), pps)
	}
	
	def getChartType(rt: ReceiveType): Option[ChartType] = rt match {
		case _: BidPrice     => Some(MainChart)
		case _: AskPrice     => Some(MainChart)
		case _: LastPrice    => Some(MainChart)
		case _: ClosingPrice => None
		
		case _: BidSize  => Some(VolumeChart)
		case _: AskSize  => Some(VolumeChart)
		case _: LastSize => Some(VolumeChart)
		
		case _: Stochastic => Some(CalcChart)
		
		case _: OrderFilled    => Some(MainChart)
		case _: OrderStatus    => Some(MainChart)
		case _: OrderCancelled => Some(MainChart)
	}
	
	def getTraceType(rt: ReceiveType): Option[TraceType] = rt match { 
		case _: BidPrice     => Some(BidPriceTrace)
		case _: AskPrice     => Some(AskPriceTrace)
		case _: LastPrice    => Some(LastPriceTrace)
		case _: ClosingPrice => None
		
		case _: BidSize  => Some(BidSizeTrace)
		case _: AskSize  => Some(AskSizeTrace)
		case _: LastSize => Some(LastSizeTrace)
		
		case _: Stochastic => Some(StochasticTrace)
		
		case _: OrderFilled    => Some(OrdersTrace)
		case _: OrderStatus    => Some(OrdersTrace)
		case _: OrderCancelled => Some(OrdersTrace)
	}
	
	def getTraceName(tt: TraceType): String = tt.name
	def getTraceColor(tt: TraceType): Color = tt.color
	
	def createSwingChart(ct: ChartType): SwingChart = ct match {
		case MainChart => 
			new SwingChart(new RangePolicyFixedLookback(180*1000), new RangePolicyPadded(0.10))
		case VolumeChart =>
			new SwingChart(new RangePolicyFixedLookback(180*1000), new RangePolicyPadded(0, 20))
		case CalcChart =>
			new SwingChart(new RangePolicyFixedLookback(180*1000), new RangePolicyFixed(0, 100))
		case _ => null
	}
}

class SwingChart(val xAxisRangePolicy: ARangePolicy, val yAxisRangePolicy: ARangePolicy) extends Component {
	val traces: mutable.Map[TraceType, ITrace2D] = mutable.Map.empty

	override lazy val peer = new Chart2D()
	peer.setUseAntialiasing(true)
	peer.getAxisX().setPaintGrid(true)
	peer.getAxisX().getAxisTitle().setTitle("Seconds Ago")
	peer.getAxisX().setRangePolicy(xAxisRangePolicy)
	peer.getAxisX().setFormatter(new LabelFormatterTimeAgo())

	peer.getAxisY().setPaintGrid(true)
	peer.getAxisY().getAxisTitle().setTitle("Dollars".reverse); //dont ask
	peer.getAxisY().setRangePolicy(yAxisRangePolicy);
	
	def addPoint(rt: ReceiveType) {
		SwingChart.getTraceType(rt) foreach	(tt => traces.getOrElseUpdate(tt, addNewTrace(tt)).addPoint(getPoint(rt)))
	}
	
	def addNewTrace(tt: TraceType): ITrace2D = {
		val trace = new Trace2DLtd(200)
		trace.setName(tt.name)
		trace.setColor(tt.color)
		peer.addTrace(trace)
		return trace
	}		
	
}