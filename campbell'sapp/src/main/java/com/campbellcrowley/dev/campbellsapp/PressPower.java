package com.campbellcrowley.dev.campbellsapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import static com.campbellcrowley.dev.campbellsapp.PCStatusFragment.sendAction;

/**
 * Implementation of App Widget functionality.
 */
public class PressPower extends AppWidgetProvider {

  public static final String PRESS_POWER = "com.campbellcrowley.dev.campbellsapp.PRESSPOWER_WIDGET_CLICK";
  public static final String HOLD_POWER = "com.campbellcrowley.dev.campbellsapp.HOLDPOWER_WIDGET_CLICK";
  public static final String PRESS_RESET = "com.campbellcrowley.dev.campbellsapp.PRESSRESET_WIDGET_CLICK";
  public static final String WAKE = "com.campbellcrowley.dev.campbellsapp.WAKE_WIDGET_CLICK";

  public static void updateAppWidget(final Context context, AppWidgetManager appWidgetManager,
                                     int appWidgetId, String action) {

    CharSequence widgetText;
    switch (action) {
      case PRESS_POWER:
      default:
        widgetText = context.getString(R.string.button_presspower);
        break;
      case HOLD_POWER:
        widgetText = context.getString(R.string.button_holdpower);
        break;
      case PRESS_RESET:
        widgetText = context.getString(R.string.button_pressreset);
        break;
      case WAKE:
        widgetText = context.getString(R.string.button_wake);
        break;
    }
    // Construct the RemoteViews object
    RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.press_power);
    views.setTextViewText(R.id.widget_button, widgetText);

    Intent intent = new Intent(context, PressPower.class);
    intent.setAction(action);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    views.setOnClickPendingIntent(R.id.widget_button, pendingIntent);

    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views);
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);
    final String moreText = "This could still fail. Please ensure you are signed in and have permission to do this for this.";

    if (SettingsActivity.getBoolean(SettingsActivity.KEY_NOTIF_WIDGET_EVENT)) {
      if (PRESS_POWER.equals(intent.getAction())) {
        sendAction("press-power");
        Log.i("Press-Power Widget", "Pressed Power!");
        TextNotificationManager.notify(context, "Sent request to press power.", moreText, 0);
      } else if (HOLD_POWER.equals(intent.getAction())) {
        sendAction("hold-power");
        Log.i("Press-Power Widget", "Held Power!");
        TextNotificationManager.notify(context, "Sent request to hold power.", moreText, 0);
      } else if (PRESS_RESET.equals(intent.getAction())) {
        sendAction("press-reset");
        Log.i("Press-Power Widget", "Pressed Reset!");
        TextNotificationManager.notify(context, "Sent request to press reset.", moreText, 0);
      } else if (WAKE.equals(intent.getAction())) {
        sendAction("wake");
        Log.i("Press-Power Widget", "Sent magic packet!");
        TextNotificationManager.notify(context, "Sent request to wake computer.", moreText, 0);
      }
    }
  }

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    // There may be multiple widgets active, so update all of them
    for (int appWidgetId : appWidgetIds) {
      updateAppWidget(context, appWidgetManager, appWidgetId, PRESS_POWER);
    }
  }
}
