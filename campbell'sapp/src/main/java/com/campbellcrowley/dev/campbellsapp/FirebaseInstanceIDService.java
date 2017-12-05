package com.campbellcrowley.dev.campbellsapp;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by campbell on 12/4/2017.
 */

public class FirebaseInstanceIDService extends FirebaseInstanceIdService {
  public static String currentToken = "";

  @Override
  public void onTokenRefresh() {
    String refreshedToken = FirebaseInstanceId.getInstance().getToken();
    Log.d("FirebaseIDService", "Refreshed token: " + refreshedToken);
    if (refreshedToken != null) {
      currentToken = refreshedToken;
    }
  }
}
