package thc.sandbox.marketmanager.strategies

import thc.sandbox.slf4s.Logger
import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.ActorSystem
import thc.sandbox.marketmanager.data.MarketDataType
import thc.sandbox.marketmanager.data.OrderDataType
import thc.sandbox.util.MovingAverage
import thc.sandbox.marketmanager.data.LastPrice
import thc.sandbox.marketmanager.data.LastSize

object TestStrategy extends StrategyCreator {
	
	def create(ar: ActorRef, money: Double)(implicit as: ActorSystem): ActorRef = {
		as.actorOf(Props(creator=new TestStrategy(ar, money)), "TestStrategy")
	}
	
}

class TestStrategy(val manager: ActorRef, var money: Double) extends Strategy with Logger {
	
	val averageVolume = new MovingAverage[Double](100)
	val STaveragePrice = new MovingAverage[Double](10)
	val MTaveragePrice = new MovingAverage[Double](20)
	var lastPrice: Double = 0
	
	def processMarketDataType(mdt: MarketDataType) {
		//logger.info(s"received market data: $mdt")
		mdt match {
			case LastPrice(_, price, _) => 
				lastPrice = price
				STaveragePrice.add(price)
				MTaveragePrice.add(price)
			case LastSize(_, size, _) =>
				averageVolume.add(size)
			case _ => return
		}
		logger.info(f"price $$$lastPrice%2.2f ST avg price $$${STaveragePrice.avg}%2.2f, MT avg price $$${MTaveragePrice.avg}%2.2f, avg volume ${averageVolume.avg}%2.3f")
	}
	
	def processOrderDataType(odt: OrderDataType) {
		logger.error(s"received order data type: $odt")
	}
	
}