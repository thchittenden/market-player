package thc.sandbox.marketmanager.strategies

import thc.sandbox.marketmanager.data.OrderDataType
import thc.sandbox.marketmanager.data.MarketDataType
import com.lmax.disruptor.RingBuffer
import thc.sandbox.marketmanager.data.ReceiveTypeContainer
import thc.sandbox.marketmanager.data.RequestTypeContainer


abstract class Strategy {
		
	val orderQueue: RingBuffer[]
	val dataQueue: RingBuffer[]
	
}