package thc.sandbox.marketmanager

import thc.sandbox.marketmanager.data.archive.ArchiveMarketConnection
import thc.sandbox.marketmanager.data.dummy.DummyMarketConnection
import thc.sandbox.marketmanager.data.ib.IBMarketConnection
import thc.sandbox.marketmanager.recorder.FileRecorderFileParser
import thc.sandbox.marketmanager.strategies.TestStrategyBuilder
import thc.sandbox.marketmanager.visualizer.swing.SwingVisualizer
import thc.sandbox.slf4s.Logger

object Main extends Logger {
	
	def main(args: Array[String]) {
				
		val connection = getFileConnection()
		
		val mm = new MarketManager(connection) with SwingVisualizer// with Benchmarker 
		
		val tsb = new TestStrategyBuilder
		tsb.moneyOption = Some(1000.00)
		tsb.symbolOption = Some("TNA")
		mm.addStrategy(tsb)
//		tsb.symbolOption = Some("GOOG")
//		mm.addStrategy(tsb)
		
		mm.start()		
	}
	
	def getIBConnection(): MarketConnection = {
		val connection = new IBMarketConnection
		
		connection.connect("localhost", 7496, 1)
		if(!connection.isConnected()) {
			logger.error("not connected! aborting...")
			throw new IllegalStateException()
		}
		
		return connection
	}
	
	def getFileConnection(): MarketConnection = {
		val connection = new ArchiveMarketConnection
		connection.setFile("/Users/thchittenden/Downloads/market-recorder/TNA@2013-06-27T23:53:57")
		connection.setParser(new FileRecorderFileParser(0))
		return connection
	}
	
	def getDummyConnection(): MarketConnection = new DummyMarketConnection()

}
