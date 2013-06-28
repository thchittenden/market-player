package thc.sandbox.marketmanager.visualizer.swing

import java.awt.Color
import scala.collection._
import scala.swing._
import org.joda.time.DateTime
import info.monitorenter.gui.chart.Chart2D
import info.monitorenter.gui.chart.traces.Trace2DLtd
import thc.sandbox.marketmanager.strategies.Strategy
import scala.swing.TabbedPane.Page
import thc.sandbox.marketmanager.MarketManager
import com.lmax.disruptor.EventHandler
import thc.sandbox.marketmanager.data.LastPrice
import thc.sandbox.marketmanager.data.AskPrice
import thc.sandbox.marketmanager.data.BidPrice
import thc.sandbox.marketmanager.visualizer.LabelFormatterTimeAgo
import thc.sandbox.marketmanager.visualizer.RangePolicyFixedLookback
import thc.sandbox.marketmanager.visualizer.RangePolicyPadded
import thc.sandbox.marketmanager.visualizer.swing.SwingChart._
import thc.sandbox.marketmanager.data.ReceiveType
import thc.sandbox.slf4s.Logger

trait SwingVisualizer extends MarketManager with Logger {

	object StockVisualizer extends SimpleSwingApplication {
		val tabs = new TabbedPane
		tabs.tabLayoutPolicy = TabbedPane.Layout.Scroll
		tabs.tabPlacement = Alignment.Top
		val statistics = new Statistics
		statistics.preferredSize = new Dimension(600, 50)

		def top = new MainFrame {

			override def closeOperation() {
				//invoke stop in MarketManager
				stop()
				super.closeOperation()
			}
			
			title = "Stock Visualizer"
				
			contents = new BoxPanel(Orientation.Vertical) {
				contents += tabs
				contents += statistics
			}
			
			size = new Dimension(1200, 800)
		}
		
		//startup the visualizer
		startup(Array())
	}
	
	override def onNewStrategy(s: Strategy) {
		super.onNewStrategy(s)
		StockVisualizer.tabs.pages += new Page(s.title, new StrategyVisualizer(s))
		StockVisualizer.statistics.moneyLabel.text = f"$currentPosition%2.2f";
		StockVisualizer.tabs.revalidate()
	}
	
	override def onInvalidateCurrentPosition() {
		super.onInvalidateCurrentPosition()
		StockVisualizer.statistics.moneyLabel.text = f"$currentPosition%2.2f";
		val pl = currentPosition - initialPosition
		StockVisualizer.statistics.plLabel.text = f"$pl%2.2f";
		StockVisualizer.statistics.plLabel.foreground = pl match {
			case d if d > 0  => Color.green
			case d if d == 0 => Color.black
			case _           => Color.red
		}
	}	
}

class Statistics extends GridPanel(2, 4) {
	maximumSize = new Dimension(600, 100)
	var moneyLabel = new Label("0.00");
	var plLabel = new Label("0.00");
	
	contents += new Label("Money")
	contents += new Label("P/L")
	contents += moneyLabel
	contents += plLabel	
}

class StrategyVisualizer(s: Strategy) extends GridBagPanel {
	private val charts: mutable.Map[ChartType, SwingChart] = mutable.Map.empty
		
	private val c = new Constraints
	c.fill = GridBagPanel.Fill.Both
		
	
	private def handleData(data: Any) = data match {
		case rt: ReceiveType =>
			getChartType(rt) foreach (ct => charts.getOrElseUpdate(ct, createChart(ct)).addPoint(rt))
		case _ =>
	}
	
	private def createChart(ct: ChartType): SwingChart = {
		val chart = createSwingChart(ct)
		layout(chart) = getConstraints(ct, c)
		revalidate()
		chart
	}
	
	private def getConstraints(ct: ChartType, c: Constraints): Constraints = {
		ct match {
			case MainChart => 
				c.weightx = 1.0
				c.weighty = 0.66
				c.grid = (0, 0)
			case VolumeChart =>
				c.weightx = 1.0
				c.weighty = 0.33
				c.grid = (0, 1)
			case StochasticChart =>
				c.weightx = 1.0
				c.weighty = 0.33
				c.grid = (0, 2)
			case MACDChart =>
				c.weightx = 1.0
				c.weighty = 0.33
				c.grid = (0, 3)
		}
		return c
	}
	
	private val dataQueueHandler = new EventHandler[thc.sandbox.marketmanager.data.Container]() {
		def onEvent(data: thc.sandbox.marketmanager.data.Container, sequence: Long, endOfBatch: Boolean) = handleData(data.value)
	}
	s.auditQueue.handleEventsWith(dataQueueHandler)
		
}