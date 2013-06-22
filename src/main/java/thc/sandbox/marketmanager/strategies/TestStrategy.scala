package thc.sandbox.marketmanager.strategies

import java.util.concurrent.Executor
import com.lmax.disruptor.dsl.Disruptor
import thc.sandbox.marketmanager.data
import thc.sandbox.marketmanager.data.Container
import thc.sandbox.marketmanager.data.LastPrice
import thc.sandbox.marketmanager.data.LastSize
import thc.sandbox.marketmanager.data.SimpleRTStockRequest
import thc.sandbox.slf4s.Logger
import thc.sandbox.util.SimpleMovingAverage
import thc.sandbox.util.Stochastic
import thc.sandbox.marketmanager.data.AskPrice

class TestStrategyBuilder extends StrategyBuilder[TestStrategy] {
	var moneyOption: Option[Double] = None
	var symbolOption: Option[String] = None
	
	private def incompleteException: Exception = {
		val errorMessage = new StringBuilder().append("invalid builder:")
		errorMessage.append("\n\tMoney: ").append(moneyOption)
		errorMessage.append("\n\tSymbol: ").append(symbolOption)
		errorMessage.append("\n\tOrderQueue: ").append(orderQueueOption)
		errorMessage.append("\n\tDataQueue: ").append(dataQueueOption)
		new IllegalStateException(errorMessage.toString)
	}
	
	def construct(implicit executor: Executor) =
		(for {
			money <- moneyOption
			symbol <- symbolOption
			orderQueue <- orderQueueOption
			dataQueue <- dataQueueOption
		} yield new TestStrategy(symbol, money, orderQueue, dataQueue)) getOrElse(throw incompleteException)
}

class TestStrategy(symbol: String, money: Double, oq: Disruptor[Container], dq: Disruptor[Container])(implicit executor: Executor) extends Strategy(oq, dq) with Logger {
	
	val title = s"Testing ($symbol)"
	val dataRequests = Seq(SimpleRTStockRequest(symbol))
	var currentPosition = money
	
	val averageVolume = new SimpleMovingAverage[Double](100)
	val STaveragePrice = new SimpleMovingAverage[Double](10)
	val MTaveragePrice = new SimpleMovingAverage[Double](20)
	val stochastic = new Stochastic(14, 3, 3)
	
	var lastPrice: Double = 0
	
	def handleData(data: Any) {
		logger.trace(s"TestStrategy($symbol) received data: $data")
		data match {
			case LastPrice(_, price, _) => 
				lastPrice = price
				STaveragePrice.add(price)
				MTaveragePrice.add(price)
			case AskPrice(_, price, now) =>
				stochastic.add(price)
				publishAudit(thc.sandbox.marketmanager.data.Stochastic(stochastic.percentD, now))
			case LastSize(_, size, _) =>
				averageVolume.add(size)
			case _ =>
		}
		logger.info(f"$symbol: price $$$lastPrice%2.2f ST avg price $$${STaveragePrice.avg}%2.2f, MT avg price $$${MTaveragePrice.avg}%2.2f, avg volume ${averageVolume.avg}%2.3f ($data)")
	}
	
}
