package thc.sandbox.marketmanager

import thc.sandbox.marketmanager.data.ib.IBMarketConnection
import thc.sandbox.slf4s.Logger
import thc.sandbox.marketmanager.strategies.TestStrategyBuilder
import thc.sandbox.marketmanager.visualizer.swing.SwingVisualizer
import thc.sandbox.marketmanager.data.dummy.DummyMarketConnection
import thc.sandbox.marketmanager.benchmarker.Benchmarker

object Main extends Logger {
	
	def main(args: Array[String]) {
				
		val connection = getDummyConnection()
		
		val mm = new MarketManager(connection) with SwingVisualizer// with Benchmarker 
		
		val tsb = new TestStrategyBuilder
		tsb.moneyOption = Some(1000.00)
		tsb.symbolOption = Some("TNA")
		mm.addStrategy(tsb)
		tsb.symbolOption = Some("GOOG")
		mm.addStrategy(tsb)
		
		mm.start()		
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
	
	def getDummyConnection(): MarketConnection = new DummyMarketConnection()

}
