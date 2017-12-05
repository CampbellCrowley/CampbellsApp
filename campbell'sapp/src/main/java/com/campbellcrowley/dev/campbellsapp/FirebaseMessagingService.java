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
    if (message.getData().size() > 0) {
      Log.i("FirebaseMessage", "Message: " + message.getData().get("message"));
      if (message.getData().get("message") != null) {
        String moreText = "";
        String messageText = message.getData().get("message");
        if (messageText.equals(SettingsActivity.KEY_NOTIF_POWER_PRESSED) || messageText.equals(SettingsActivity.KEY_NOTIF_POWER_HELD) || messageText.equals(SettingsActivity.KEY_NOTIF_RESET_PRESSED) || messageText.equals(SettingsActivity.KEY_NOTIF_WAKE_EVENT)) {
          moreText = "Received response from the server confirming this event was acted upon.";
        } else if (messageText.equals(SettingsActivity.KEY_NOTIF_POWER_ON)) {
          moreText = "The computer was turned on!";
        } else if (messageText.equals(SettingsActivity.KEY_NOTIF_POWER_OFF)) {
          moreText = "The computer was turned off!";
        }
        TextNotificationManager.notify(this, messageText, moreText, 0);
      }
    }
  }
}
