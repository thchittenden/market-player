package thc.sandbox.marketmanager

import thc.sandbox.marketmanager.data.ib.IBMarketConnection
import thc.sandbox.slf4s.Logger
import thc.sandbox.marketmanager.strategies.TestStrategyBuilder
import thc.sandbox.marketmanager.visualizer.SwingVisualizer

object Main extends Logger {
	
	def main(args: Array[String]) {
		
//		implicit val as: ActorSystem = ActorSystem("MarketManager")
		
		val connection = getIBConnection()
		
		val mm = new MarketManager(connection) with SwingVisualizer
		
		val tsb = new TestStrategyBuilder
		tsb.moneyOption = Some(1000.00)
		tsb.symbolOption = Some("TNA")
		mm.addStrategy(tsb)
		tsb.symbolOption = Some("GOOG")
		mm.addStrategy(tsb)
		
		mm.start()
		
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
	
//	def getDummyConnection: MarketConnection = new DummyMarketConnection()

}