package thc.sandbox.marketmanager.data.archive

import scala.io.BufferedSource
import scala.io.Source
import scala.util.parsing.combinator._

import org.joda.time.DateTime

import thc.sandbox.marketmanager.MarketConnection
import thc.sandbox.marketmanager.data.AskPrice
import thc.sandbox.marketmanager.data.BidPrice
import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.marketmanager.data.LastPrice
import thc.sandbox.marketmanager.data.LastSize
import thc.sandbox.marketmanager.data.OrderRequest
import thc.sandbox.marketmanager.data.ReceiveType

class ArchiveMarketConnection extends MarketConnection {

	val aggregate = false
	val delay = 50
	val id = 0
	
	var parser: MarketDataParser = new DefaultParser(id)

	def setParser(p: MarketDataParser) {
		parser = p
	}
	
	
	var fileSourceOption: Option[BufferedSource] = None
	
	def setFile(filename: String) {
		fileSourceOption = Some(Source.fromFile(filename, "ASCII"))
	}
	
	val fileReader = new Runnable() {
		var lastSecond: Option[DateTime] = None
		var curData: List[ReceiveType] = List.empty
		
		def processAggregate() {
			logger.debug("starting file playback")
			for {fileSource <- fileSourceOption.toIterable; line <- fileSource.getLines } {
				val curSecond = parser.parseTime(line)
				val sameSecond = (for {
					last <- lastSecond
					cur <- curSecond
				} yield last == cur) getOrElse false
				
				if(sameSecond) { //aggregate data
					if(curData.isEmpty) curData = parser.parseLine(line)
					else {
						curData = for (zippedData <- curData zip parser.parseLine(line))
							yield zippedData match {
								case (LastPrice(id, p1, now), LastPrice(_, p2, _)) => LastPrice(id, (p1+p2)/2, now)
								case (AskPrice(id, p1, now), AskPrice(_, p2, _)) => AskPrice(id, (p1+p2)/2, now)
								case (BidPrice(id, p1, now), BidPrice(_, p2, _)) => BidPrice(id, (p1+p2)/2, now)
								case (LastSize(id, s1, now), LastSize(_, s2, _)) => LastSize(id, s1+s2, now)
								case (_, _) => ??? //throws NotImplementedError, SCALA FUCK YEAH
							}
					}
				} else { //send data
					for(data <- curData) { callback(id, data) }
					curData = List.empty
					Thread.sleep(delay)
				}
								
				lastSecond = curSecond
			}
		}
		
		def processNoAggregate() {
			for {fileSource <- fileSourceOption.toIterable
				 line <- fileSource.getLines 
				 data <- parser.parseLine(line)} {
					 callback(id, data)
					 Thread.sleep(delay)
				 }
		}
		
		def run() {
			if(aggregate) processAggregate()
			else processNoAggregate()
		}

	}
	
	//give everyone the same ID
	def subscribe(dr: DataRequest) = {
		new Thread(fileReader).start(); id
	}

	def unsubscribe(tickerId: Int) { }
	
	def nextOrderId() = {
		0
	}
	
	def placeOrder(or: OrderRequest) {
		//TODO
	}
	
	def cancelOrder(orderId: Int) {
		//TODO
	}
	
	def stop() {
		
	}
	
}

trait MarketDataParser {
	def parseTime(str: String): Option[DateTime]
	def parseLine(str: String): List[ReceiveType] 
}

// date,time,price,bid,ask,size
// 09/28/2009,09:10:37,35.6,35.29,35.75,150
class DefaultParser(val id: Int) extends JavaTokenParsers with MarketDataParser{
	def int: Parser[Int] = wholeNumber ^^ (_.toInt)
	def double: Parser[Double] = decimalNumber ^^ (_.toDouble)
	
	def date: Parser[DateTime] = int ~ "/" ~ int ~ "/" ~ int ^^ { case m~"/"~d~"/"~y => new DateTime(y, m, d, 0, 0, 0, 0)}
	def time: Parser[DateTime] = int ~ ":" ~ int ~ ":" ~ int ^^ { case h~":"~m~":"~s => new DateTime(1970, 1, 1, h, m, s, 0)}
	def datetime: Parser[DateTime] = date ~ "," ~ time ^^ { case date~","~time => date.plus(time.getMillis())}

	def price: Parser[Double] = decimalNumber ^^ (_.toDouble)
	def ask: Parser[Double] = decimalNumber ^^ (_.toDouble)
	def bid: Parser[Double] = decimalNumber ^^ (_.toDouble)
	def size: Parser[Int] = wholeNumber ^^ (_.toInt)
	
	def line: Parser[List[ReceiveType]] = datetime ~","~ price ~","~ bid ~","~ ask ~","~ size ^^ 
				{case dt~","~price~","~bid~","~ask~","~size => List(LastPrice(id, price, dt), BidPrice(id, bid, dt), AskPrice(id, ask, dt), LastSize(id, size/100, dt)) }	

	def parseTime(str: String): Option[DateTime] = parse(datetime, str) map (Some(_)) getOrElse None
	def parseLine(str: String): List[ReceiveType] = parse(line, str) getOrElse List()
}