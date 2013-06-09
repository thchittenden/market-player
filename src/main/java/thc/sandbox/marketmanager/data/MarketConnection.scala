package thc.sandbox.marketmanager.data

import akka.actor.ActorRef

trait MarketConnection {

	def connect()
	def disconnect()
	
	def subscribe(a: ActorRef, dr: DataRequest): Int
	def unsubscribe(a: ActorRef, id: Int)

	def placeOrder(a: ActorRef, or: OrderRequest): Int
	def cancelOrder(id: Int)
	
}