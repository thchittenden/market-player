package thc.sandbox.util

class Stochastic(period: Int, dSmoothing: Int, ddSmoothing: Int) {
	
	private val hiQueue = new MovingMax(period)
	private val loQueue = new MovingMin(period)
	
	private var warmingUp = 0
	private var percentKcalc: Double = 50.0
	private val percentDcalc = new ExponentialMovingAverage(50.0, 2.0/(dSmoothing + 1))
	private val percentDslowcalc = new ExponentialMovingAverage(50.0, 2.0/(ddSmoothing + 1))
	
	def add(x: Double): Double = {
		hiQueue.add(x)
		loQueue.add(x)
		
		if(warmingUp < period) {
			warmingUp += 1
			if(warmingUp == period) {
				percentKcalc = 100*(x - loQueue.min)/(hiQueue.max - loQueue.min)
				percentDcalc.reset(percentKcalc)
				percentDslowcalc.reset(percentDcalc.avg)
			}
			return percentKcalc
		}
		
		percentKcalc = 100*(x - loQueue.min)/(hiQueue.max - loQueue.min)
		percentDcalc.add(percentKcalc)
		percentDslowcalc.add(percentDcalc.avg)
		
		return percentKcalc
	}
	
	def percentK = percentKcalc
	def percentD = percentDcalc.avg
	def percentDslow = percentDslowcalc.avg

	
}