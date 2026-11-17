package net.gitsaibot.af;

import net.gitsaibot.af.AfProvider.AfWidgets;
import net.gitsaibot.af.util.AfWidgetInfo;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class AfWidget extends AppWidgetProvider {

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		for (int appWidgetId : appWidgetIds) {
			Intent updateIntent = new Intent(AfWorker.ACTION_DELETE_WIDGET)
					.setData(ContentUris.withAppendedId(AfWidgets.CONTENT_URI, appWidgetId));
			AfWorkManager.enqueueWork(context, updateIntent);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds)
	{
		if (appWidgetIds == null) {
			appWidgetIds = appWidgetManager.getAppWidgetIds(
					new ComponentName(context, AfWidget.class));
		}
		for (int appWidgetId : appWidgetIds) {
			Intent updateIntent = new Intent(AfWorker.ACTION_UPDATE_WIDGET)
					.setData(ContentUris.withAppendedId(AfWidgets.CONTENT_URI, appWidgetId));
			AfWorkManager.enqueueWork(context, updateIntent);
		}
	}
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        int minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        int maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        int minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        int maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);

        float density = context.getResources().getDisplayMetrics().density;

        final int portW_px = (int)(minWidth * density);
        final int portH_px = (int)(maxHeight * density);

        final int landW_px = (int)(maxWidth * density);
        final int landH_px = (int)(minHeight * density);

        // Only the size is saved here. Redrawing directly from this callback raced with the
        // calibration screen and the scheduled updates (flicker), and some launchers call it
        // again after every redraw.
        AfSettings settingsProbe = null;
        try {
            Uri widgetUri = ContentUris.withAppendedId(AfWidgets.CONTENT_URI, appWidgetId);
            AfWidgetInfo widgetInfo = AfWidgetInfo.build(context, widgetUri);
            widgetInfo.loadSettings(context);
            settingsProbe = AfSettings.build(context, widgetInfo);
            settingsProbe.loadSettings();

            boolean sizeChanged = !AfSettings.hasExactDimensions(context, appWidgetId, portW_px, portH_px, landW_px, landH_px);
            AfSettings.saveExactDimensions(context, appWidgetId, portW_px, portH_px, landW_px, landH_px);

            boolean calibrating = settingsProbe.getCalibrationTarget() == appWidgetId;
            boolean usesCalibratedSize = settingsProbe.getCachedUseSpecificDimensions();

            if (sizeChanged && !calibrating && !usesCalibratedSize) {
                AfWorkManager.enqueueWork(context, new Intent(AfWorker.ACTION_UPDATE_WIDGET).setData(widgetUri));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
