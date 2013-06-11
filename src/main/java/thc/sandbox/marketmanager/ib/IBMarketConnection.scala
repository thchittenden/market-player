package thc.sandbox.marketmanager.ib

import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConversions.mapAsScalaConcurrentMap
import scala.collection.concurrent
import scala.collection.mutable
import scala.concurrent.Lock

import org.joda.time.DateTime

import com.ib.client.CommissionReport
import com.ib.client.Contract
import com.ib.client.ContractDetails
import com.ib.client.EClientSocket
import com.ib.client.EWrapper
import com.ib.client.Execution
import com.ib.client.Order
import com.ib.client.OrderState
import com.ib.client.UnderComp

import TypeConverters.dataRequestAsContract
import TypeConverters.orderRequestAsContract
import TypeConverters.orderRequestAsOrder
import thc.sandbox.marketmanager.data.AskPrice
import thc.sandbox.marketmanager.data.AskSize
import thc.sandbox.marketmanager.data.BidPrice
import thc.sandbox.marketmanager.data.BidSize
import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.marketmanager.data.LastPrice
import thc.sandbox.marketmanager.data.LastSize
import thc.sandbox.marketmanager.data.MarketConnection
import thc.sandbox.marketmanager.data.MarketDataType
import thc.sandbox.marketmanager.data.OrderCancelled
import thc.sandbox.marketmanager.data.OrderDataType
import thc.sandbox.marketmanager.data.OrderFilled
import thc.sandbox.marketmanager.data.OrderRequest
import thc.sandbox.marketmanager.data.OrderStatus
import thc.sandbox.slf4s.Logger
import thc.sandbox.util.ActorGroup

class IBMarketConnection extends MarketConnection with EWrapper with Logger {
		
	//create clientSocket
	val clientSocket = new EClientSocket(this)
	
	var curTickerId = 0
	var curOrderId = 0
	
	//need a lock to prevent subscribe/unsubscribe conflicts
	val subscribeLock = new Lock();
	val orderIdLock = new Lock();
	
	//map both directions for efficient querying either way
	val openRequests: mutable.Map[DataRequest, Int] = mutable.HashMap.empty
	val openTickers:  mutable.Map[Int, DataRequest] = mutable.HashMap.empty
	
	//maps from ids to the recipients
	val dataReceivers: concurrent.Map[Int, ActorGroup] = new ConcurrentHashMap[Int, ActorGroup]()
	val orderReceivers: concurrent.Map[Int, ActorGroup] = new ConcurrentHashMap[Int, ActorGroup]()
	
	def connect(host: String, port: Int, clientId: Int) {
		logger.info(s"connecting to $host on port $port with clientId $clientId")
		clientSocket.eConnect(host, port, clientId)
	}
	
	val isConnected: (() => Boolean) = clientSocket.isConnected
	
	def disconnect() {
		logger.info("disconnecting...")
		if(!orderReceivers.isEmpty)
			logger.warn("orders still open!")
			
		clientSocket.eDisconnect()
	}
	
	def subscribe(ag: ActorGroup, dr: DataRequest): Int = {
		logger.info(s"subscribing $ag for $dr")
		
		subscribeLock.acquire
		if(openRequests contains dr) {
			//add actor to existing requests
			val tickerId = openRequests(dr)		
			
			dataReceivers.get(tickerId) foreach (_ ++= ag)
			
			subscribeLock.release
			return tickerId
			
		} else {
			//open new request
			val tickerId = curTickerId; curTickerId += 1;

			dataReceivers.put(tickerId, ag)
			openRequests.put(dr, tickerId)
			openTickers.put(tickerId, dr)
			clientSocket.reqMktData(tickerId, dr, "", false)
		
			subscribeLock.release
			return tickerId
		}
	}
	
	def unsubscribe(ag: ActorGroup, id: Int) {
		subscribeLock.acquire
		dataReceivers.get(id) foreach { curRecs => 
			curRecs --= ag
			if(curRecs.isEmpty) {
				clientSocket.cancelMktData(id)
				openTickers.remove(id) foreach (openRequests.remove(_))
				dataReceivers.remove(id)
			}
		}
		subscribeLock.release
	}
	

		
	def sendMessage(message: MarketDataType) {
		dataReceivers.get(message.id) foreach (_ ! message)
	}
	
	def sendMessage(message: OrderDataType) {
		orderReceivers.get(message.id) foreach (_ ! message)
	}
	
	def placeOrder(ag: ActorGroup, m: OrderRequest): Int = {
		orderIdLock.acquire
		val orderId = curOrderId; curOrderId += 1
		orderIdLock.release
		
		orderReceivers.put(orderId, ag) 
		clientSocket.placeOrder(orderId, m, m)
		return orderId
	}
	
