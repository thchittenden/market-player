package thc.sandbox.marketmanager.benchmarker

import thc.sandbox.marketmanager.MarketManager
import thc.sandbox.marketmanager.data.OrderRequest
import org.joda.time.DateTime
import thc.sandbox.marketmanager.data.ReceiveType
import thc.sandbox.marketmanager.data.OrderStatus
import thc.sandbox.marketmanager.data.SimpleStockMarketBuyOrder
import thc.sandbox.marketmanager.data.SimpleStockMarketSellOrder
import thc.sandbox.marketmanager.data.SimpleStockLimitBuyOrder
import thc.sandbox.marketmanager.data.SimpleStockLimitSellOrder
import collection._
import thc.sandbox.marketmanager.data.OrderFilled
import thc.sandbox.marketmanager.data.OrderCancelled

//TODO complete this, is MM level benchmarker even appropriate?
//do we even need a benchmarker?
trait Benchmarker extends MarketManager {

	val completedOrders: mutable.ListBuffer[OrderFilled] = mutable.ListBuffer.empty
	val buyOrders: mutable.Set[Int] = mutable.HashSet.empty
	val sellOrders: mutable.Set[Int] = mutable.HashSet.empty

	//val 
	
	val startTime = new DateTime()
	var orderCount = 0
	
	override def onNewData(id: Int, dr: ReceiveType) = {
				super.onNewData(id, dr);
		dr match {
			case data @ OrderStatus(id, filled, avgFillPrice, time) =>  {
				
			}
			case data @ OrderFilled(id, filled, avgFillPrice, time) => {
				completedOrders += data
			}
			case OrderCancelled(id, filled, avgFillPrice, time) if filled != 0 => {
				completedOrders += OrderFilled(id, filled, avgFillPrice, time)
			}
			case _ =>
		}
	}
	
	override def onNewOrder(or: OrderRequest) {
		super.onNewOrder(or)
		orderCount += 1
		or match {
			case  SimpleStockMarketBuyOrder(id: Int, symbol: String, quantity: Int) => buyOrders += id
			case  SimpleStockMarketSellOrder(id: Int, symbol: String, quantity: Int) => sellOrders += id
			case  SimpleStockLimitBuyOrder(id: Int, symbol: String, quantity: Int, price: Double) => buyOrders += id
			case  SimpleStockLimitSellOrder(id: Int, symbol: String, quantity: Int, price: Double) => sellOrders += id
		}
	}
	
	override def stop() {
		printBenchmarkReport()
		super.stop()
	}
	
	def printBenchmarkReport() {
		println("benchmark report")
	}
}