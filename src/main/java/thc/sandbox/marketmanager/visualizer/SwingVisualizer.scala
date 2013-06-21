package thc.sandbox.marketmanager.visualizer

import java.awt.Color
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import scala.collection._
import scala.swing._
import org.joda.time.DateTime
import info.monitorenter.gui.chart.Chart2D
import info.monitorenter.gui.chart.ITrace2D
import info.monitorenter.gui.chart.labelformatters.ALabelFormatter
import info.monitorenter.gui.chart.rangepolicies.ARangePolicy
import info.monitorenter.gui.chart.traces.Trace2DLtd
import javax.swing.BoxLayout
import javax.swing.JFrame
import javax.swing.JPanel
import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.marketmanager.strategies.Strategy
import scala.swing.TabbedPane.Page
import thc.sandbox.marketmanager.MarketManager
import com.lmax.disruptor.EventHandler
import com.lmax.disruptor
import thc.sandbox.marketmanager.data.LastPrice
import thc.sandbox.marketmanager.data.AskPrice
import thc.sandbox.marketmanager.data.BidPrice
import javax.swing.border.EmptyBorder

trait SwingVisualizer extends MarketManager {

	val viz = new StockVisualizer
	viz.startup(Array())
	
	override def onNewStrategy(s: Strategy) {
		super.onNewStrategy(s)
		viz.tabs.pages += new Page(s.title, new StrategyVisualizer(s))
	}
	
}

class StockVisualizer extends SimpleSwingApplication {
	val tabs = new TabbedPane
	tabs.tabLayoutPolicy = TabbedPane.Layout.Scroll
	tabs.tabPlacement = Alignment.Top
	
	def top = new MainFrame {
		title = "Stock Visualizer"
		contents = new GridPanel(1, 1) {
			contents += tabs
		}
		
		size = new Dimension(600, 400)
	}
}

class StrategyVisualizer(s: Strategy) extends GridPanel(1, 1) {	
	val last = new Trace2DLtd(200)
	last.setColor(Color.green)
	last.setPhysicalUnits("Time", "Dollars ($)")
	last.setName(s"${s.title} last")
	
	val ask = new Trace2DLtd(200)
	ask.setColor(Color.red)
	ask.setPhysicalUnits("Time", "Dollars ($)")
	ask.setName("ask")
	
	val bid = new Trace2DLtd(200)
	bid.setColor(Color.black)
	bid.setPhysicalUnits("Time", "Dollars ($)")
	bid.setName("bid")
		
	val chart = new Chart2D()
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
	
	contents += new Component() { override lazy val peer = chart }
		
	def handleData(data: Any) =	data match {
		case LastPrice(id: Int, price: Double, time: DateTime) => last.addPoint(time.getMillis(), price)
		case AskPrice(id: Int, price: Double, time: DateTime)  => ask.addPoint(time.getMillis(), price)
		case BidPrice(id: Int, price: Double, time: DateTime)  => bid.addPoint(time.getMillis(), price)
		case _ =>
	}
	
	private val dataQueueHandler = new EventHandler[thc.sandbox.marketmanager.data.Container]() {
		def onEvent(data: thc.sandbox.marketmanager.data.Container, sequence: Long, endOfBatch: Boolean) = handleData(data.value)
	}
	s.auditQueue.handleEventsWith(dataQueueHandler)

}

