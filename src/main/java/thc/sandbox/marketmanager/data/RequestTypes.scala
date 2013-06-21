package thc.sandbox.marketmanager.data

import com.lmax.disruptor.EventFactory

object RequestTypeContainer {
	val factory: EventFactory[RequestTypeContainer] = new EventFactory[RequestTypeContainer]() {
		def newInstance: RequestTypeContainer = new RequestTypeContainer
	}
}

final class RequestTypeContainer {
	var value: RequestType = null
}

abstract sealed class RequestType {
	val symbol: String
}

abstract sealed class DataRequest extends RequestType
case class SimpleRTStockRequest(symbol: String) extends DataRequest


abstract sealed class OrderRequest extends RequestType
case class SimpleStockMarketBuyOrder(symbol: String, quantity: Int) extends OrderRequest
case class SimpleStockMarketSellOrder(symbol: String, quantity: Int) extends OrderRequest
case class SimpleStockLimitBuyOrder(symbol: String, quantity: Int, price: Double) extends OrderRequest
case class SimpleStockLimitSellOrder(symbol: String, quantity: Int, price: Double) extends OrderRequest
