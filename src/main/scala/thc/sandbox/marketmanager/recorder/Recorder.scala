package thc.sandbox.marketmanager.recorder

import scala.collection.mutable

import org.apache.commons.io.FileUtils
import org.joda.time.DateTime

import com.lmax.disruptor.EventHandler
import com.lmax.disruptor.ExceptionHandler

import thc.sandbox.marketmanager.MarketManager
import thc.sandbox.marketmanager.data.Container
import thc.sandbox.marketmanager.data.DataRequest
import thc.sandbox.marketmanager.data.ID


trait Recorder extends MarketManager {
	var recorderPath = ""
	private val begin = new DateTime().toString("yyyy-MM-dd'T'HH:mm:ss")
	private val fileRecorders: mutable.Map[Int, FileRecorder] = mutable.Map.empty
		
	//we don't want a fileRecorder error to crash the whole system, remove any filerecorders in error
	private val exceptionHandler = new ExceptionHandler {
		def handleEventException(ex: Throwable, seq: Long, event: Object) {
			logger.error(s"Removing FileRecorder with exception for event $event", ex)
			event match {
				case x: ID => fileRecorders.remove(x.id)
			}
		}

		def handleOnStartException(ex: Throwable) {
			logger.error("FileRecorder exception on start", ex)
		}
		
		def handleOnShutdownException(ex: Throwable) {
			logger.error("FileRecorder exception on stop", ex)
		}
	}
	
	private val eventHandler = new EventHandler[Container] {
		def onEvent(c: Container, pos: Long, endOfBatch: Boolean) {
			for {
				id <- c.id
				rec <- fileRecorders get id
				dataStr <- FileFormat.data2Str(c.value)
			} yield rec.writeLine(dataStr)
		}
	}
	marketData.handleEventsWith(eventHandler)
	marketData.handleExceptionsFor(eventHandler).`with`(exceptionHandler)
	
	override def onSubscribe(dr: DataRequest): Int = {
		val id = super.onSubscribe(dr)
		if(!(fileRecorders contains id)) {
			fileRecorders.put(id, new FileRecorder(FileUtils.getFile(recorderPath, s"${dr.symbol}@$begin")))
		}
		return id
	}
	
	override def stop() {
		fileRecorders foreach (x => {x._2.flush; x._2.close})
		super.stop()
	}
	
}