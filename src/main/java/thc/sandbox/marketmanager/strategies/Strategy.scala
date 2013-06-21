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
	protected def handleData(data: Any): Unit

	val auditQueue: Disruptor[Container] = new Disruptor[Container](Container, 256, executor)
	val auditQueueRingBuffer = auditQueue.getRingBuffer()
	def start() {
		auditQueue.start()
	}
		
	var ids: Set[Int] = immutable.BitSet.empty
	private val dataQueueHandler = new EventHandler[Container](){
		def onEvent(c: Container, pos: Long, endOfBatch: Boolean) {
			c.id foreach (id => if(!ids.contains(id)) return)
			
			val id = auditQueueRingBuffer.next()
			auditQueueRingBuffer.get(id).value = c.value
			auditQueueRingBuffer.publish(id)
			handleData(c.value)
		}
	}
	dataQueue.handleEventsWith(dataQueueHandler)

	private val orderRingBuffer: RingBuffer[Container] = orderQueue.getRingBuffer();
	protected def placeOrder(or: OrderRequest) {
		if(orderQueue eq null) throw new IllegalStateException("no order queue registerd!")
		val id = orderRingBuffer.next()
		orderRingBuffer.get(id).value = or
		orderRingBuffer.publish(id)
	}
	
}