package net.gitsaibot.af.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WindScaleTest {

	@Test
	public void scaleLeavesRoomAboveGustsAndThreshold() {
		double[] scale = AfDetailedWidget.computeWindScale(52.0, 40.0);
		assertEquals(60.0, scale[0], 0.0);
		assertEquals(20.0, scale[1], 0.0);
	}

	@Test
	public void calmDayStillShowsThreshold() {
		double[] scale = AfDetailedWidget.computeWindScale(8.0, 40.0);
		assertTrue(scale[0] >= 40.0 * 1.25);
	}

	@Test
	public void stormUsesLargerSteps() {
		double[] scale = AfDetailedWidget.computeWindScale(130.0, 40.0);
		assertTrue(scale[0] >= 130.0);
		assertTrue(scale[0] / scale[1] <= 4.0);
	}

	@Test
	public void scaleIsAWholeNumberOfSteps() {
		for (double max = 0; max <= 200; max += 7) {
			double[] scale = AfDetailedWidget.computeWindScale(max, 40.0);
			assertEquals(0.0, scale[0] % scale[1], 1e-9);
			assertTrue(scale[0] >= max);
		}
	}
}
