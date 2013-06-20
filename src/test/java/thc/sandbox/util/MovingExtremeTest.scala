package thc.sandbox.util

import org.junit.Test
import org.junit.Assert._


class MovingExtremeTest2 {

	val testData: List[Double] = List(1, 4, 2, 3, 5, 8, 12, 4, 9, 29, 1, 2, 6, 2, 4, 1, 9, 2)
	
	@Test def testMovingMax() {
		val period = 5
		val movingMax = new MovingMax(period)
				
		//add first four elements
		for(x <- testData.slice(0, 4)) movingMax.add(x)
		
		//iterate through the window
		for(window <- testData.sliding(period)) {
			println(window)
			movingMax.add(window(4))
			assertEquals(window.max, movingMax.max, 0.001)
		}
	}
	
	@Test def testMovingMaxStartup() {
		val period = 3
		val movingMax = new MovingMax(period)
		movingMax.add(1.0)
		assertEquals(1.0, movingMax.max, 0.001)
		
		movingMax.add(2.0)
		assertEquals(2.0, movingMax.max, 0.001)
		
		movingMax.add(1.5)
		assertEquals(2.0, movingMax.max, 0.001)
		
		movingMax.add(1.0)
		assertEquals(2.0, movingMax.max, 0.001)
		
		movingMax.add(1.0)
		assertEquals(1.5, movingMax.max, 0.001)
	}
	
}