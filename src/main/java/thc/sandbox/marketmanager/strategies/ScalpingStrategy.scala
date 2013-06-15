package thc.sandbox.marketmanager.strategies

import thc.sandbox.slf4s.Logger
import thc.sandbox.marketmanager.data.MarketDataType
import thc.sandbox.marketmanager.data.OrderDataType
import thc.sandbox.util.MovingAverage

class ScalpingStrategy(var money: Double) extends Strategy with Logger {
	
	val averageVolume = new MovingAverage[Double](100)
	val STaveragePrice = new MovingAverage[Double](20)
	val MTaveragePrice = new MovingAverage[Double](100)
	
	def processMarketDataType(mdt: MarketDataType) {
		logger.info(s"received market data: $mdt")
	}
	
	def processOrderDataType(odt: OrderDataType) {
		logger.error(s"received order data type: $odt")
	}
	
}