package thc.sandbox.marketmanager

import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.marketmanager.data.OrderRequest
import thc.sandbox.marketmanager.data.ReceiveType
import thc.sandbox.slf4s.Logger

/**
 * Market Connection interface for brokerage specific implementations
 */
trait MarketConnection extends Logger {
	
	var callback: (Int, ReceiveType) => Unit = (_, _) => logger.error("no callback registered!")
	def registerCallback(cb: (Int, ReceiveType) => Unit) {
		callback = cb
	}
	
	def subscribe(dr: DataRequest): Int
	def unsubscribe(tickerId: Int): Unit
	
	def nextOrderId(): Int
	def placeOrder(or: OrderRequest): Unit
	def cancelOrder(orderId: Int): Unit
	
	def stop(): Unit
}