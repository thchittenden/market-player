package thc.sandbox.marketmanager.data

abstract sealed class DataRequest

case class SimpleRTStockRequest(symbol: String) extends DataRequest


abstract sealed class OrderRequest {
	val symbol: String
}

case class SimpleStockMarketBuyOrder(symbol: String, quantity: Int) extends OrderRequest
case class SimpleStockMarketSellOrder(symbol: String, quantity: Int) extends OrderRequest
case class SimpleStockLimitBuyOrder(symbol: String, quantity: Int, price: Double) extends OrderRequest
case class SimpleStockLimitSellOrder(symbol: String, quantity: Int, price: Double) extends OrderRequest