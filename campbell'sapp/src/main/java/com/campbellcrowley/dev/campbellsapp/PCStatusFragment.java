package com.campbellcrowley.dev.campbellsapp;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

import static com.campbellcrowley.dev.campbellsapp.MainActivity.getToken;
import static com.campbellcrowley.dev.campbellsapp.MainActivity.getUserLevel;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PCStatusFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PCStatusFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PCStatusFragment extends Fragment implements AsyncResponse {

  private static final String ARG_SECTION_NUMBER = "section_number";
  public static BarGraphSeries<DataPoint> mSeries;
  private static View rootView;
  private static boolean currentButtonState = true;
  private static boolean stateNow = false;
  private static AsyncResponse asyncTaskDelegate = null;
  private static Timer Interval;

  private OnFragmentInteractionListener mListener;

  public PCStatusFragment() {
    CookieManager cookieManager = new CookieManager();
    CookieHandler.setDefault(cookieManager);
    asyncTaskDelegate = this;
  }

  public static PCStatusFragment newInstance(int sectionNumber) {
    PCStatusFragment fragment = new PCStatusFragment();
    Bundle args = new Bundle();
    args.putInt(ARG_SECTION_NUMBER, sectionNumber);
    fragment.setArguments(args);

    return fragment;
  }

  private static void getInfo() {
    sendRequest task = new sendRequest();
    task.delegate = asyncTaskDelegate;
    try {
      task.execute(new URL("https://dev.campbellcrowley.com/secure/pc/get-info"));
    } catch (MalformedURLException e) {
      clearRows();
      addRow("Failed to get latest info! Campbell broke something...");
      e.printStackTrace();
    }
  }

  public static void ToggleButtonEnabled(boolean newState) {
    currentButtonState = newState;
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
    if (rootView == null) return;
    LinearLayout scrollView = rootView.findViewById(R.id.moreinfo);
    if (scrollView == null) return;
    TextView newRow = new TextView(MainActivity.getAppContext());
    newRow.setText(text);
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
  public void onResume() {
    super.onResume();
    (Interval = new Timer()).scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        getInfo();
      }
    }, 0, 8000);
  }

  @Override
  public void onPause() {
    super.onPause();
    Interval.cancel();
  }

  @Override
  public void processFinish(List<String> output) {
    if (output.size() > 0) {
      if (output.get(0).contains("STATE--:")) {
        try {
          String[] daysString = output.get(0).split("\n")[1].split(",");
          double[] daysNumber = new double[7];
          for (int i = 0; i < daysNumber.length; i++) {
            daysNumber[i] = Double.parseDouble(daysString[i]);
          }
          UpdateGraphData(daysNumber);
          stateNow = output.get(0).split("STATE--: ")[1].charAt(1) == 'n';
          if (rootView != null) {
            TextView textView = rootView.findViewById(R.id.content);
            textView.setText(getString(R.string.pcstatus_format, stateNow ? "On" : "Off"));
          }
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String[] moreInfo = output.get(0).split("Server Start")[0].split("\n");
            String infoString = "Previous Event: " + output.get(0).split("Previous Event: ")[1].split("\n")[0] + "\n"
                    + String.join("\n", Arrays.copyOfRange(moreInfo, 3, moreInfo.length));
            String[] infoList = infoString.split("\n");
            clearRows();
            for (int i = 0; i < infoList.length; i++) {
              addRow(infoList[i]);
            }
          }
        } catch (ArrayIndexOutOfBoundsException e) {
          clearRows();
          addRow(output.get(0));
          e.printStackTrace();
        }
      } else {
        showMessage(output.get(0));
        ToggleButtonEnabled(getUserLevel() >= 5);
      }
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    rootView = inflater.inflate(R.layout.fragment_pcstatus, container, false);

    TextView textView = rootView.findViewById(R.id.content);
    textView.setText(getString(R.string.pcstatus_format, stateNow ? "On" : "Off"));

    GraphView graph = rootView.findViewById(R.id.graph);
    mSeries = new BarGraphSeries<>(new DataPoint[]{
            new DataPoint(0.5, 0),
            new DataPoint(1.5, 0),
            new DataPoint(2.5, 0),
            new DataPoint(3.5, 0),
            new DataPoint(4.5, 0),
            new DataPoint(5.5, 0),
            new DataPoint(6.5, 0)
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
    mSeries.setDrawValuesOnTop(true);
    graph.addSeries(mSeries);

    PCStatusFragment.ToggleButtonEnabled(currentButtonState);

    LinearLayout buttonParent = rootView.findViewById(R.id.powerbuttons_parent);
    for (int i = 0; i < buttonParent.getChildCount(); i++) {
      String request = "unknown";
      switch (i) {
        case 0:
          request = "press-power";
          break;
        case 1:
          request = "hold-power";
          break;
        case 2:
          request = "press-reset";
          break;
        case 3:
          request = "wake";
          break;
      }

      final String finalRequest = request;
      buttonParent.getChildAt(i).setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
          sendRequest task = new sendRequest();
          task.delegate = asyncTaskDelegate;
          try {
            task.execute(new URL("https://dev.campbellcrowley.com/secure/pc/" + finalRequest));
            ToggleButtonEnabled(false);
          } catch (MalformedURLException e) {
            clearRows();
            addRow("Failed to get latest info! Campbell broke something...");
            e.printStackTrace();
          }
        }
      });
    }

    return rootView;
  }

  public void showMessage(String message) {
    Snackbar.make(getActivity().findViewById(R.id.main_content), message, Snackbar.LENGTH_LONG).show();
  }

  // TODO: Rename method, update argument and hook method into UI event
  public void onButtonPressed(Uri uri) {
    if (mListener != null) {
      mListener.onFragmentInteraction(uri);
    }
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof OnFragmentInteractionListener) {
      mListener = (OnFragmentInteractionListener) context;
    } else {
      throw new RuntimeException(context.toString()
              + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }

  /**
   * This interface must be implemented by activities that contain this
   * fragment to allow an interaction in this fragment to be communicated
   * to the activity and potentially other fragments contained in that
   * activity.
   * <p>
   * See the Android Training lesson <a href=
   * "http://developer.android.com/training/basics/fragments/communicating.html"
   * >Communicating with Other Fragments</a> for more information.
   */
  public interface OnFragmentInteractionListener {
    // TODO: Update argument type and name
    void onFragmentInteraction(Uri uri);
  }

  private static class sendRequest extends AsyncTask<URL, Void, List<String>> {
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
        Log.i("PCStatusFragment", "Sending http request.");
        byte[] postDataBytes = postData.getBytes("UTF-8");

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", Integer.toString(postDataBytes.length));
        connection.setRequestProperty("Cookie", "token=" + getToken());
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
      Log.i("PCStatusFragment", "Downloaded " + responses.size() + " responses.");
      delegate.processFinish(responses);
    }
  }
}
