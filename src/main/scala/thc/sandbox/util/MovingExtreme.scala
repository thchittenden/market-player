package thc.sandbox.util

import scala.collection.mutable.PriorityQueue
import scala.collection.mutable.Stack
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ArrayStack


class MovingExtreme(val period: Int, val op: (Double, Double) => Double, id: Double) {
	
	/**
	 * Maintains the extreme of elements inserted into the stack
	 */
	private class ExtremeStack {
		val stack = new ArrayStack[(Double, Double)]
		var extreme: Double = id
	
		def size = stack.size
		def isEmpty = stack.isEmpty
		def clear = { stack.clear; extreme = id; }
		
		def push(x: Double) {
			extreme = op(x, extreme)
			stack.push((x, extreme))
		}	 
		def pushAll(xs: TraversableOnce[(Double, Double)]): Unit = xs foreach (tup => push (tup._1))
		
		def pop() {
			stack.pop()
			
			if(stack.isEmpty) extreme = id
			else extreme = stack.top._2
		}
	}
	
	private var warmingUp = true
	private val enqueueStack: ExtremeStack = new ExtremeStack
	private val dequeueStack: ExtremeStack = new ExtremeStack
	
	protected var extreme: Double = id
	
	def add(x: Double): Double = {
		if(warmingUp) {
			enqueueStack.push(x)
			extreme = enqueueStack.extreme
			if(enqueueStack.size == period)
				warmingUp = false
		} else {
			if(dequeueStack.isEmpty) {
				dequeueStack.pushAll(enqueueStack.stack)
				enqueueStack.clear
			}
			dequeueStack.pop
			enqueueStack.push(x)
			extreme = op(enqueueStack.extreme, dequeueStack.extreme) 
		}
		return extreme
	}
	
}

class MovingMax(period: Int) extends MovingExtreme(period, (d1, d2) => d1 max d2, Double.MinValue) {
	def max = extreme
}
class MovingMin(period: Int) extends MovingExtreme(period, (d1, d2) => d1 min d2, Double.MaxValue) {
	def min = extreme
}