package thc.sandbox.marketmanager.dummy

import thc.sandbox.marketmanager.data.MarketConnection
import thc.sandbox.util.ActorGroup
import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.marketmanager.data.OrderRequest
import collection._
import akka.actor.ActorSystem
import scala.concurrent.duration._
import scala.util.Random
import thc.sandbox.marketmanager.data.LastPrice
import org.joda.time.DateTime
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import akka.actor.Cancellable


class DummyMarketConnection(implicit val as: ActorSystem) extends MarketConnection {
	
	var curTickerId = 0;
	
	val openRequests: mutable.Map[DataRequest, Int] = mutable.HashMap.empty
	val openTickers:  mutable.Map[Int, DataRequest] = mutable.HashMap.empty
	val dataReceivers: concurrent.Map[Int, ActorGroup] = new ConcurrentHashMap[Int, ActorGroup]()
	val dataSpawners: concurrent.Map[Int, Cancellable] = new ConcurrentHashMap[Int, Cancellable]()
	
	val r = new Random()
	
	def subscribe(ag: ActorGroup, dr: DataRequest): Int = {
		if(openRequests contains dr) {
			//add actor to existing requests
			val tickerId = openRequests(dr)		
			
			dataReceivers.get(tickerId) foreach (_ ++= ag)
			
			return tickerId
			
		} else {
			//open new request
			val tickerId = curTickerId; curTickerId += 1;

			dataReceivers.put(tickerId, ag)
			openRequests.put(dr, tickerId)
			openTickers.put(tickerId, dr)
			
			val center = (r.nextDouble * 100) + 50
			val spawner: Cancellable = as.scheduler.schedule(Duration.Zero, Duration.create(500, MILLISECONDS))(dataReceivers.get(tickerId) foreach (_ ! getLastPrice(tickerId, center)))
			dataSpawners.put(tickerId, spawner)
			return tickerId
		}
	}
	
	def getLastPrice(id: Int, center: Double): LastPrice = LastPrice(id, center + (r.nextGaussian)/10, new DateTime())
	
	def unsubscribe(ag: ActorGroup, id: Int) {
				dataReceivers.get(id) foreach { curRecs => 
			curRecs --= ag
			if(curRecs.isEmpty) {
				openTickers.remove(id) foreach (openRequests.remove(_))
				dataReceivers.remove(id)
				dataSpawners.remove(id) foreach (c => c.cancel)
			}
		}
	}

	def placeOrder(ag: ActorGroup, or: OrderRequest): Int = {
		return 0
	}
	
	def cancelOrder(id: Int) {
		
	}
	
}