package thc.sandbox.marketmanager.ib

import com.ib.client.Contract
import com.ib.client.Order
import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.marketmanager.data.OrderRequest
import thc.sandbox.marketmanager.data.SimpleRTStockRequest
import thc.sandbox.marketmanager.data.SimpleStockMarketBuyOrder
import thc.sandbox.marketmanager.data.SimpleStockMarketSellOrder
import thc.sandbox.marketmanager.data.SimpleStockLimitSellOrder
import thc.sandbox.marketmanager.data.SimpleStockLimitBuyOrder

object TypeConverters {
		
	private final val STOCK_SEC_TYPE = "STK"
	private final val CURRENCY = "USD"
	private final val EXCHANGE = "SMART"
	private final val BUY = "BUY"
	private final val SELL = "SELL"
	private final val MARKET_ORDER_TYPE = "MKT"
	private final val LIMIT_ORDER_TYPE = "LMT"
		
	implicit def dataRequestAsContract(dr: DataRequest): Contract = {
		val ret = new Contract()
		ret.m_currency = CURRENCY
		ret.m_exchange = EXCHANGE
		dr match {
			case SimpleRTStockRequest(symbol) => ret.m_symbol = symbol
												 ret.m_secType = STOCK_SEC_TYPE
		}
		return ret
	}
	
	implicit def orderRequestAsContract(or: OrderRequest): Contract = {
		val ret = new Contract()
		ret.m_currency = CURRENCY
		ret.m_exchange = EXCHANGE
		or match {
			case SimpleStockMarketBuyOrder(symbol: String, _) => 
				ret.m_symbol = symbol
				ret.m_secType = STOCK_SEC_TYPE
			case SimpleStockMarketSellOrder(symbol: String, _) =>
				ret.m_symbol = symbol
				ret.m_secType = STOCK_SEC_TYPE
			case SimpleStockLimitBuyOrder(symbol: String, _, _) =>
				ret.m_symbol = symbol
				ret.m_secType = STOCK_SEC_TYPE
			case SimpleStockLimitSellOrder(symbol: String, _, _) =>
				ret.m_symbol = symbol
				ret.m_secType = STOCK_SEC_TYPE
		}
		return ret
	}
	
	implicit def orderRequestAsOrder(or: OrderRequest): Order = {
		val ret = new Order()
		or match {
			case SimpleStockMarketBuyOrder(symbol: String, quantity: Int) => 
				ret.m_totalQuantity = quantity
				ret.m_action = BUY
				ret.m_orderType = MARKET_ORDER_TYPE
			case SimpleStockMarketSellOrder(symbol: String, quantity: Int) =>
				ret.m_totalQuantity = quantity
				ret.m_action = SELL
				ret.m_orderType = MARKET_ORDER_TYPE	
			case SimpleStockLimitBuyOrder(symbol: String, quantity: Int, limit: Double) =>
				ret.m_totalQuantity = quantity
				ret.m_lmtPrice = limit
				ret.m_action = BUY
				ret.m_orderType = LIMIT_ORDER_TYPE
			case SimpleStockLimitSellOrder(symbol: String, quantity: Int, limit: Double) =>
				ret.m_totalQuantity = quantity
				ret.m_lmtPrice = limit
				ret.m_action = SELL
				ret.m_orderType = LIMIT_ORDER_TYPE
		}
		return ret
	}
	
}