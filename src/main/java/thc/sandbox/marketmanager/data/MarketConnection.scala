package thc.sandbox.marketmanager.data

import akka.actor.ActorRef
import thc.sandbox.util.ActorGroup

/**
 * Market Connection interface for brokerage specific implementations
 */
trait MarketConnection {
	
	def subscribe(ag: ActorGroup, dr: DataRequest): Int
	def unsubscribe(ag: ActorGroup, id: Int)

	def placeOrder(a: ActorGroup, or: OrderRequest): Int
	def cancelOrder(id: Int)
	
}