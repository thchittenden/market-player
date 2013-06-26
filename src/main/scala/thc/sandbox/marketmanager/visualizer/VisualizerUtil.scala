package thc.sandbox.marketmanager.visualizer

import info.monitorenter.gui.chart.rangepolicies.ARangePolicy
import info.monitorenter.gui.chart.labelformatters.ALabelFormatter
import org.joda.time.DateTime

class RangePolicyPadded(val paddinglo: Double, val paddinghi: Double) extends ARangePolicy {
	def this(padding: Double) = this(padding, padding)
	
	def getMax(chartmin: Double, chartmax: Double): Double = chartmax + paddinghi
	def getMin(chartmin: Double, chartmax: Double): Double = chartmin - paddinglo
}

class RangePolicyFixedLookback(val frame: Double) extends ARangePolicy {
	def getMax(chartmin: Double, chartmax: Double): Double = chartmax
	def getMin(chartmin: Double, chartmax: Double): Double = chartmax - frame
}

class RangePolicyFixed(val low: Double, val hi: Double) extends ARangePolicy {
	def getMax(chartmin: Double, chartmax: Double): Double = hi
	def getMin(chartmin: Double, chartmax: Double): Double = low
}

class LabelFormatterTimeAgo extends ALabelFormatter {
	
	def now: Double = new DateTime().getMillis()
	
	val getMinimumValueShiftForChange: Double = 1000
	
	var lastFormatted: Double = 0
	
	def getNextEvenValue(point: Double, ceiling: Boolean): Double = {
		val p: Double = point - (point % 1000)
		if(ceiling)	p + 1000
		else		p
				
	}
	
	def format(point: Double): String = {
		lastFormatted = point
		f"${(now - point)/1000}%2.0f"
	}
	
	def parse(str: String): Number = lastFormatted
}