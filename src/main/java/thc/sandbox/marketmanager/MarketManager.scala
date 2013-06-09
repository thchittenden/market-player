package thc.sandbox.marketmanager

import scala.collection.Seq
import akka.actor.ActorDSL._
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import thc.sandbox.marketmanager.data.MarketConnection
import thc.sandbox.marketmanager.data.SimpleRTStockRequest
import thc.sandbox.marketmanager.strategies.Strategy
import thc.sandbox.marketmanager.data.OrderRequest

class MarketManager(conn: MarketConnection, initialMoney: Double) {
	
	implicit val actorSystem = ActorSystem("MarketManager")
	val orderDispatcher = actor(new Act {
		become {
			case or: OrderRequest => conn.placeOrder(sender, or)
		}
	})
	
	var strategies: List[ActorRef] = List.empty
	
	def addStrategy(strategyConstructor: ActorRef => Props, symbols: Seq[String]) {
		val strategyProps = strategyConstructor(orderDispatcher)
		val strategyActor = actorSystem.actorOf(strategyProps)
		strategies = strategyActor::strategies
		
		//subscribe to symbols
		symbols foreach (s => conn.subscribe(strategyActor, SimpleRTStockRequest(s)))
	}
		
}