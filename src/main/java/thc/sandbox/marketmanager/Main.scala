package thc.sandbox.marketmanager

import thc.sandbox.marketmanager.ib.IBMarketConnection
import thc.sandbox.marketmanager.data.MarketConnection
import thc.sandbox.marketmanager.strategies.TestStrategy
import akka.actor.ActorRef
import thc.sandbox.slf4s.Logger
import akka.actor.ActorSystem

object Main extends Logger {
	
	def main(args: Array[String]) {
		
		implicit val actorSystem = ActorSystem("MarketManager")
		val connection = new IBMarketConnection
		
		connection.connect("localhost", 7496, 1)
		if(!connection.isConnected()) {
			logger.error("not connected! aborting...")
			return
		}
		
		val mm = new MarketManager(connection, 1000.00) with Visualizer
		mm.addStrategy(TestStrategy, "TNA", 1000.00, false)
		
	}

}