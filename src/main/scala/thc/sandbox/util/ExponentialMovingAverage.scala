package thc.sandbox.util

class ExponentialMovingAverage(initial: Double, val weight: Double) {
	
	var avg: Double = initial
	
	def add(x: Double): Double = {
		avg += weight*(x - avg)
		return avg
	}
	
	def reset(initial: Double) { avg = initial }
	
	
}