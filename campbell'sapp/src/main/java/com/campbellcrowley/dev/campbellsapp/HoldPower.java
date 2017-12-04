package com.campbellcrowley.dev.campbellsapp;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import static com.campbellcrowley.dev.campbellsapp.PressPower.HOLD_POWER;
import static com.campbellcrowley.dev.campbellsapp.PressPower.updateAppWidget;

/**
 * Implementation of App Widget functionality.
 */
public class HoldPower extends AppWidgetProvider {

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    // There may be multiple widgets active, so update all of them
    for (int appWidgetId : appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId, HOLD_POWER);
    }
  }
}