	def cancelOrder(id: Int) { 
		clientSocket cancelOrder id
	}
	
	
	//EWrapperImpl
	def tickPrice(tickerId: Int, field: Int, price: Double, canAutoExecute: Int) {
		val now = new DateTime()
		field match {
			case 1 => sendMessage( BidPrice(tickerId, price, now))
			case 2 => sendMessage( AskPrice(tickerId, price, now))
			case 4 => sendMessage(LastPrice(tickerId, price, now))
			case _ => logger.debug(s"unsupported operation: tickPrice($tickerId, $field, $price, $canAutoExecute)")
		}
	}
	
	def tickSize(tickerId: Int, field: Int, size: Int) {
		val now = new DateTime()
		field match {
			case 0 => sendMessage( BidSize(tickerId, size, now))
			case 3 => sendMessage( AskSize(tickerId, size, now))
			case 5 => sendMessage(LastSize(tickerId, size, now))
			case _ => logger.info(s"unsupported operation: tickSize($tickerId, $field, $size)")
		}
	}
	def tickOptionComputation(tickerId: Int, field: Int, impliedVol: Double,
		delta: Double, optPrice: Double, pvDividend: Double,
		gamma: Double, vega: Double, theta: Double, undPrice: Double) {

	}
	def tickGeneric(tickerId: Int, tickType: Int, value: Double) {

	}
	def tickString(tickerId: Int, tickType: Int, value: String) {

	}
	def tickEFP(tickerId: Int, tickType: Int, basisPoints: Double,
		formattedBasisPoints: String, impliedFuture: Double, holdDays: Int,
		futureExpiry: String, dividendImpact: Double, dividendsToExpiry: Double) {

	}
	def orderStatus(orderId: Int, status: String, filled: Int, remaining: Int,
		avgFillPrice: Double, permId: Int, parentId: Int, lastFillPrice: Double,
		clientId: Int, whyHeld: String) {
			
		status match {
			case "Submitted" => sendMessage(OrderStatus(orderId, filled, avgFillPrice))
			case "Filled"    => sendMessage(OrderFilled(orderId, avgFillPrice))
								orderReceivers.remove(orderId)
			case "Cancelled" => sendMessage(OrderCancelled(orderId, filled, avgFillPrice))
								orderReceivers.remove(orderId)
			case _ 			 => logger.info(s"unsuppored order status: $status")
		}
		
	}
	def openOrder(orderId: Int, contract: Contract, order: Order, orderState: OrderState) {
		
	}
	
	def openOrderEnd() {
		
	}
	def updateAccountValue(key: String, value: String, currency: String, accountName: String) {

	}
	def updatePortfolio(contract: Contract, position: Int, marketPrice: Double, marketValue: Double,
		averageCost: Double, unrealizedPNL: Double, realizedPNL: Double, accountName: String) {

	}
	def updateAccountTime(timeStamp: String) {

	}
	def accountDownloadEnd(accountName: String) {

	}
	def nextValidId(orderId: Int) {
		curOrderId = orderId
	}
	def contractDetails(reqId: Int, contractDetails: ContractDetails) {

	}
	def bondContractDetails(reqId: Int, contractDetails: ContractDetails) {

	}
	def contractDetailsEnd(reqId: Int) {

	}
	def execDetails(reqId: Int, contract: Contract, execution: Execution) {

	}
	def execDetailsEnd(reqId: Int) {

	}
	def updateMktDepth(tickerId: Int, position: Int, operation: Int, side: Int, price: Double, size: Int) {

	}
	def updateMktDepthL2(tickerId: Int, position: Int, marketMaker: String, operation: Int,
		side: Int, price: Double, size: Int) {

	}
	def updateNewsBulletin(msgId: Int, msgType: Int, message: String, origExchange: String) {

	}
	def managedAccounts(accountsList: String) {

	}
	def receiveFA(faDataType: Int, xml: String) {

	}
	def historicalData(reqId: Int, date: String, open: Double, high: Double, low: Double,
		close: Double, volume: Int, count: Int, WAP: Double, hasGaps: Boolean) {

	}
	def scannerParameters(xml: String) {

	}
	def scannerData(reqId: Int, rank: Int, contractDetails: ContractDetails, distance: String,
		benchmark: String, projection: String, legsStr: String) {

	}
	def scannerDataEnd(reqId: Int) {

	}
	def realtimeBar(reqId: Int, time: Long, open: Double, high: Double, low: Double, close: Double, volume: Long, wap: Double, count: Int) {

	}
	def currentTime(time: Long) {
		logger.info(s"current market time: ${new DateTime(time).toString()}")
	}
	def fundamentalData(reqId: Int, data: String) {

	}
	def deltaNeutralValidation(reqId: Int, underComp: UnderComp) {

	}
	def tickSnapshotEnd(reqId: Int) {

	}
	def marketDataType(reqId: Int, marketDataType: Int) {

	}
	def commissionReport(commissionReport: CommissionReport) {

	}
	def error(ex: Exception) {
		logger.error(ex.getMessage)
	}
	def error(str: String) {
		logger.error(str)
	}
	def error(id: Int, errorCode: Int, errorMsg: String) {
		logger.error(s"id: $id, errorCode: $errorCode, errorMessage: $errorMsg")
	}
	def connectionClosed() {
		logger.info("Connection Closed!")
	}
}