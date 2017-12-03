package com.campbellcrowley.dev.campbellsapp;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

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

public class MainActivity extends AppCompatActivity implements AsyncResponse {

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
            PlaceholderFragment.addRow("Signed out");
            tokenState = UNVALIDATED;
            PlaceholderFragment.ToggleButtonEnabled(tokenState == VALID);
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
      PlaceholderFragment.addRow("Validating...");
      task.execute(new URL("https://dev.campbellcrowley.com/tokensignin"));
    } catch (MalformedURLException e) {
      PlaceholderFragment.addRow("Failed to authenticate! Campbell broke something...");
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
      PlaceholderFragment.addRow("Failed to sign in!");
      if (e.getStatusCode() == 10)
        PlaceholderFragment.addRow("Is this an unsigned version of the app?");
    }
  }

  @Override
  public void processFinish(List<String> output) {
    PlaceholderFragment.clearRows();
    for (int i = 0; i < output.size(); i++) {
      if (output.get(i).indexOf(") Authenticated!") > 0 && output.get(i).indexOf(getAccount().getDisplayName()) == 0) {
        tokenState = VALID;
        userLevel = Integer.parseInt(output.get(i).substring(output.get(i).indexOf("(") + 1, output.get(i).indexOf(")")));
        PlaceholderFragment.addRow("Authenticated " + getAccount().getGivenName() + " (" + userLevel + ")");
      } else if (output.get(i).indexOf("Invalid token") == 0) {
        tokenState = INVALID;
        PlaceholderFragment.addRow("Failed to authenticate " + getAccount().getGivenName());
      }
    }
    if (output.size() == 0) {
      PlaceholderFragment.addRow("I'm sorry, but the server doesn't like you.");
    }
    PlaceholderFragment.ToggleButtonEnabled(tokenState == VALID);
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

    public static BarGraphSeries<DataPoint> mSeries;
    public static View rootView;

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

    public static void ToggleButtonEnabled(boolean newState) {
      if (rootView == null) return;
      LinearLayout buttonParent = rootView.findViewById(R.id.powerbuttons_parent);
      if (buttonParent == null) return;
      for (int i = 0; i < buttonParent.getChildCount(); i++) {
        buttonParent.getChildAt(i).setEnabled(newState);
      }
    }

    public static void clearRows() {
      if (rootView == null) return;
      LinearLayout scrollView = rootView.findViewById(R.id.moreinfo);
      if (scrollView == null) return;
      if (scrollView.getChildCount() > 0) scrollView.removeAllViews();
    }

    public static void addRow(String text) {
      TextView newRow = new TextView(MainActivity.getAppContext());
      newRow.setText(text);
      if (rootView == null) return;
      LinearLayout scrollView = rootView.findViewById(R.id.moreinfo);
      if (scrollView == null) return;
      scrollView.addView(newRow);
    }

    private static void UpdateGraphData(double[] newData) {
      DataPoint[] parsedData = new DataPoint[newData.length];
      for (int i = 0; i < parsedData.length; i++) {
        parsedData[i] = new DataPoint(i + 0.5, newData[i]);
      }
      mSeries.resetData(parsedData);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
      if (getArguments().getInt(ARG_SECTION_NUMBER) == 2) {
        View rootView = inflater.inflate(R.layout.fragment_timermenu, container, false);
        TextView textView = rootView.findViewById(R.id.content);
        textView.setText(getString(R.string.timer_format, getArguments().getInt(ARG_SECTION_NUMBER)));
        return rootView;
      } else if (getArguments().getInt(ARG_SECTION_NUMBER) == 4) {
        rootView = inflater.inflate(R.layout.fragment_pcstatus, container, false);

        TextView textView = rootView.findViewById(R.id.content);
        textView.setText(getString(R.string.pcstatus_format, getArguments().getInt(ARG_SECTION_NUMBER)));

        GraphView graph = rootView.findViewById(R.id.graph);
        mSeries = new BarGraphSeries<>(new DataPoint[]{
                new DataPoint(0.5, 100),
                new DataPoint(1.5, 10),
                new DataPoint(2.5, 20),
                new DataPoint(3.5, 30),
                new DataPoint(4.5, 40),
                new DataPoint(5.5, 50),
                new DataPoint(6.5, 60)
        });
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(100);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(7);
        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
          @Override
          public String formatLabel(double value, boolean isValueX) {
            if (isValueX) {
              String[] DOWTitles = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
              int newValue = (int) Math.floor(value);
              if (newValue >= 0 && newValue < DOWTitles.length) {
                return DOWTitles[newValue];
              } else {
                return "Unk";
              }
            } else {
              return super.formatLabel(value, isValueX) + "%";
            }
          }
        });
        mSeries.setAnimated(true);
        mSeries.setSpacing(3);
        graph.addSeries(mSeries);

        boolean isSignedIn = tokenState == VALID;
        if (isSignedIn)
          addRow(getAccount().getDisplayName() + " (" + userLevel + ") is signed in!");
        else if (getAccount() != null) addRow("Authenticating " + getAccount().getDisplayName());

        ToggleButtonEnabled(isSignedIn);

        return rootView;
      } else {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        TextView textView = rootView.findViewById(R.id.section_label);
        textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
        return rootView;
      }
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
        Log.i("MainActivitySignIn", "Sending: " + postData);
        Log.i("MainActivitySignIn", "Name: " + getAccount().getDisplayName());
        Log.i("MainActivitySignIn", "ID: " + getAccount().getId());
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
      return PlaceholderFragment.newInstance(position + 1);
    }

    @Override
    public int getCount() {
      // Show 4 total pages.
      return 4;
    }
  }
}

