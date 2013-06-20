package thc.sandbox.marketmanager

import scala.collection._
import akka.actor.ActorDSL.Act
import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import thc.sandbox.marketmanager.data.MarketConnection
import thc.sandbox.marketmanager.data.OrderRequest
import thc.sandbox.marketmanager.data.SimpleRTStockRequest
import thc.sandbox.marketmanager.strategies.Strategy
import thc.sandbox.marketmanager.strategies.StrategyCreator
import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.util.ActorGroup
import thc.sandbox.marketmanager.data.OrderStatus
import akka.pattern._

class MarketManager(val conn: MarketConnection, var money: Double)(implicit val as: ActorSystem) {

	val strategies: mutable.ListBuffer[ActorRef] = mutable.ListBuffer.empty
	val strategyMoney: mutable.Map[ActorRef, Double] = mutable.HashMap.empty
	val subscriptions: mutable.Map[Int, DataRequest] = mutable.HashMap.empty
	
	val tickerIdToStock: mutable.Map[Int, String] = mutable.HashMap.empty
	val orderIdToStock: mutable.Map[Int, String] = mutable.HashMap.empty
	
	val messageProcessor = actor(new Act {
		become {
			case or: OrderRequest => sender ! order(ActorGroup(sender), or)
		}
	})
	
	def addStrategy(strategy: StrategyCreator, symbol: String, initialMoney: Double, allowMargin: Boolean) {
		val strategyActor = strategy.create(messageProcessor, symbol, initialMoney)
		strategies.prepend(strategyActor)
		strategyMoney.put(strategyActor, initialMoney)
		
		//subscribe to symbol
		subscribe(ActorGroup(strategyActor), SimpleRTStockRequest(symbol))
	}
	
	def subscribe(actors: ActorGroup, dr: DataRequest) {
		val tickerId = conn.subscribe(actors, dr)
		tickerIdToStock += tickerId -> dr.symbol
		subscriptions += tickerId -> dr
	}
		
	def order(actors: ActorGroup, or: OrderRequest): Int = {
		//validate(or)
		val id = conn.placeOrder(actors, or)
		orderIdToStock += id -> or.symbol
		return id
	}
	
}