package net.gitsaibot.af;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class AfIconSetsTest {

	private static void assertCovers(String name, int[] reference, int[] set) {
		assertEquals(name + " size", reference.length, set.length);
		for (int i = 0; i < reference.length; i++) {
			if (reference[i] != 0) {
				assertNotEquals(name + " has no icon for MET code " + (i + 1), 0, set[i]);
			}
		}
	}

	@Test
	public void monochromeSetsCoverEveryWeatherCode() {
		assertCovers("Meteocons day", AfUtils.WEATHER_ICONS_DAY, AfIconSets.METEOCONS_DAY);
		assertCovers("Meteocons night", AfUtils.WEATHER_ICONS_NIGHT, AfIconSets.METEOCONS_NIGHT);
		assertCovers("Material day", AfUtils.WEATHER_ICONS_DAY, AfIconSets.MATERIAL_DAY);
		assertCovers("Material night", AfUtils.WEATHER_ICONS_NIGHT, AfIconSets.MATERIAL_NIGHT);
		assertCovers("Meteocons Flat day", AfUtils.WEATHER_ICONS_DAY, AfIconSets.METEOCONS_FLAT_DAY);
		assertCovers("Meteocons Flat night", AfUtils.WEATHER_ICONS_NIGHT, AfIconSets.METEOCONS_FLAT_NIGHT);
		assertCovers("Meteocons Line day", AfUtils.WEATHER_ICONS_DAY, AfIconSets.METEOCONS_LINE_DAY);
		assertCovers("Meteocons Line night", AfUtils.WEATHER_ICONS_NIGHT, AfIconSets.METEOCONS_LINE_NIGHT);
	}
}
