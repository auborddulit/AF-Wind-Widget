package net.gitsaibot.af;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.gitsaibot.af.AfProvider.AfPointDataForecastColumns;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class PointDataInsertTest {

	@Test
	public void insertHasOnePlaceholderPerColumn() {
		String sql = AfProvider.buildPointDataInsertSql();
		long placeholders = sql.chars().filter(c -> c == '?').count();
		assertEquals(AfProvider.POINT_DATA_INSERT_COLUMNS.length, placeholders);
	}

	@Test
	public void insertStoresTheWindColumns() {
		List<String> columns = Arrays.asList(AfProvider.POINT_DATA_INSERT_COLUMNS);
		assertTrue(columns.contains(AfPointDataForecastColumns.WIND_SPEED));
		assertTrue(columns.contains(AfPointDataForecastColumns.WIND_GUST));
		assertTrue(columns.contains(AfPointDataForecastColumns.WIND_DIRECTION));
	}

	@Test
	public void everyInsertedColumnIsReadBack() {
		List<String> readColumns = Arrays.asList(AfPointDataForecastColumns.ALL_COLUMNS);
		for (String column : AfProvider.POINT_DATA_INSERT_COLUMNS) {
			assertTrue("Column not queried: " + column, readColumns.contains(column));
		}
	}
}
