package thc.sandbox.marketmanager
import com.lmax.disruptor.RingBuffer
import thc.sandbox.marketmanager.data.ReceiveTypeContainer
import thc.sandbox.marketmanager.data.RequestTypeContainer
import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.marketmanager.data.ReceiveType
import thc.sandbox.marketmanager.data.OrderRequest

/**
 * Market Connection interface for brokerage specific implementations
 */
trait MarketConnection {
	
	def registerCallback(cb: (ReceiveType => Unit)): Unit
	
	def subscribe(dr: DataRequest): Int
	def unsubscribe(tickerId: Int): Unit
	
	def placeOrder(or: OrderRequest): Int
	def cancelOrder(orderId: Int): Unit
}