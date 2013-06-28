package thc.sandbox.marketmanager.data

import org.joda.time.DateTime
import com.lmax.disruptor.EventFactory


sealed abstract class ReceiveType

abstract sealed class MarketDataType extends ReceiveType with ID
case class LastPrice(id: Int, price: Double, time: DateTime) extends MarketDataType
case class AskPrice(id: Int, price: Double, time: DateTime) extends MarketDataType
case class BidPrice(id: Int, price: Double, time: DateTime) extends MarketDataType
case class ClosingPrice(id: Int, price: Double, time: DateTime) extends MarketDataType

case class LastSize(id: Int, size: Int, time: DateTime) extends MarketDataType
case class AskSize(id: Int, size: Int, time: DateTime) extends MarketDataType
case class BidSize(id: Int, size: Int, time: DateTime) extends MarketDataType


abstract sealed class OrderDataType extends ReceiveType with ID
case class OrderStatus(id: Int, filled: Int, avgFillPrice: Double, time: DateTime) extends OrderDataType
case class OrderFilled(id: Int, filled: Int, avgFillPrice: Double, time: DateTime) extends OrderDataType
case class OrderCancelled(id: Int, filled: Int, avgFillPrice: Double, time: DateTime) extends OrderDataType

abstract sealed class CalculatedType extends ReceiveType
case class StochasticPercentK(percentK: Double, time: DateTime) extends CalculatedType
case class StochasticPercentD(percentD: Double, time: DateTime) extends CalculatedType
case class MACDLine(value: Double, time: DateTime) extends CalculatedType
case class MACDSignal(value: Double, time: DateTime) extends CalculatedType
