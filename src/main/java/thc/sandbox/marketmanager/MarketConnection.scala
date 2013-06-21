package thc.sandbox.marketmanager

import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.marketmanager.data.OrderRequest
import thc.sandbox.marketmanager.data.ReceiveType

/**
 * Market Connection interface for brokerage specific implementations
 */
trait MarketConnection {
	
	def registerCallback(cb: (Int, ReceiveType) => Unit): Unit
	
	def subscribe(dr: DataRequest): Int
	def unsubscribe(tickerId: Int): Unit
	
	def placeOrder(or: OrderRequest): Int
	def cancelOrder(orderId: Int): Unit
}