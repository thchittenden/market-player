package thc.sandbox.marketmanager.data.dummy

import thc.sandbox.marketmanager.MarketConnection
import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.marketmanager.data.OrderRequest
import collection._
import scala.concurrent.duration._
import scala.util.Random
import thc.sandbox.marketmanager.data.LastPrice
import org.joda.time.DateTime
import thc.sandbox.marketmanager.data.ReceiveType
import akka.actor.ActorSystem
import scala.concurrent.duration._
import akka.actor.Cancellable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import thc.sandbox.marketmanager.data.AskPrice
import thc.sandbox.marketmanager.data.BidPrice
import thc.sandbox.marketmanager.data.AskSize
import java.util.concurrent.atomic.AtomicInteger

class DummyMarketConnection extends MarketConnection {
		
	val as = ActorSystem("DummyMarketConnection")
	
	val r: Random = new Random()
	val openTickers: mutable.Map[Int, Cancellable] = mutable.Map.empty
	val curTickerId = new AtomicInteger(0)
	val curOrderId = new AtomicInteger(0)
	var callback: (Int, ReceiveType) => Unit = (_, _) => ()
	
	def registerCallback(cb: (Int, ReceiveType) => Unit) {
		callback = cb
	}
	
	def stop() {
		
	}
	
	def subscribe(dr: DataRequest): Int = {
		val tickerId = curTickerId.getAndIncrement
		createNewDataSpawner(tickerId)
		tickerId
	}
	
	def unsubscribe(tickerId: Int) {
		openTickers.remove(tickerId) foreach (_.cancel)
	}
	
	def nextOrderId() = curOrderId.getAndIncrement()
	def placeOrder(or: OrderRequest) {
		//its dumb, remember
	}
	def cancelOrder(orderId: Int) {
		//yeahhh nada
	}
	
	
	
	def createNewDataSpawner(id: Int) {
		val centerPoint = r.nextInt(400) + 50
		val variance = 0.01
		openTickers.put(id, as.scheduler.schedule(0 milliseconds, 1 second)(sendRandomData(id, centerPoint, variance)))
	}
	
	def sendRandomData(id: Int, center: Double, variance: Double) {
		val now = new DateTime()
		val last = r.nextGaussian * variance + center
		val ask = last + 0.05
		val bid = last - 0.05
		callback(id, LastPrice(id, last, now))
		callback(id, AskPrice(id, ask, now))
		callback(id, BidPrice(id, bid, now))
		callback(id, AskSize(id, r.nextInt(20), now))
	}
	
}