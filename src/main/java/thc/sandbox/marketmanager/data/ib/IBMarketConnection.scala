package thc.sandbox.marketmanager.data.ib

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
import thc.sandbox.marketmanager.MarketConnection
import thc.sandbox.marketmanager.data.AskPrice
import thc.sandbox.marketmanager.data.AskSize
import thc.sandbox.marketmanager.data.BidPrice
import thc.sandbox.marketmanager.data.BidSize
import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.marketmanager.data.LastPrice
import thc.sandbox.marketmanager.data.LastSize
import thc.sandbox.marketmanager.data.OrderCancelled
import thc.sandbox.marketmanager.data.OrderFilled
import thc.sandbox.marketmanager.data.OrderRequest
import thc.sandbox.marketmanager.data.OrderStatus
import thc.sandbox.marketmanager.data.ReceiveType
import thc.sandbox.slf4s.Logger

class IBMarketConnection extends MarketConnection with EWrapper with Logger {

	var callback = (id: Int, x: ReceiveType) => logger.error(s"received data without callback! $x")
	def registerCallback(cb: (Int, ReceiveType) => Unit) {
		callback = cb;
	}
	
	//create clientSocket
	val clientSocket = new EClientSocket(this)
	
	var curTickerId = 0
	var curOrderId = 0
	
	//map both directions for efficient querying either way
	val openRequests: mutable.Map[DataRequest, Int] = mutable.HashMap.empty
	val openTickers:  mutable.Map[Int, DataRequest] = mutable.HashMap.empty

	//subscription logic
	private def subscribeNew(dr: DataRequest): Int = {
		val tickerId = curTickerId; curTickerId += 1;

		openRequests.put(dr, tickerId)
		openTickers.put(tickerId, dr)
		clientSocket.reqMktData(tickerId, dr, "", false)
		
		return tickerId
	}
	
	def subscribe(dr: DataRequest): Int = 
		openRequests getOrElse(dr, subscribeNew(dr))
	
	def unsubscribe(id: Int) = 
		openTickers.remove(id) flatMap (dr => openRequests.remove(dr)) foreach (id => clientSocket.cancelMktData(id))


	// order logic
	def placeOrder(or: OrderRequest): Int = {
		val orderId = curOrderId; curOrderId += 1
		clientSocket.placeOrder(orderId, or, or)
		return orderId
	}
	
	def cancelOrder(id: Int) { 
		clientSocket cancelOrder id
	}
	
	def connect(host: String, port: Int, clientId: Int) {
		logger.info(s"connecting to $host on port $port with clientId $clientId")
		clientSocket.eConnect(host, port, clientId)
	}
	
	def isConnected(): Boolean = clientSocket.isConnected
	def disconnect() {
		logger.info("disconnecting...")
		clientSocket.eDisconnect
	}
	
	// EWrapperImpl
	def sendMessage = callback
	
	def tickPrice(tickerId: Int, field: Int, price: Double, canAutoExecute: Int) {
		val now = new DateTime()
		field match {
			case 1 => sendMessage(tickerId,  BidPrice(tickerId, price, now))
			case 2 => sendMessage(tickerId,  AskPrice(tickerId, price, now))
			case 4 => sendMessage(tickerId, LastPrice(tickerId, price, now))
			case _ => logger.debug(s"unsupported operation: tickPrice($tickerId, $field, $price, $canAutoExecute)")
		}
	}
	
	def tickSize(tickerId: Int, field: Int, size: Int) {
		val now = new DateTime()
		field match {
			case 0 => sendMessage(tickerId,  BidSize(tickerId, size, now))
			case 3 => sendMessage(tickerId,  AskSize(tickerId, size, now))
			case 5 => sendMessage(tickerId, LastSize(tickerId, size, now))
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
			
		val now = new DateTime()
		status match {
			case "Submitted" => sendMessage(orderId, OrderStatus(orderId, filled, avgFillPrice, now))
			case "Filled"    => sendMessage(orderId, OrderFilled(orderId, filled, avgFillPrice, now))
			case "Cancelled" => sendMessage(orderId, OrderCancelled(orderId, filled, avgFillPrice, now))
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
