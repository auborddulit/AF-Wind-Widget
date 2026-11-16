package net.gitsaibot.af;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

import androidx.preference.PreferenceManager;

import net.gitsaibot.af.AfProvider.AfLocations;
import net.gitsaibot.af.AfProvider.AfLocationsColumns;
import net.gitsaibot.af.util.AfLocationInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Backup and restore of the settings of the widget being configured, plus the saved locations,
 * as a small JSON file the user picks (Downloads, Google Drive...).
 *
 * While the configuration screen is open, the settings being edited live in the default
 * SharedPreferences: widget settings without prefix, app-wide settings as "global_temp_*".
 * A restore writes them back there, so they only take effect when "Apply changes" is pressed.
 */
public final class AfSettingsBackup {

	static final String FORMAT = "af-wind-settings";
	static final int VERSION = 1;

	/** App-wide settings included in a backup (the screen calibration is device specific). */
	static final String[] GLOBAL_KEYS = {
			"global_temp_provider_string",
			"global_temp_update_rate_string",
			"global_temp_awake_only_bool",
			"global_temp_wifi_only_bool" };

	private AfSettingsBackup() { }

	public static class RestoreResult {
		public int numSettings;
		public int numLocationsAdded;
		public AfLocationInfo selectedLocation;
	}

	/* ---------- Pure part (unit tested) ---------- */

	static boolean isBackedUpKey(String key) {
		if (key == null) return false;
		for (String globalKey : GLOBAL_KEYS) {
			if (globalKey.equals(key)) return true;
		}
		if (key.startsWith("global") || key.startsWith("backup") || key.startsWith("exact")) {
			return false;
		}
		return key.endsWith("bool") || key.endsWith("int") || key.endsWith("long") || key.endsWith("string");
	}

	/** Settings to JSON; the value type follows the key suffix, as in AfSettings. */
	static JSONObject settingsToJson(Map<String, ?> preferences) throws JSONException {
		JSONObject settings = new JSONObject();
		for (Map.Entry<String, ?> entry : preferences.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (!isBackedUpKey(key) || value == null) continue;

			if (key.endsWith("bool") && value instanceof Boolean) {
				settings.put(key, (boolean) (Boolean) value);
			} else if (key.endsWith("int") && value instanceof Integer) {
				settings.put(key, (int) (Integer) value);
			} else if (key.endsWith("long") && value instanceof Long) {
				settings.put(key, (long) (Long) value);
			} else if (key.endsWith("string") && value instanceof String) {
				settings.put(key, value);
			}
		}
		return settings;
	}

