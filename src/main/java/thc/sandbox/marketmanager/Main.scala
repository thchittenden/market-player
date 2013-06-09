package thc.sandbox.marketmanager

import thc.sandbox.marketmanager.ib.IBMarketConnection
import thc.sandbox.marketmanager.data.MarketConnection
import thc.sandbox.marketmanager.strategies.TestStrategy

object Main {
	
	def main(args: Array[String]) {
		val connection: MarketConnection = new IBMarketConnection
		connection.connect
		
		val mm: MarketManager = new MarketManager(connection, 1000.00)
		mm.addStrategy(TestStrategy.constructor, Seq("TNA"))
		
	}

}