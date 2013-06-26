package thc.sandbox.marketmanager.data

import com.lmax.disruptor.EventFactory

abstract sealed class RequestType

abstract sealed class DataRequest extends RequestType
case class SimpleRTStockRequest(symbol: String) extends DataRequest


abstract sealed class OrderRequest extends RequestType with ID
abstract sealed class BuyRequest extends OrderRequest
abstract sealed class SellRequest extends OrderRequest

case class SimpleStockMarketBuyOrder(id: Int, symbol: String, quantity: Int) extends BuyRequest
case class SimpleStockMarketSellOrder(id: Int, symbol: String, quantity: Int) extends SellRequest
case class SimpleStockLimitBuyOrder(id: Int, symbol: String, quantity: Int, price: Double) extends BuyRequest
case class SimpleStockLimitSellOrder(id: Int, symbol: String, quantity: Int, price: Double) extends SellRequest
