package net.gitsaibot.af.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import net.gitsaibot.af.AfProvider.AfIntervalDataForecastColumns;
import net.gitsaibot.af.AfProvider.AfIntervalDataForecasts;
import net.gitsaibot.af.AfProvider.AfPointDataForecastColumns;
import net.gitsaibot.af.AfProvider.AfPointDataForecasts;
import net.gitsaibot.af.AfUpdate;
import net.gitsaibot.af.AfUtils;
import net.gitsaibot.af.util.AfLocationInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Weather data from Open-Meteo, using the Météo-France models
 * (AROME 1.5-2.5 km for the first two days, then ARPEGE).
 * Free, no API key: https://open-meteo.com/en/docs/meteofrance-api
 * Data licence: CC BY 4.0 (Open-Meteo / Météo-France).
 */
public class AfOpenMeteoData implements AfDataSource {

	public static final String TAG = "AfOpenMeteoData";

	private static final String HOURLY_VARIABLES =
			"temperature_2m,relative_humidity_2m,pressure_msl,precipitation,weather_code," +
			"wind_speed_10m,wind_gusts_10m,wind_direction_10m";

	private final Context mContext;
	private final AfUpdate mAfUpdate;

	private AfOpenMeteoData(Context context, AfUpdate afUpdate) {
		mContext = context;
		mAfUpdate = afUpdate;
	}

	public static AfOpenMeteoData build(Context context, AfUpdate afUpdate) {
		return new AfOpenMeteoData(context, afUpdate);
	}

	/** Maps WMO weather codes (as computed by Open-Meteo) to the widget's MET Norway icons. */
	static int mapWeatherCode(Integer code) {
		if (code == null) return 0;
		return switch (code) {
			case 0 -> AfUtils.WEATHER_ICON_CLEARSKY;
			case 1 -> AfUtils.WEATHER_ICON_FAIR;
			case 2 -> AfUtils.WEATHER_ICON_PARTLYCLOUD;
			case 3 -> AfUtils.WEATHER_ICON_CLOUDY;
			case 45, 48 -> AfUtils.WEATHER_ICON_FOG;
			case 51, 53, 55 -> AfUtils.WEATHER_ICON_LIGHTRAIN;
			case 56, 57 -> AfUtils.WEATHER_ICON_LIGHTSLEET;
			case 61 -> AfUtils.WEATHER_ICON_LIGHTRAIN;
			case 63 -> AfUtils.WEATHER_ICON_RAIN;
			case 65 -> AfUtils.WEATHER_ICON_HEAVYRAIN;
			case 66 -> AfUtils.WEATHER_ICON_LIGHTSLEET;
			case 67 -> AfUtils.WEATHER_ICON_SLEET;
			case 71, 77 -> AfUtils.WEATHER_ICON_LIGHTSNOW;
			case 73 -> AfUtils.WEATHER_ICON_SNOW;
			case 75 -> AfUtils.WEATHER_ICON_HEAVYSNOW;
			case 80 -> AfUtils.WEATHER_ICON_LIGHTRAINSHOWERS;
			case 81 -> AfUtils.WEATHER_ICON_RAINSHOWERS;
			case 82 -> AfUtils.WEATHER_ICON_HEAVYRAINSHOWERS;
			case 85 -> AfUtils.WEATHER_ICON_LIGHTSNOWSHOWERS;
			case 86 -> AfUtils.WEATHER_ICON_HEAVYSNOWSHOWERS;
			case 95 -> AfUtils.WEATHER_ICON_RAINSHOWERANDTHUNDER;
			case 96, 97, 99 -> AfUtils.WEATHER_ICON_HEAVYRAINSHOWERSANDTHUNDER;
			default -> 0;
		};
	}

