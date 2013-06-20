package thc.sandbox.marketmanager.strategies

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.pattern.AskSupport
import thc.sandbox.marketmanager.data.MarketDataType
import thc.sandbox.marketmanager.data.OrderDataType

//strategy creator trait to hold Strategy factory
trait StrategyCreator {
	def create(od: ActorRef, symbol: String, money: Double)(implicit as: ActorSystem): ActorRef
}

abstract class Strategy extends Actor {
	
	val manager: ActorRef
	var money: Double
	
	def processOrderDataType(odt: OrderDataType): Unit
	def processMarketDataType(mdt: MarketDataType): Unit
	
	def receive = {
		case odt: OrderDataType => processOrderDataType(odt)
		case mdt: MarketDataType => processMarketDataType(mdt)
	}
	
}