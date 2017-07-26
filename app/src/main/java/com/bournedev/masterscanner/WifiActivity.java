package com.bournedev.masterscanner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class WifiActivity extends AppCompatActivity {

    private final String TAG = WifiActivity.class.getSimpleName();
    // Declare global variables
    private WifiManager mWifiManager;
    private WifiScanReceiver mWifiScanReceiver;
    private ListView mListView;

    private Timer myTimer;
    private ArrayAdapter<String> mArrayAdapter;
    private List<String> mArrayList = new ArrayList<>();
    private static DecimalFormat df = new DecimalFormat("#.###");

    private static final String REGISTER_URL = "http:/localhost/wifi-rssi-upload.php";
    private static final String KEY_SSID = "ssid";
    private static final String KEY_RSSI = "rssi";
    private static final String KEY_DISTANCE = "distance";

    private String setAP;
    private String name;
    private String rssi;
    private String distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        mListView = (ListView) findViewById(R.id.listView1);

        ensureWifi();

        // Register the Broadcast Receiver
        mWifiScanReceiver = new WifiScanReceiver();

        TextView ap = (TextView) findViewById(R.id.wifi_ap_choice);

        // Start scanning for all AP's or selected one
        Button startScan = (Button) findViewById(R.id.start_wifi);
        startScan.setOnClickListener(v -> {

            setAP = ap.getText().toString();
            Log.i(TAG, setAP);

            registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            setScheduler();
        });

        // Stop scanning
        Button stopScan = (Button) findViewById(R.id.stop_wifi);
        stopScan.setOnClickListener(v -> {

            myTimer.cancel();
            myTimer.purge();
            unregisterReceiver(mWifiScanReceiver);
        });
    }

    /**
     * Method to start scheduler that will run a wifi scan every two seconds
     */
    private void setScheduler() {

        // Timer added to get new scan result once every 2 seconds
        myTimer = new Timer();

        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethod();
            }
        }, 0, 2000);
    }

    /**
     * Timer method to run at the same time as the main activity
     */
    private void TimerMethod() {
        this.runOnUiThread(Timer_Tick);
    }

    /**
     * Runnable thread that allows for scan to be called
     * without crashing out the main thread
     */
    private final Runnable Timer_Tick = () -> {
        try {
            // start a scan of ap's
            mWifiManager.startScan();

            Log.i(TAG, "Starting scan");
        } catch (final Exception e) {
            e.getStackTrace();
        }
    };

    /**
     * Broadcast Receiver to capture the WiFi AP information
     */
    class WifiScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Log.i(TAG, "Clear details to refresh the screen for each new scan");
            // Clear details to refresh the screen for each new scan
            if (mArrayList.size() > 0) {
                try {
                    mArrayList.clear();
                    mArrayAdapter.clear();
                    mArrayAdapter.notifyDataSetChanged();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }

            // Store all listed AP's
            List<ScanResult> mResultList = mWifiManager.getScanResults();
            Log.d(TAG, "The number of AP's: " + mResultList.size());

            try {

                // Run through each signal and retrieve the SSID & RSSI
                for (final ScanResult accessPoint : mResultList) {

                    if (accessPoint.SSID.equalsIgnoreCase(setAP)) {

                        // If user has chosen a single AP to watch
                        name = setAP;
                        Log.i(TAG, "AP: " + name);

                        rssi = String.valueOf(accessPoint.level);
                        Log.i(TAG, "RSSI: " + rssi);

                        // Calculate the distance from the RSSI
                        distance = String.valueOf(calculateDistance(accessPoint.level));
                        Log.i(TAG, "Distance: " + distance);

                        String apDetails = accessPoint.SSID + "\n" +
                                String.valueOf(accessPoint.level) + "\n" +
                                String.valueOf(calculateDistance(accessPoint.level)) + "\n";

                        // Add to List that will be displayed to user
                        mArrayList.add(apDetails);

                    } else if (setAP == null || setAP.equalsIgnoreCase("")){

                        // Display all Wi-Fi AP's in the area
                        name = accessPoint.SSID;
                        Log.i(TAG, "AP: " + name);

                        rssi = String.valueOf(accessPoint.level);
                        Log.i(TAG, "RSSI: " + rssi);

                        distance = String.valueOf(calculateDistance(accessPoint.level));
                        Log.i(TAG, "Distance: " + distance);

                        String apDetails = "AP: " + accessPoint.SSID + "\n" +
                                "RSSI: " + String.valueOf(accessPoint.level) + "\n" +
                                "Distance: " + String.valueOf(calculateDistance(accessPoint.level)) + "\n";

                        // Add to List that will be displayed to user
                        mArrayList.add(apDetails);
                    }

                }
            } catch (Exception e) {

                Log.e(TAG, e.getMessage());
            }

            // Display details in ListView
            mArrayAdapter = new ArrayAdapter<>(getApplicationContext(),
                    android.R.layout.simple_list_item_1, mArrayList);
            mListView.setAdapter(mArrayAdapter);

            // Create a StringRequest and add ssid and rssi as the parameters
            StringRequest stringRequest = new StringRequest(Request.Method.POST, REGISTER_URL,
                    response -> Toast.makeText(WifiActivity.this, response, Toast.LENGTH_SHORT).show(),
                    error -> Toast.makeText(WifiActivity.this, error.toString(), Toast.LENGTH_LONG).show()) {
                @Override
                protected Map<String, String> getParams() {

                    Map<String, String> params = new HashMap<>();
                    params.put(KEY_SSID, name);
                    params.put(KEY_RSSI, rssi);
                    params.put(KEY_DISTANCE, distance);
                    return params;
                }

            };

            // Create the RequestQueue and add the new StringRequest
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
            requestQueue.add(stringRequest);
        }
    }

    @Override
    protected void onPause() {

        super.onPause();

        myTimer.cancel();
        myTimer.purge();
        // Unregister the Receiver
        unregisterReceiver(mWifiScanReceiver);
    }

    @Override
    protected void onResume() {

        super.onResume();

        // re-register the receiver
        registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        myTimer.cancel();
        myTimer.purge();
    }

    /**
     * Method to ensure that the wifi is enabled
     */
    private void ensureWifi() {

        // Instantiate the WiFi Manager
        mWifiManager = (WifiManager) getApplicationContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Check that the WiFi is enabled
        if (!mWifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            mWifiManager.setWifiEnabled(true);
        }
    }

    /**
     * Method to calculate rssi into distance in meters
     *
     * @param value rssi read
     * @return calculated distance
     */
    private double calculateDistance(int value) {

        double distance;
        // The one meter read and the path-loss exponent
        double _1MeterRead = -37.91;
        double pathLossExponent = 2;

        distance = Math.pow(10, ((value - _1MeterRead) / (-10 * pathLossExponent)));

        return Double.parseDouble(df.format(distance));
    }
}
