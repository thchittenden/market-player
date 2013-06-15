package thc.sandbox.marketmanager

import thc.sandbox.marketmanager.MarketConnection
import thc.sandbox.marketmanager.data.dummy.DummyMarketConnection
import thc.sandbox.marketmanager.data.ib.IBMarketConnection
import thc.sandbox.slf4s.Logger

object Main extends Logger {
	
	def main(args: Array[String]) {
		
//		implicit val as: ActorSystem = ActorSystem("MarketManager")
		
		val connection = getIBConnection()
		
//		val mm = new MarketManager(connection, 1000.00) with Visualizer
//		mm.addStrategy(TestStrategy, "TNA", 1000.00, false)
//		mm.addStrategy(TestStrategy, "GOOG", 1000.00, false)
//		mm.addStrategy(TestStrategy, "AAPL", 1000.00, false)
		
	}
	
	def getIBConnection(): MarketConnection = {
		val connection = new IBMarketConnection
		
		connection.connect("localhost", 7496, 1)
		if(!connection.isConnected()) {
			logger.error("not connected! aborting...")
			throw new IllegalStateException()
		}
		connection
	}
	
	def getDummyConnection: MarketConnection = new DummyMarketConnection()

}