	@Override
	public void update(AfLocationInfo afLocationInfo, long currentUtcTime)
			throws AfDataUpdateException {
		Log.d(TAG, "update(): Started Open-Meteo update. (afLocationInfo=" + afLocationInfo + ")");

		mAfUpdate.updateWidgetRemoteViews("Downloading weather data...", false);

		Double latitude = afLocationInfo.getLatitude();
		Double longitude = afLocationInfo.getLongitude();

		if (latitude == null || longitude == null) {
			throw new AfDataUpdateException("Missing location information. Latitude/Longitude was null");
		}

		String buildUrl = String.format(Locale.US,
				"https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
				"&models=meteofrance_seamless&hourly=%s" +
				"&wind_speed_unit=kmh&timeformat=unixtime&timezone=auto" +
				"&past_days=1&forecast_days=3",
				latitude, longitude, HOURLY_VARIABLES);

		HttpURLConnection connection = null;

		try {
			Log.d(TAG, "Downloading weather data from URL=" + buildUrl);

			connection = AfUtils.setupHttpClient(new URL(buildUrl), mContext);
			connection.connect();

			int code = connection.getResponseCode();
			if (code != HttpURLConnection.HTTP_OK) {
				if (code == 429) {
					throw new AfDataUpdateException(buildUrl, AfDataUpdateException.Reason.RATE_LIMITED);
				}
				throw new IOException("Invalid response from server: " + code);
			}

			InputStream content = AfUtils.getGzipInputStream(connection);
			String json = AfUtils.convertStreamToString(content);

			if (json == null || json.isEmpty()) {
				throw new AfDataUpdateException("API response was empty.");
			}

			mAfUpdate.updateWidgetRemoteViews("Parsing weather data...", false);

			OpenMeteoResponse response = new Gson().fromJson(json, OpenMeteoResponse.class);
			if (response == null || response.hourly == null || response.hourly.time == null) {
				throw new AfDataUpdateException("Failed to parse JSON or response was incomplete.");
			}

			Hourly h = response.hourly;
			int n = h.time.size();

			ArrayList<ContentValues> pointDataValues = new ArrayList<>(n);
			ArrayList<ContentValues> intervalDataValues = new ArrayList<>(n);

			long lastTime = -1;

			for (int i = 0; i < n; i++) {
				Long t = h.time.get(i);
				if (t == null) continue;
				long time = t * 1000L;
				lastTime = Math.max(lastTime, time);

				Float temperature = get(h.temperature, i);

				// Point data (instant values)
				if (temperature != null) {
					ContentValues cv = new ContentValues();
					cv.put(AfPointDataForecastColumns.LOCATION, afLocationInfo.getId());
					cv.put(AfPointDataForecastColumns.TIME_ADDED, currentUtcTime);
					cv.put(AfPointDataForecastColumns.TIME, time);
					cv.put(AfPointDataForecastColumns.TEMPERATURE, temperature);
					putIfNotNull(cv, AfPointDataForecastColumns.HUMIDITY, get(h.humidity, i));
					putIfNotNull(cv, AfPointDataForecastColumns.PRESSURE, get(h.pressure, i));
					putIfNotNull(cv, AfPointDataForecastColumns.WIND_SPEED, get(h.windSpeed, i));
					putIfNotNull(cv, AfPointDataForecastColumns.WIND_GUST, get(h.windGusts, i));
					putIfNotNull(cv, AfPointDataForecastColumns.WIND_DIRECTION, get(h.windDirection, i));
					pointDataValues.add(cv);
				}

				// Interval data: Open-Meteo precipitation is the sum over the preceding hour.
				Float precipitation = get(h.precipitation, i);
				Integer weatherCode = (h.weatherCode != null && i < h.weatherCode.size()) ? h.weatherCode.get(i) : null;

				if (precipitation != null || weatherCode != null) {
					ContentValues cv = new ContentValues();
					cv.put(AfIntervalDataForecastColumns.LOCATION, afLocationInfo.getId());
					cv.put(AfIntervalDataForecastColumns.TIME_ADDED, currentUtcTime);
					cv.put(AfIntervalDataForecastColumns.TIME_FROM, time - DateUtils.HOUR_IN_MILLIS);
					cv.put(AfIntervalDataForecastColumns.TIME_TO, time);

					float rain = precipitation != null ? precipitation : 0.0f;
					cv.put(AfIntervalDataForecastColumns.RAIN_VALUE, rain);
					cv.put(AfIntervalDataForecastColumns.RAIN_MINVAL, rain);
					cv.put(AfIntervalDataForecastColumns.RAIN_MAXVAL, rain);

					int icon = mapWeatherCode(weatherCode);
					if (icon != 0) {
						cv.put(AfIntervalDataForecastColumns.WEATHER_ICON, icon);
					}
					intervalDataValues.add(cv);
				}
			}

			if (pointDataValues.isEmpty()) {
				throw new AfDataUpdateException("No usable forecast values in response.");
			}

			// Use the time zone found by Open-Meteo if the location does not have one yet.
			if (response.timezone != null && !response.timezone.isEmpty() &&
					(afLocationInfo.getTimeZone() == null || afLocationInfo.getTimeZone().trim().isEmpty())) {
				afLocationInfo.setTimeZone(response.timezone);
			}

			ContentResolver resolver = mContext.getContentResolver();
			resolver.bulkInsert(AfPointDataForecasts.CONTENT_URI, pointDataValues.toArray(new ContentValues[0]));
			resolver.bulkInsert(AfIntervalDataForecasts.CONTENT_URI, intervalDataValues.toArray(new ContentValues[0]));

			resolver.update(AfPointDataForecasts.CONTENT_URI, null, null, null);
			resolver.update(AfIntervalDataForecasts.CONTENT_URI, null, null, null);

			Log.d(TAG, String.format("update(): %d point entries, %d interval entries.",
					pointDataValues.size(), intervalDataValues.size()));

			// AROME is refreshed every 3 hours (hourly for the short range); refresh hourly.
			afLocationInfo.setLastForecastUpdate(currentUtcTime);
			afLocationInfo.setForecastValidTo(lastTime > 0 ? lastTime : currentUtcTime + DateUtils.DAY_IN_MILLIS);
			afLocationInfo.setNextForecastUpdate(currentUtcTime + DateUtils.HOUR_IN_MILLIS);
			afLocationInfo.commit(mContext);

		} catch (AfDataUpdateException e) {
			throw e;
		} catch (Exception e) {
			Log.e(TAG, "update failed", e);
			throw new AfDataUpdateException(e.toString());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static Float get(List<Float> list, int i) {
		if (list == null || i >= list.size()) return null;
		Float v = list.get(i);
		return (v == null || v.isNaN()) ? null : v;
	}

	private static void putIfNotNull(ContentValues cv, String key, Float value) {
		if (value != null) cv.put(key, value);
	}

	// DATA MODELS

	private static class OpenMeteoResponse {
		@SerializedName("timezone")
		public String timezone;
		@SerializedName("hourly")
		public Hourly hourly;
	}

	private static class Hourly {
		@SerializedName("time")
		public List<Long> time;
		@SerializedName("temperature_2m")
		public List<Float> temperature;
		@SerializedName("relative_humidity_2m")
		public List<Float> humidity;
		@SerializedName("pressure_msl")
		public List<Float> pressure;
		@SerializedName("precipitation")
		public List<Float> precipitation;
		@SerializedName("weather_code")
		public List<Integer> weatherCode;
		@SerializedName("wind_speed_10m")
		public List<Float> windSpeed;
		@SerializedName("wind_gusts_10m")
		public List<Float> windGusts;
		@SerializedName("wind_direction_10m")
		public List<Float> windDirection;
	}
}
