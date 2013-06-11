package thc.sandbox.marketmanager.data

import org.joda.time.DateTime

abstract class MarketDataType {
	val id: Int
}

case class LastPrice(id: Int, price: Double, time: DateTime) extends MarketDataType
case class AskPrice(id: Int, price: Double, time: DateTime) extends MarketDataType
case class BidPrice(id: Int, price: Double, time: DateTime) extends MarketDataType

case class LastSize(id: Int, size: Int, time: DateTime) extends MarketDataType
case class AskSize(id: Int, size: Int, time: DateTime) extends MarketDataType
case class BidSize(id: Int, size: Int, time: DateTime) extends MarketDataType

abstract class OrderDataType {
	val id: Int
}

case class OrderStatus(id: Int, filled: Int, avgFillPrice: Double) extends OrderDataType
case class OrderFilled(id: Int, avgFillPrice: Double) extends OrderDataType
case class OrderCancelled(id: Int, filled: Int, avgFillPrice: Double) extends OrderDataType