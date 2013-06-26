package thc.sandbox.util

import scala.collection._

class SimpleMovingAverage[T : Fractional](window: Int) {

	private val data: mutable.Queue[T] = new mutable.Queue[T]
	
	private val n: Fractional[T] = implicitly[Fractional[T]]
	import n._
	
	private val nWindow = fromInt(window)
	var avg: T = zero
	
	def add(x : T): T = {
		if(data.length == window) {
			val out: T = data.dequeue
			data.enqueue(x)
			
			avg = avg + (x - out)/nWindow
		} else {
			val csize: T = fromInt(data.size)
			val nsize = csize + one
			data.enqueue(x)
			
			avg = (csize*avg + x)/nsize
		}
		return avg
	}
	
	
}