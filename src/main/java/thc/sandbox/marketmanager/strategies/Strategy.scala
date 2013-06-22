package thc.sandbox.marketmanager.strategies

import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.RingBuffer
import com.lmax.disruptor.dsl.Disruptor
import thc.sandbox.marketmanager.data.Container
import thc.sandbox.marketmanager.data.OrderRequest
import thc.sandbox.marketmanager.data.DataRequest
import java.util.concurrent.Executor
import scala.concurrent.Future
import collection._

abstract class StrategyBuilder[T <: Strategy] {
	var orderQueueOption: Option[Disruptor[Container]] = None
	var dataQueueOption: Option[Disruptor[Container]] = None
	
	def construct(implicit executor: Executor): T 
}

abstract class Strategy(orderQueue: Disruptor[Container], dataQueue: Disruptor[Container])(implicit executor: Executor) {
	
	//to be implemented by strategies
	val title: String
	val dataRequests: Seq[DataRequest]
	var currentPosition: Double
	protected def handleData(data: Any): Unit

	val auditQueue: Disruptor[Container] = new Disruptor[Container](Container, 256, executor)
	val auditQueueRingBuffer = auditQueue.getRingBuffer()
	def start() {
		auditQueue.start()
	}
		
	var ids: Set[Int] = immutable.BitSet.empty
	private val dataQueueHandler = new EventHandler[Container](){
		def onEvent(c: Container, pos: Long, endOfBatch: Boolean) {
			//if we have an id and it doesn't match return
			//TODO create separate ring buffer for every DR?
			c.id foreach (id => if(!(ids contains id)) return)
			
			handleData(c.value)
			publishAudit(c.value)
		}
	}
	dataQueue.handleEventsWith(dataQueueHandler)

	private val orderRingBuffer: RingBuffer[Container] = orderQueue.getRingBuffer();
	protected def placeOrder(or: OrderRequest) {
		if(orderQueue eq null) throw new IllegalStateException("no order queue registerd!")
		val orderId = orderRingBuffer.next()
		orderRingBuffer.get(orderId).value = or
		orderRingBuffer.publish(orderId)
		
		publishAudit(or)
	}
	
	protected def publishAudit(d: Any) {
		val auditId = auditQueueRingBuffer.next()
		auditQueueRingBuffer.get(auditId).value = d
		auditQueueRingBuffer.publish(auditId)
	}
	
}