	/** JSON to settings with the right Java types; unknown or badly typed entries are skipped. */
	static Map<String, Object> settingsFromJson(JSONObject settings) {
		Map<String, Object> result = new HashMap<>();
		if (settings == null) return result;

		Iterator<String> keys = settings.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			if (!isBackedUpKey(key)) continue;
			try {
				if (key.endsWith("bool")) {
					result.put(key, settings.getBoolean(key));
				} else if (key.endsWith("int")) {
					result.put(key, settings.getInt(key));
				} else if (key.endsWith("long")) {
					result.put(key, settings.getLong(key));
				} else if (key.endsWith("string")) {
					result.put(key, settings.getString(key));
				}
			} catch (JSONException e) {
				// Skip this entry
			}
		}
		return result;
	}

	static JSONObject checkFormat(String json) throws JSONException {
		JSONObject root = new JSONObject(json);
		if (!FORMAT.equals(root.optString("format"))) {
			throw new JSONException("Not an AF Wind settings file");
		}
		return root;
	}

	/* ---------- Android part ---------- */

	public static String buildBackup(Context context, AfLocationInfo currentLocation) throws JSONException {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		JSONObject root = new JSONObject();
		root.put("format", FORMAT);
		root.put("version", VERSION);
		root.put("created", System.currentTimeMillis());
		root.put("settings", settingsToJson(prefs.getAll()));

		JSONArray locations = new JSONArray();
		Cursor c = context.getContentResolver().query(AfLocations.CONTENT_URI, null, null, null, null);
		if (c != null) {
			try {
				while (c.moveToNext()) {
					JSONObject location = new JSONObject();
					location.put("title", getString(c, AfLocationsColumns.TITLE));
					location.put("title_detailed", getString(c, AfLocationsColumns.TITLE_DETAILED));
					location.put("latitude", getDouble(c, AfLocationsColumns.LATITUDE));
					location.put("longitude", getDouble(c, AfLocationsColumns.LONGITUDE));
					location.put("time_zone", getString(c, AfLocationsColumns.TIME_ZONE));
					locations.put(location);
				}
			} finally {
				c.close();
			}
		}
		root.put("locations", locations);

		if (currentLocation != null && currentLocation.getLatitude() != null && currentLocation.getLongitude() != null) {
			JSONObject selected = new JSONObject();
			selected.put("title", currentLocation.getTitle());
			selected.put("latitude", currentLocation.getLatitude());
			selected.put("longitude", currentLocation.getLongitude());
			root.put("selected_location", selected);
		}

		return root.toString(2);
	}

	public static RestoreResult restoreBackup(Context context, String json) throws Exception {
		JSONObject root = checkFormat(json);
		RestoreResult result = new RestoreResult();

		// Settings: written to the editing state, applied with "Apply changes"
		Map<String, Object> settings = settingsFromJson(root.optJSONObject("settings"));
		SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
		for (Map.Entry<String, Object> entry : settings.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) value);
			else if (value instanceof Integer) editor.putInt(entry.getKey(), (Integer) value);
			else if (value instanceof Long) editor.putLong(entry.getKey(), (Long) value);
			else if (value instanceof String) editor.putString(entry.getKey(), (String) value);
		}
		editor.commit();
		result.numSettings = settings.size();

		// Locations: add the ones that are missing
		ContentResolver resolver = context.getContentResolver();
		JSONArray locations = root.optJSONArray("locations");
		if (locations != null) {
			for (int i = 0; i < locations.length(); i++) {
				JSONObject location = locations.optJSONObject(i);
				if (location == null || !location.has("latitude") || !location.has("longitude")) continue;

				double latitude = location.getDouble("latitude");
				double longitude = location.getDouble("longitude");
				if (findLocation(resolver, latitude, longitude) != null) continue;

				ContentValues values = new ContentValues();
				values.put(AfLocationsColumns.TITLE, location.optString("title", ""));
				values.put(AfLocationsColumns.TITLE_DETAILED, location.optString("title_detailed", ""));
				values.put(AfLocationsColumns.LATITUDE, latitude);
				values.put(AfLocationsColumns.LONGITUDE, longitude);
				String timeZone = location.optString("time_zone", "");
				if (!timeZone.isEmpty() && !timeZone.equals("null")) {
					values.put(AfLocationsColumns.TIME_ZONE, timeZone);
				}
				if (resolver.insert(AfLocations.CONTENT_URI, values) != null) {
					result.numLocationsAdded++;
				}
			}
		}

		// Location shown by the widget when the backup was made
		JSONObject selected = root.optJSONObject("selected_location");
		if (selected != null && selected.has("latitude") && selected.has("longitude")) {
			Uri locationUri = findLocation(resolver, selected.getDouble("latitude"), selected.getDouble("longitude"));
			if (locationUri != null) {
				result.selectedLocation = AfLocationInfo.build(context, locationUri);
			}
		}

		return result;
	}

	private static Uri findLocation(ContentResolver resolver, double latitude, double longitude) {
		Cursor c = resolver.query(AfLocations.CONTENT_URI, null, null, null, null);
		if (c == null) return null;
		try {
			while (c.moveToNext()) {
				Double lat = getDouble(c, AfLocationsColumns.LATITUDE);
				Double lon = getDouble(c, AfLocationsColumns.LONGITUDE);
				if (lat != null && lon != null
						&& Math.abs(lat - latitude) < 0.001 && Math.abs(lon - longitude) < 0.001) {
					long id = c.getLong(c.getColumnIndexOrThrow(AfLocations._ID));
					return ContentUris.withAppendedId(AfLocations.CONTENT_URI, id);
				}
			}
		} finally {
			c.close();
		}
		return null;
	}

	private static String getString(Cursor c, String column) {
		int i = c.getColumnIndex(column);
		return (i == -1 || c.isNull(i)) ? null : c.getString(i);
	}

	private static Double getDouble(Cursor c, String column) {
		int i = c.getColumnIndex(column);
		return (i == -1 || c.isNull(i)) ? null : c.getDouble(i);
	}
}
