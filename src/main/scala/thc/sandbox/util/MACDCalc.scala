package thc.sandbox.util

class MACDCalc(p1: Int, p2: Int, p3: Int) {

	private val stEMA = new ExponentialMovingAverage(0, 2.0/(p1+1))
	private val ltEMA = new ExponentialMovingAverage(0, 2.0/(p2+1))
	private val signalEMA = new ExponentialMovingAverage(0, 2.0/(p3+1))
	
	def macd = stEMA.avg - ltEMA.avg
	def signal = signalEMA.avg

	var warmingUp = true
	
	def add(x: Double) {
		if(warmingUp == true) {
			warmingUp = false
			stEMA.reset(x)
			ltEMA.reset(x)
			signalEMA.reset(0)
		} else {
			stEMA.add(x)
			ltEMA.add(x)
			signalEMA.add(macd)
		}
	}
	
	def reset(x: Double) {
		stEMA.reset(x)
		ltEMA.reset(x)
		signalEMA.reset(x)
	}
}