package net.gitsaibot.af.data;

import android.content.Context;
import android.util.Log;

import net.gitsaibot.af.AfUpdate;
import net.gitsaibot.af.AfUtils;
import net.gitsaibot.af.util.AfLocationInfo;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Finds the time zone of a location with Open-Meteo (free, no account needed).
 * Replaces the GeoNames time zone lookup of the original app.
 */
public class AfTimeZoneData implements AfDataSource {

	public final static String TAG = "AfTimeZoneData";

	private final Context mContext;
	private final AfUpdate mAfUpdate;

	private AfTimeZoneData(Context context, AfUpdate afUpdate) {
		mContext = context;
		mAfUpdate = afUpdate;
	}

	public static AfTimeZoneData build(Context context, AfUpdate afUpdate) {
		return new AfTimeZoneData(context, afUpdate);
	}

	public void update(AfLocationInfo afLocationInfo, long currentUtcTime) throws AfDataUpdateException {
		String timeZone = afLocationInfo.getTimeZone();
		if (timeZone != null) timeZone = timeZone.trim();

		if (timeZone != null && !timeZone.isEmpty() && !timeZone.equalsIgnoreCase("null")) {
			return; // Already known
		}

		Double latitude = afLocationInfo.getLatitude();
		Double longitude = afLocationInfo.getLongitude();

		if (latitude == null || longitude == null) {
			throw new AfDataUpdateException("Missing location information. Latitude/Longitude was null");
		}

		mAfUpdate.updateWidgetRemoteViews("Getting timezone data...", false);

		String buildUrl = String.format(Locale.US,
				"https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
				"&timezone=auto&forecast_days=1&current=temperature_2m",
				latitude, longitude);

		Log.d(TAG, "Retrieving timezone data from URL=" + buildUrl);

		try {
			HttpURLConnection httpClient = AfUtils.setupHttpClient(new URL(buildUrl), mContext);

			int code = httpClient.getResponseCode();
			if (code != 200) {
				if (code == 429) {
					throw new AfDataUpdateException(buildUrl, AfDataUpdateException.Reason.RATE_LIMITED);
				}
				throw new IOException("Invalid response from server: " + code);
			}

			InputStream content = AfUtils.getGzipInputStream(httpClient);
			JSONObject jObject = new JSONObject(AfUtils.convertStreamToString(content));

			timeZone = jObject.getString("timezone");
			Log.d(TAG, "Parsed TimeZone='" + timeZone + "'");

			afLocationInfo.setTimeZone(timeZone);
			afLocationInfo.commit(mContext);
		} catch (AfDataUpdateException e) {
			throw e;
		} catch (Exception e) {
			Log.d(TAG, "Failed to retrieve timezone data. (" + e.getMessage() + ")");
			throw new AfDataUpdateException();
		}
	}
}
