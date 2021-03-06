package thc.sandbox.marketmanager.strategies

import java.util.concurrent.Executor

import scala.collection.mutable

import com.lmax.disruptor.dsl.Disruptor

import thc.sandbox.marketmanager.data.AskPrice
import thc.sandbox.marketmanager.data.BidPrice
import thc.sandbox.marketmanager.data.ClosingPrice
import thc.sandbox.marketmanager.data.Container
import thc.sandbox.marketmanager.data.LastPrice
import thc.sandbox.marketmanager.data.LastSize
import thc.sandbox.marketmanager.data.SimpleRTStockRequest
import thc.sandbox.marketmanager.data.SimpleStockLimitBuyOrder
import thc.sandbox.marketmanager.data.SimpleStockLimitSellOrder
import thc.sandbox.slf4s.Logger
import thc.sandbox.util.SimpleMovingAverage
import thc.sandbox.util.Stochastic

class ScalpingStrategyBuilder extends StrategyBuilder[ScalpingStrategy] {
	var moneyOption: Option[Double] = None
	var symbolOption: Option[String] = None
	
	private def incompleteException: Exception = {
		val errorMessage = new StringBuilder().append("invalid builder:")
		errorMessage.append("\n\tMoney: ").append(moneyOption)
		errorMessage.append("\n\tSymbol: ").append(symbolOption)
		errorMessage.append("\n\tOrderQueue: ").append(orderQueueOption)
		errorMessage.append("\n\tDataQueue: ").append(dataQueueOption)
		errorMessage.append("\n\tOrderIDGen: ").append(orderIdGenOption)
		errorMessage.append("\n\tInvalidateCurPos: ").append(invalidateCurrentPosOption)
		new IllegalStateException(errorMessage.toString)
	}
	
	def construct(implicit executor: Executor) =
		(for {
			money <- moneyOption
			symbol <- symbolOption
			orderQueue <- orderQueueOption
			dataQueue <- dataQueueOption
			orderIdGen <- orderIdGenOption
			invalidateCurPos <- invalidateCurrentPosOption
		} yield new ScalpingStrategy(symbol, money, orderQueue, dataQueue, orderIdGen, invalidateCurPos)) getOrElse(throw incompleteException)
}

class ScalpingStrategy(symbol: String, money: Double, 
					   oq: Disruptor[Container], dq: Disruptor[Container], 
					   orderIdGen: () => Int, invalidateCurPos: () => Unit)
					  (implicit executor: Executor) extends Strategy(oq, dq, orderIdGen, invalidateCurPos) with Logger {
	
	val title = s"Scalping Strategy ($symbol)"
	val dataRequests = Seq(SimpleRTStockRequest(symbol))
	var currentPosition = money
	
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
				val quantity = (orderSize / lastAsk).toInt
				val id = orderIdGen()
				val order = SimpleStockLimitBuyOrder(id, symbol, quantity, lastBid)
				placeOrder(order)
			}
		}
		if(stochasticAbove80 && stochastic.percentD < 80) {
			if(quantityHeld > 0) {
				val id = orderIdGen()
				val order = SimpleStockLimitSellOrder(id, symbol, quantityHeld, lastBid)
				placeOrder(order)
			}
		}
		if(stochastic.percentD > 80) stochasticAbove80 = true
		if(stochastic.percentD < 20) stochasticBelow20 = true
		logger.trace(f"stochastic: %%K = ${stochastic.percentK}%2.2f, %%d = ${stochastic.percentD}%2.2f")
	}

	
	def handleData(data: Any) {
		logger.info(s"received market data: $data")
		data match {
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
	
}
