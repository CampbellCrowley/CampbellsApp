package com.campbellcrowley.dev.campbellsapp;

import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by campbell on 12/4/2017.
 */

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {
  public FirebaseMessagingService() {
    super();
  }

  @Override
  public void onMessageReceived(RemoteMessage message) {

    Log.i("FirebaseMessage", "Received message from Firebase: " + message.getData().toString());
    if (message.getData().size() == 0) return;
    String messageText = message.getData().get("message");
    Log.i("FirebaseMessage", "Message: " + messageText);
    if (messageText == null || messageText.length() == 0) return;

    String moreText = "";
    if (messageText.equals(SettingsActivity.KEY_NOTIF_POWER_PRESSED) || messageText.equals(SettingsActivity.KEY_NOTIF_POWER_HELD) || messageText.equals(SettingsActivity.KEY_NOTIF_RESET_PRESSED) || messageText.equals(SettingsActivity.KEY_NOTIF_WAKE_EVENT)) {
      moreText = "Received response from the server confirming this event was acted upon.";
      if (messageText.equals(SettingsActivity.KEY_NOTIF_POWER_PRESSED)) {
        messageText = "Power Pressed";
      } else if (messageText.equals(SettingsActivity.KEY_NOTIF_POWER_HELD)) {
        messageText = "Power Held";
      } else if (messageText.equals(SettingsActivity.KEY_NOTIF_RESET_PRESSED)) {
        messageText = "Reset Held";
      } else if (messageText.equals(SettingsActivity.KEY_NOTIF_WAKE_EVENT)) {
        messageText = "Computer Awakened";
      }
    } else if (messageText.equals(SettingsActivity.KEY_NOTIF_POWER_ON)) {
      moreText = messageText;
      messageText = "The computer was turned on!";
    } else if (messageText.equals(SettingsActivity.KEY_NOTIF_POWER_OFF)) {
      moreText = messageText;
      messageText = "The computer was turned off!";
    }
    Log.i("FirebaseMessage", "Notification: " + messageText + " AND " + moreText);
    TextNotificationManager.notify(this, messageText, moreText, 0, messageText);
  }
}
