package thc.sandbox.marketmanager

import java.util.concurrent.Executors
import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.RingBuffer
import com.lmax.disruptor.dsl.Disruptor
import thc.sandbox.marketmanager.data.Container
import thc.sandbox.marketmanager.data.OrderRequest
import thc.sandbox.marketmanager.data.ReceiveType
import thc.sandbox.slf4s.Logger
import thc.sandbox.marketmanager.strategies.Strategy
import thc.sandbox.marketmanager.strategies.StrategyBuilder
import collection._
import thc.sandbox.marketmanager.data.DataRequest

class MarketManager(val conn: MarketConnection) extends Logger {

	private implicit val executor = Executors.newCachedThreadPool();
	
	val orderData: Disruptor[Container] = new Disruptor[Container](Container, 32, executor)
	val orderDataRingBuffer = orderData.getRingBuffer()
	val marketData: Disruptor[Container] = new Disruptor[Container](Container, 512, executor)
	val marketDataRingBuffer = marketData.getRingBuffer()
	
	val strategies: mutable.Set[Strategy] = mutable.HashSet.empty
	var initialPosition = 0.0
	var currentPosition = 0.0
	
	//register connection callback
	conn.registerCallback(onNewData)
	
	//register order queue callback
	private val orderDataQueueHandler = new EventHandler[Container]() {
		def onEvent(c: Container, pos: Long, endOfBatch: Boolean) {
			c.value match {
				case or: OrderRequest => onNewOrder(or)
				case _ => logger.warn(s"invalid order data ${c.value}")
			}
			
		}
	}
	orderData.handleEventsWith(orderDataQueueHandler)
	
	def addStrategy[T <: Strategy](sb: StrategyBuilder[T]) {
		sb.dataQueueOption = Some(marketData)
		sb.orderQueueOption = Some(orderData)
		sb.orderIdGenOption = Some(conn.nextOrderId)
		sb.invalidateCurrentPosOption = Some(onInvalidateCurrentPosition)
		onNewStrategy(sb.construct)	
	}
	
	
	//callbacks for extensions
	protected def onNewStrategy(s: Strategy) { 
		currentPosition += s.currentPosition
		initialPosition += s.currentPosition
		s.ids = immutable.BitSet((for (dr <- s.dataRequests) yield onSubscribe(dr)): _*)
		strategies.add(s) 
	}
	protected def onSubscribe(dr: DataRequest): Int = {
		conn.subscribe(dr)
	}
	protected def onNewOrder(or: OrderRequest) {
		conn.placeOrder(or) 
	}
	protected def onNewData(dataId: Int, data: ReceiveType) { 
		val id = marketDataRingBuffer.next()
		marketDataRingBuffer.get(id).id = Some(dataId)
		marketDataRingBuffer.get(id).value = data
		marketDataRingBuffer.publish(id)
	}
	protected def onInvalidateCurrentPosition() {
		currentPosition = strategies.foldLeft(0.00)(_ + _.currentPosition)
	}
	
	def start() {
		orderData.start()
		marketData.start()
		strategies foreach (_.start())
	}
	
	def stop() {
		logger.info("shutting down!")
		strategies foreach (_.stop())
		conn.stop()
		orderData.shutdown()
		marketData.shutdown()
	}
}
