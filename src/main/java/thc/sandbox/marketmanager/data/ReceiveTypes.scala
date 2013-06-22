package thc.sandbox.marketmanager.data

import org.joda.time.DateTime
import com.lmax.disruptor.EventFactory


sealed abstract class ReceiveType 

abstract sealed class MarketDataType extends ReceiveType {
	val id: Int
}
case class LastPrice(id: Int, price: Double, time: DateTime) extends MarketDataType
case class AskPrice(id: Int, price: Double, time: DateTime) extends MarketDataType
case class BidPrice(id: Int, price: Double, time: DateTime) extends MarketDataType
case class ClosingPrice(id: Int, price: Double, time: DateTime) extends MarketDataType

case class LastSize(id: Int, size: Int, time: DateTime) extends MarketDataType
case class AskSize(id: Int, size: Int, time: DateTime) extends MarketDataType
case class BidSize(id: Int, size: Int, time: DateTime) extends MarketDataType


abstract sealed class OrderDataType extends ReceiveType {
	val id: Int
}
case class OrderStatus(id: Int, filled: Int, avgFillPrice: Double, time: DateTime) extends OrderDataType
case class OrderFilled(id: Int, filled: Int, avgFillPrice: Double, time: DateTime) extends OrderDataType
case class OrderCancelled(id: Int, filled: Int, avgFillPrice: Double, time: DateTime) extends OrderDataType

abstract sealed class CalculatedType extends ReceiveType
case class Stochastic(value: Double, time: DateTime) extends CalculatedType
