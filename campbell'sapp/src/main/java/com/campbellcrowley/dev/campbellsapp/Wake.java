package com.campbellcrowley.dev.campbellsapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import static com.campbellcrowley.dev.campbellsapp.PressPower.WAKE;
import static com.campbellcrowley.dev.campbellsapp.PressPower.updateAppWidget;

/**
 * Created by campbell on 12/3/2017.
 */

public class Wake extends AppWidgetProvider {
  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    // There may be multiple widgets active, so update all of them
    for (int appWidgetId : appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId, WAKE);
    }
  }
}
