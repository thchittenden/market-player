package thc.sandbox.marketmanager.visualizer

import info.monitorenter.gui.chart.rangepolicies.ARangePolicy
import info.monitorenter.gui.chart.labelformatters.ALabelFormatter
import org.joda.time.DateTime


class RangePolicyPadded(val padding: Double) extends ARangePolicy {
	
	def getMax(chartmin: Double, chartmax: Double): Double = chartmax + padding
	
	def getMin(chartmin: Double, chartmax: Double): Double = chartmin - padding
	
}

class LabelFormatterTimeAgo extends ALabelFormatter {
	
	def now: Double = new DateTime().getMillis()
	
	val getMinimumValueShiftForChange: Double = 100
	
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