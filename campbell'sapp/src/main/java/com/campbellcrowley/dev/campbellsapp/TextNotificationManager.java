package com.campbellcrowley.dev.campbellsapp;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Helper class for showing and canceling text manager
 * notifications.
 * <p>
 * This class makes heavy use of the {@link NotificationCompat.Builder} helper
 * class to create notifications in a backward-compatible way.
 */
public class TextNotificationManager {
  /**
   * The unique identifier for this type of notification.
   */
  private static final String NOTIFICATION_TAG = "TextManager";

  /**
   * Shows the notification, or updates a previously shown notification of
   * this type, with the given parameters.
   * <p>
   * TODO: Customize this method's arguments to present relevant content in
   * the notification.
   * <p>
   * TODO: Customize the contents of this method to tweak the behavior and
   * presentation of text manager notifications. Make
   * sure to follow the
   * <a href="https://developer.android.com/design/patterns/notifications.html">
   * Notification design guidelines</a> when doing so.
   *
   * @see #cancel(Context)
   */
  public static void notify(final Context context,
                            final String exampleString, final int number, final long eventTime) {
    boolean notificationsEnabled = SettingsActivity.getBoolean(SettingsActivity.KEY_NOTIF_GLOBAL_ENABLE);
    Log.i("NotificationManager", "Notifications enabled: " + notificationsEnabled);
    if (!notificationsEnabled) return;

    final Resources res = context.getResources();

    // This image is used as the notification's large icon (thumbnail).
    // TODO: Remove this if your notification has no relevant thumbnail.
    final Bitmap picture = BitmapFactory.decodeResource(res, R.drawable.example_picture);


    final long finalEventTime = eventTime == 0 ? System.currentTimeMillis() : eventTime;
    final String ticker = exampleString;
    final String title = res.getString(
            R.string.text_manager_notification_title_template, exampleString);
    final String text = res.getString(
            R.string.text_manager_notification_placeholder_text_template, exampleString);

    final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "PCStatusChannel")

            // Set appropriate defaults for the notification light, sound,
            // and vibration.
            .setDefaults(Notification.DEFAULT_ALL)

            // Set required fields, including the small icon, the
            // notification title, and text.
            .setSmallIcon(R.drawable.ic_stat_text_manager)
            .setContentTitle(title)
            .setContentText(text)

            // All fields below this line are optional.

            // Use a default priority (recognized on devices running Android
            // 4.1 or later)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            // Provide a large icon, shown with the notification in the
            // notification drawer on devices running Android 3.0 or later.
            .setLargeIcon(picture)

            // Set ticker text (preview) information for this notification.
            .setTicker(ticker)

            // Show a number. This is useful when stacking notifications of
            // a single type.
            .setNumber(number)

            // If this notification relates to a past or upcoming event, you
            // should set the relevant time information using the setWhen
            // method below. If this call is omitted, the notification's
            // timestamp will by set to the time at which it was shown.
            .setWhen(finalEventTime)

            // Set the pending intent to be initiated when the user touches
            // the notification.
            .setContentIntent(
                    PendingIntent.getActivity(
                            context,
                            0,
                            new Intent(Intent.ACTION_VIEW, Uri.parse("https://dev.campbellcrowley.com/pc")),
                            PendingIntent.FLAG_UPDATE_CURRENT))

            // Show expanded text content on devices running Android 4.1 or
            // later.
            .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(text)
                    .setBigContentTitle(title)
                    .setSummaryText("Sent action"))

            // Example additional actions for this notification. These will
            // only show on devices running Android 4.1 or later, so you
            // should ensure that the activity in this notification's
            // content intent provides access to the same actions in
            // another way.
            /* .addAction(
                    R.drawable.ic_action_stat_share,
                    res.getString(R.string.action_share),
                    PendingIntent.getActivity(
                            context,
                            0,
                            Intent.createChooser(new Intent(Intent.ACTION_SEND)
                                    .setType("text/plain")
                                    .putExtra(Intent.EXTRA_TEXT, "Dummy text"), "Dummy title"),
                            PendingIntent.FLAG_UPDATE_CURRENT))
            .addAction(
                    R.drawable.ic_action_stat_reply,
                    res.getString(R.string.action_reply),
                    null)*/

            // Automatically dismiss the notification when it is touched.
            .setAutoCancel(true);

    NotificationChannel mChannel = null;
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      mChannel = new NotificationChannel("PCStatusChannel", title, NotificationManager.IMPORTANCE_DEFAULT);
    }

    notify(context, builder.build(), mChannel);
  }

  @TargetApi(Build.VERSION_CODES.ECLAIR)
  private static void notify(final Context context, final Notification notification, final NotificationChannel channel) {
    final NotificationManager nm = (NotificationManager) context
            .getSystemService(Context.NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      nm.createNotificationChannel(channel);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
      nm.notify(NOTIFICATION_TAG, 0, notification);
    } else {
      nm.notify(NOTIFICATION_TAG.hashCode(), notification);
    }
  }

  /**
   * Cancels any notifications of this type previously shown using
   */
  @TargetApi(Build.VERSION_CODES.ECLAIR)
  public static void cancel(final Context context) {
    final NotificationManager nm = (NotificationManager) context
            .getSystemService(Context.NOTIFICATION_SERVICE);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
      nm.cancel(NOTIFICATION_TAG, 0);
    } else {
      nm.cancel(NOTIFICATION_TAG.hashCode());
    }
  }
}
