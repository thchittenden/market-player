package thc.sandbox.marketmanager.strategies

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import thc.sandbox.slf4s.Logger
import thc.sandbox.marketmanager.data.MarketDataType
import thc.sandbox.marketmanager.data.OrderDataType
import thc.sandbox.util.MovingAverage



object ScalpingStrategy extends StrategyCreator {
	
	def create(ar: ActorRef, money: Double)(implicit as: ActorSystem): ActorRef = {
		as.actorOf(Props(creator=new ScalpingStrategy(ar, money)))
	}
	
}
class ScalpingStrategy(val manager: ActorRef, var money: Double) extends Strategy with Logger {
	
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