package thc.sandbox.marketmanager.recorder

import scala.util.parsing.combinator.JavaTokenParsers

import org.joda.time.DateTime

import thc.sandbox.marketmanager.data.AskPrice
import thc.sandbox.marketmanager.data.AskSize
import thc.sandbox.marketmanager.data.BidPrice
import thc.sandbox.marketmanager.data.BidSize
import thc.sandbox.marketmanager.data.LastPrice
import thc.sandbox.marketmanager.data.LastSize
import thc.sandbox.marketmanager.data.ReceiveType
import thc.sandbox.marketmanager.data.archive.MarketDataParser
import thc.sandbox.slf4s.Logger

object FileFormat {
	val KEY_LASTPRICE = "LP"
	val KEY_ASKPRICE  = "AP"
	val KEY_BIDPRICE  = "BP"
	val KEY_LASTSIZE  = "LS"
	val KEY_ASKSIZE   = "AS"
	val KEY_BIDSIZE   = "BS"
		
	def data2Str(d: Any): Option[String] = { 
		return d match {
			case LastPrice(_, p, time) => Some(f"$KEY_LASTPRICE,${time.toString("yyyy/MM/dd'T'HH:mm:ss.SSS")},$p%2.2f")
			case AskPrice(_, p, time)  => Some(f"$KEY_ASKPRICE,${time.toString("yyyy/MM/dd'T'HH:mm:ss.SSS")},$p%2.2f")
			case BidPrice(_, p, time)  => Some(f"$KEY_BIDPRICE,${time.toString("yyyy/MM/dd'T'HH:mm:ss.SSS")},$p%2.2f")
			case LastSize(_, s, time)  => Some(f"$KEY_LASTSIZE,${time.toString("yyyy/MM/dd'T'HH:mm:ss.SSS")},$s")
			case AskSize(_, s, time)   => Some(f"$KEY_ASKSIZE,${time.toString("yyyy/MM/dd'T'HH:mm:ss.SSS")},$s")
			case BidSize(_, s, time)   => Some(f"$KEY_BIDSIZE,${time.toString("yyyy/MM/dd'T'HH:mm:ss.SSS")},$s")
			case _ => None
		}
	}
}

//KEY,time,value
class FileRecorderFileParser(id: Int) extends JavaTokenParsers with MarketDataParser with Logger {
	import FileFormat._
	def int: Parser[Int] = wholeNumber ^^ (_.toInt)
	def double: Parser[Double] = decimalNumber ^^ (_.toDouble)
	
	def datetime: Parser[DateTime] = int~"/"~int~"/"~int~"T"~int~":"~int~":"~int~"."~int ^^ 
		{case y~_~m~_~d~_~h~_~min~_~s~_~ms => new DateTime(y, m, d, h, min, s, ms)}

	def lastprice: Parser[LastPrice] = s"$KEY_LASTPRICE," ~ datetime ~ "," ~ double ^^ {case _~dt~","~price => LastPrice(id, price, dt)}
	def askprice: Parser[AskPrice] = s"$KEY_ASKPRICE," ~ datetime ~ "," ~ double ^^ {case _~dt~","~price => AskPrice(id, price, dt)}
	def bidprice: Parser[BidPrice] = s"$KEY_BIDPRICE," ~ datetime ~ "," ~ double ^^ {case _~dt~","~price => BidPrice(id, price, dt)}
	
	def lastsize: Parser[LastSize] = s"$KEY_LASTSIZE," ~ datetime ~ "," ~ int ^^ {case _~dt~","~size => LastSize(id, size, dt)}
	def asksize: Parser[AskSize] = s"$KEY_ASKSIZE," ~ datetime ~ "," ~ int ^^ {case _~dt~","~size => AskSize(id, size, dt)}
	def bidsize: Parser[BidSize] = s"$KEY_BIDSIZE," ~ datetime ~ "," ~ int ^^ {case _~dt~","~size => BidSize(id, size, dt)}

	def line: Parser[ReceiveType] = lastprice | askprice | bidprice | lastsize | asksize | bidsize

	//parseTime = None to prevent aggregating
	def parseTime(str: String): Option[DateTime] = None
	def parseLine(str: String): List[ReceiveType] = {
		val parsed = parse(line, str)
		if(parsed.isEmpty) {
			logger.warn(s"invalid line found: $str")
			List() 
		} else List(parsed.get)
	}
}