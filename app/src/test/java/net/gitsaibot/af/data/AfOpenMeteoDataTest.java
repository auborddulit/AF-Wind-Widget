package net.gitsaibot.af.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import net.gitsaibot.af.AfUtils;

import org.junit.Test;

public class AfOpenMeteoDataTest {

	private static final String SAMPLE = "{"
			+ "\"latitude\":42.56,\"longitude\":2.34,\"timezone\":\"Europe/Paris\","
			+ "\"hourly\":{"
			+ "\"time\":[1763337600,1763341200,1763344800],"
			+ "\"temperature_2m\":[7.1,6.8,null],"
			+ "\"relative_humidity_2m\":[74,76,80],"
			+ "\"pressure_msl\":[1012.3,1012.0,1011.8],"
			+ "\"precipitation\":[0.0,1.2,0.4],"
			+ "\"weather_code\":[3,61,null],"
			+ "\"wind_speed_10m\":[18.0,22.5,25.1],"
			+ "\"wind_gusts_10m\":[38.0,45.7,52.2],"
			+ "\"wind_direction_10m\":[290,300,310]"
			+ "}}";

	@Test
	public void parsesHourlyValuesIncludingNulls() {
		AfOpenMeteoData.OpenMeteoResponse r = AfOpenMeteoData.parse(SAMPLE);
		assertNotNull(r);
		assertEquals("Europe/Paris", r.timezone);
		assertEquals(3, r.hourly.time.size());
		assertEquals(1763337600L, (long) r.hourly.time.get(0));
		assertEquals(7.1f, AfOpenMeteoData.get(r.hourly.temperature, 0), 0.001f);
		assertNull(AfOpenMeteoData.get(r.hourly.temperature, 2));
		assertEquals(45.7f, AfOpenMeteoData.get(r.hourly.windGusts, 1), 0.001f);
		assertEquals(25.1f, AfOpenMeteoData.get(r.hourly.windSpeed, 2), 0.001f);
		assertNull(r.hourly.weatherCode.get(2));
		assertNull(AfOpenMeteoData.get(r.hourly.windSpeed, 10));
	}

	@Test
	public void mapsWeatherCodesToIcons() {
		assertEquals(AfUtils.WEATHER_ICON_CLEARSKY, AfOpenMeteoData.mapWeatherCode(0));
		assertEquals(AfUtils.WEATHER_ICON_CLOUDY, AfOpenMeteoData.mapWeatherCode(3));
		assertEquals(AfUtils.WEATHER_ICON_LIGHTRAIN, AfOpenMeteoData.mapWeatherCode(61));
		assertEquals(AfUtils.WEATHER_ICON_HEAVYSNOW, AfOpenMeteoData.mapWeatherCode(75));
		assertEquals(AfUtils.WEATHER_ICON_RAINSHOWERANDTHUNDER, AfOpenMeteoData.mapWeatherCode(95));
		assertEquals(0, AfOpenMeteoData.mapWeatherCode(null));
		assertEquals(0, AfOpenMeteoData.mapWeatherCode(42));
	}
}
