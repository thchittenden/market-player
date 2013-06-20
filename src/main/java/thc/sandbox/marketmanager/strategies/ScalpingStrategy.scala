package thc.sandbox.marketmanager.strategies

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.actor.Props
import thc.sandbox.marketmanager.data.MarketDataType
import thc.sandbox.marketmanager.data.OrderDataType
import thc.sandbox.slf4s.Logger
import thc.sandbox.util.SimpleMovingAverage
import thc.sandbox.util.Stochastic
import thc.sandbox.marketmanager.data.LastPrice
import thc.sandbox.marketmanager.data.LastSize
import thc.sandbox.marketmanager.data.ClosingPrice
import thc.sandbox.marketmanager.data.BidPrice
import thc.sandbox.marketmanager.data.AskPrice
import thc.sandbox.marketmanager.data.OrderRequest
import thc.sandbox.marketmanager.data.SimpleStockMarketBuyOrder
import thc.sandbox.marketmanager.data.SimpleStockMarketSellOrder
import thc.sandbox.marketmanager.data.OrderFilled
import scala.concurrent.ExecutionContext.Implicits.global
import collection._
import akka.util.Timeout
import scala.concurrent.Future
import scala.util.Success
import scala.util.Failure
import thc.sandbox.marketmanager.data.SimpleStockLimitSellOrder
import thc.sandbox.marketmanager.data.SimpleStockLimitBuyOrder


object ScalpingStrategy extends StrategyCreator {
	
	def create(ar: ActorRef, symbol: String, money: Double)(implicit as: ActorSystem): ActorRef = {
		as.actorOf(Props(creator=new ScalpingStrategy(ar, symbol, money)), symbol)
	}
	
}
class ScalpingStrategy(val manager: ActorRef, val symbol: String, var money: Double) extends Strategy with Logger {
	
	implicit val timeout: Timeout = 10000
	
	var quantityHeld: Int = 0
	val openOrders: mutable.Map[Int, Int] = mutable.Map.empty
	
	var lastPrice = 0.0
	var lastAsk = 0.0
	var lastBid = 0.0
	
	val averageVolume = new SimpleMovingAverage[Double](100)
	val STaveragePrice = new SimpleMovingAverage[Double](20)
	val MTaveragePrice = new SimpleMovingAverage[Double](100)
	val stochastic = new Stochastic(14, 3, 3)
	
	val orderSize = money * 0.25
	var stochasticBelow20 = false
	var stochasticAbove80 = false
	
	def processData() {
		if(stochasticBelow20 && stochastic.percentD > 20) {
			stochasticBelow20 = false
			if(money > orderSize) {
				val quantity: Int = (orderSize / lastAsk).toInt
				val future = manager ? SimpleStockLimitBuyOrder(symbol, quantity, lastBid)
				future onComplete {
					case Success(orderId: Int) => openOrders.put(orderId, quantity)
					case Failure(t) => logger.error(s"could not commit buy order: $t")
				}
			}
		}
		if(stochasticAbove80 && stochastic.percentD < 80) {
			if(quantityHeld > 0) {
				val future = manager ? SimpleStockLimitSellOrder(symbol, quantityHeld, lastBid)
				future onComplete {
					case Success(orderId: Int) => openOrders.put(orderId, -quantityHeld)
					case Failure(t) => logger.error(s"could not commit sell order: $t")
				}
			}
		}
		if(stochastic.percentD > 80) stochasticAbove80 = true
		if(stochastic.percentD < 20) stochasticBelow20 = true
		logger.trace(f"stochastic: %K = ${stochastic.percentK}%2.2f, %d = ${stochastic.percentD}%2.2f")
	}

	
	def processMarketDataType(mdt: MarketDataType) {
		logger.info(s"received market data: $mdt")
		mdt match {
			case LastPrice(_, price, _) => 
				lastPrice = price
				STaveragePrice.add(price)
				MTaveragePrice.add(price)
			case AskPrice(_, price, _) =>
				stochastic.add(price)
				lastAsk = price
			case BidPrice(_, price, _) =>
				lastBid = price
			case ClosingPrice(_, price, _) =>
			case LastSize(_, size, _) =>
				averageVolume.add(size)
			case _ =>
		}
		processData()
	}
	
	def processOrderDataType(odt: OrderDataType) {
		odt match {
			case OrderFilled(orderId, numFilled, avgFillPrice, now) => 
				money += openOrders(orderId) * avgFillPrice
				openOrders.remove(orderId)
		}
	}
	
}