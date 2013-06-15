package thc.sandbox.marketmanager

import com.lmax.disruptor.EventHandler
import thc.sandbox.marketmanager.data.ReceiveTypeContainer
import thc.sandbox.marketmanager.data.ReceiveType
import com.lmax.disruptor.RingBuffer
import thc.sandbox.marketmanager.data.RequestType

class MarketManager(val conn: MarketConnection) {

	val marketData: RingBuffer[ReceiveTypeContainer] = RingBuffer.createSingleProducer(ReceiveTypeContainer.factory, 512)
	
	def newReceiveDataHandler(rt: ReceiveType){
		val spot = marketData.next()
		marketData.get(spot).value = rt
		marketData.publish(spot)
	}
	
	def newRequestDataHandler(rt: RequestType) {
		
	}
	

}