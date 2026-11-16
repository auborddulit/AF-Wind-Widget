package net.gitsaibot.af;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AfSettingsBackupTest {

	private static Map<String, Object> samplePreferences() {
		Map<String, Object> prefs = new HashMap<>();
		prefs.put("wind_enabled_bool", true);
		prefs.put("wind_threshold_string", "45");
		prefs.put("wind_color_int", 0xFF2EC990);
		prefs.put("background_color_int", 0xC8222222);
		prefs.put("global_temp_provider_string", "1");
		prefs.put("global_temp_wifi_only_bool", false);
		// Not backed up
		prefs.put("global_temp_dp_calibrationPortraitDimension_string", "100x200");
		prefs.put("global_provider_string", "1");
		prefs.put("backup_wind_color_int", 0xFF000000);
		prefs.put("exact_dimension_long", 12L);
		prefs.put("no_type_suffix", "x");
		return prefs;
	}

	@Test
	public void keepsWidgetAndChosenGlobalSettings() throws JSONException {
		JSONObject json = AfSettingsBackup.settingsToJson(samplePreferences());
		assertTrue(json.has("wind_enabled_bool"));
		assertTrue(json.has("wind_threshold_string"));
		assertTrue(json.has("wind_color_int"));
		assertTrue(json.has("global_temp_provider_string"));
		assertTrue(json.has("global_temp_wifi_only_bool"));
		assertFalse(json.has("global_temp_dp_calibrationPortraitDimension_string"));
		assertFalse(json.has("global_provider_string"));
		assertFalse(json.has("backup_wind_color_int"));
		assertFalse(json.has("exact_dimension_long"));
		assertFalse(json.has("no_type_suffix"));
	}

	@Test
	public void roundTripKeepsValuesAndTypes() throws JSONException {
		JSONObject json = AfSettingsBackup.settingsToJson(samplePreferences());
		Map<String, Object> restored = AfSettingsBackup.settingsFromJson(new JSONObject(json.toString()));
		assertEquals(Boolean.TRUE, restored.get("wind_enabled_bool"));
		assertEquals("45", restored.get("wind_threshold_string"));
		assertEquals(0xFF2EC990, restored.get("wind_color_int"));
		assertEquals(0xC8222222, restored.get("background_color_int"));
		assertEquals("1", restored.get("global_temp_provider_string"));
		assertEquals(6, restored.size());
	}

	@Test(expected = JSONException.class)
	public void rejectsOtherFiles() throws JSONException {
		AfSettingsBackup.checkFormat("{\"hello\":\"world\"}");
	}

	@Test
	public void acceptsBackupFiles() throws JSONException {
		JSONObject root = AfSettingsBackup.checkFormat("{\"format\":\"af-wind-settings\",\"version\":1}");
		assertEquals(1, root.getInt("version"));
	}
}
