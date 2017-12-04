package com.campbellcrowley.dev.campbellsapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import static com.campbellcrowley.dev.campbellsapp.MainActivity.TokenState.INVALID;
import static com.campbellcrowley.dev.campbellsapp.MainActivity.TokenState.UNVALIDATED;
import static com.campbellcrowley.dev.campbellsapp.MainActivity.TokenState.VALID;

interface AsyncResponse {
  void processFinish(List<String> output);
}

public class MainActivity extends AppCompatActivity implements AsyncResponse, PCStatusFragment.OnFragmentInteractionListener {

  private static final int RC_SIGN_IN = 100;
  private static String idToken = "";
  private static TokenState tokenState = UNVALIDATED;
  private static int userLevel = 0;
  private static Context context;
  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  private SectionsPagerAdapter mSectionsPagerAdapter;
  /**
   * The {@link ViewPager} that will host the section contents.
   */
  private ViewPager mViewPager;
  private GoogleSignInClient mGoogleSignInClient;
  private AsyncResponse asyncTaskDelegate = null;

  public static TokenState getTokenState() {
    return tokenState;
  }

  public static String getToken() {
    return idToken;
  }

  public static int getUserLevel() {
    return userLevel;
  }

  public static GoogleSignInAccount getAccount() {
    return GoogleSignIn.getLastSignedInAccount(getAppContext());
  }

  public static Context getAppContext() {
    return MainActivity.context;
  }

  @Override
  public void onFragmentInteraction(Uri uri) {
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    MainActivity.context = getApplicationContext();

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = findViewById(R.id.container);
    mViewPager.setAdapter(mSectionsPagerAdapter);

    TabLayout tabLayout = findViewById(R.id.tabs);

    mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
    tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

    asyncTaskDelegate = this;

    GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.server_client_id)).build();
    mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    PCStatusFragment.ToggleButtonEnabled(userLevel >= 5);
  }

  @Override
  protected void onResume() {
    super.onResume();
    signInSilently();
  }

  private void signInSilently() {
    mGoogleSignInClient.silentSignIn().addOnCompleteListener(this,
            new OnCompleteListener<GoogleSignInAccount>() {
              @Override
              public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                if (task.isSuccessful()) {
                  idToken = task.getResult().getIdToken();
                  tokenState = UNVALIDATED;
                  userLevel = 0;
                  validateToken();
                } else {
                }
              }
            });
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == R.id.action_signinout) {
      if (getAccount() == null) {
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_SIGN_IN);
      } else {
        mGoogleSignInClient.signOut().addOnCompleteListener(this, new OnCompleteListener<Void>() {
          @Override
          public void onComplete(@NonNull Task<Void> task) {
            showMessage("Signed out");
            tokenState = UNVALIDATED;
            userLevel = 0;
            PCStatusFragment.ToggleButtonEnabled(userLevel >= 5);
          }
        });
      }
      return true;
    }

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == RC_SIGN_IN) {
      Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
      handleSignInResult(task);
    }
  }

  private void validateToken() {
    Log.i("MainActivitySignIn", "Validating Token.");
    ValidateTokenTask task = new ValidateTokenTask();
    task.delegate = asyncTaskDelegate;
    try {
      showMessage("Validating...");
      task.execute(new URL("https://dev.campbellcrowley.com/tokensignin"));
    } catch (MalformedURLException e) {
      showMessage("Failed to authenticate! Campbell broke something...");
      e.printStackTrace();
    }
  }

  private void handleSignInResult(@NonNull Task<GoogleSignInAccount> completedTask) {
    try {
      GoogleSignInAccount account = completedTask.getResult(ApiException.class);
      idToken = account.getIdToken();
      Log.i("MainActivitySignIn", "signInResult:succeeded");

      validateToken();
    } catch (ApiException e) {
      e.printStackTrace();
      Log.e("MainActivitySignIn", "signInResult:failed " + e.toString());
      showMessage("Failed to sign in!");
      if (e.getStatusCode() == 10)
        showMessage("Is this an unsigned version of the app?");
    }
  }

  @Override
  public void processFinish(List<String> output) {
    for (int i = 0; i < output.size(); i++) {
      if (output.get(i).indexOf(") Authenticated!") > 0 && output.get(i).indexOf(getAccount().getDisplayName()) == 0) {
        tokenState = VALID;
        userLevel = Integer.parseInt(output.get(i).substring(output.get(i).indexOf("(") + 1, output.get(i).indexOf(")")));
        showMessage("Authenticated " + getAccount().getGivenName() + " (" + userLevel + ")");
        PCStatusFragment.ToggleButtonEnabled(userLevel >= 5);
      } else if (output.get(i).indexOf("Invalid token") == 0) {
        tokenState = INVALID;
        userLevel = 0;
        showMessage("Failed to authenticate " + getAccount().getGivenName());
        PCStatusFragment.ToggleButtonEnabled(userLevel >= 5);
      }
    }
    if (output.size() == 0) {
      showMessage("I'm sorry, but the server doesn't seem to like you.");
    }
  }

  public void showMessage(String message) {
    Snackbar.make(findViewById(R.id.main_content), message, Snackbar.LENGTH_LONG).show();
  }

  enum TokenState {
    UNVALIDATED,
    VALID,
    INVALID
  }

  /**
   * A placeholder fragment containing a simple view.
   */
  public static class PlaceholderFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    public PlaceholderFragment() {
    }

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static PlaceholderFragment newInstance(int sectionNumber) {
      PlaceholderFragment fragment = new PlaceholderFragment();
      Bundle args = new Bundle();
      args.putInt(ARG_SECTION_NUMBER, sectionNumber);
      fragment.setArguments(args);
      return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
      View rootView = inflater.inflate(R.layout.fragment_main, container, false);
      TextView textView = rootView.findViewById(R.id.section_label);
      textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
      return rootView;
    }
  }

  private class ValidateTokenTask extends AsyncTask<URL, Void, List<String>> {
    private AsyncResponse delegate = null;

    protected List<String> doInBackground(URL... urls) {
      List responses = new ArrayList<String>();
      for (int i = 0; i < urls.length; i++) {
        responses.add(sendHttpRequest(urls[i], "idtoken=" + getToken(), "POST"));
      }
      return responses;
    }

    private String sendHttpRequest(URL url, String data, String method) {
      try {
        String postData = data;
        Log.i("MainActivitySignIn", "Sending Http request as: " + getAccount().getId());
        byte[] postDataBytes = postData.getBytes("UTF-8");

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", Integer.toString(postDataBytes.length));
        connection.getOutputStream().write(postDataBytes);

        Reader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));

        StringBuilder sb = new StringBuilder();
        for (int c; (c = in.read()) >= 0; )
          sb.append((char) c);
        return sb.toString();
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      } catch (ProtocolException e) {
        e.printStackTrace();
      } catch (MalformedURLException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
      return "Somebody broke something, and it probably wasn't you... probably.";
    }

    @Override
    protected void onPostExecute(List<String> responses) {
      Log.i("MainActivitySignIn", "Downloaded " + responses.size() + " responses.");
      if (delegate != null)
        delegate.processFinish(responses);
    }
  }

  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      // getItem is called to instantiate the fragment for the given page.
      // Return a PlaceholderFragment (defined as a static inner class below).
      if (position == 3) {
        return PCStatusFragment.newInstance(position + 1);
      } else {
        return PlaceholderFragment.newInstance(position + 1);
      }
    }

    @Override
    public int getCount() {
      // Show 4 total pages.
      return 4;
    }
  }
}

