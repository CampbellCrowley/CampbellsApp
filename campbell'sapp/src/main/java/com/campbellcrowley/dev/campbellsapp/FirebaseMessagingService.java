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
  }
}
