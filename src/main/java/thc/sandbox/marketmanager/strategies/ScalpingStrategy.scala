package thc.sandbox.marketmanager.strategies

import thc.sandbox.slf4s.Logger
import thc.sandbox.marketmanager.data.MarketDataType
import thc.sandbox.marketmanager.data.OrderDataType
import thc.sandbox.util.MovingAverage
import com.lmax.disruptor.dsl.Disruptor
import thc.sandbox.marketmanager.data.Container
import thc.sandbox.marketmanager.data.SimpleRTStockRequest
import java.util.concurrent.Executor

class ScalpingStrategyBuilder extends StrategyBuilder[ScalpingStrategy] {
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
		} yield new ScalpingStrategy(symbol, money, orderQueue, dataQueue)) getOrElse(throw incompleteException)
}

class ScalpingStrategy(symbol: String, var money: Double, oq: Disruptor[Container], dq: Disruptor[Container])(implicit executor: Executor) extends Strategy(oq, dq) with Logger {
	
	val title = s"Scalping ($symbol)"
	val dataRequests = Seq(SimpleRTStockRequest(symbol))
	
	val averageVolume = new MovingAverage[Double](100)
	val STaveragePrice = new MovingAverage[Double](20)
	val MTaveragePrice = new MovingAverage[Double](100)
	
	def handleData(data: Any) {
		logger.trace(s"ScalpingStrategy($symbol) received data: $data")
	}
	
}