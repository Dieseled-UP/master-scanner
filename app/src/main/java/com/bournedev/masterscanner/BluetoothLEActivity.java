package com.bournedev.masterscanner;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

/*import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;*/

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
/*import java.util.HashMap;
import java.util.Map;*/

public class BluetoothLEActivity extends AppCompatActivity {

    private final String TAG = BluetoothLEActivity.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private AdvertiseCallback mAdvertiseCallback;
    private static final long SCAN_PERIOD = 10000;
    private static final int REQUEST_ENABLE_BT = 1;
    private boolean mIsScanning;

    private Button mBtnScan;
    private Button mBtnStop;
    private ListView mListView;

    private ArrayAdapter<String> mArrayAdapter;
    private List<String> mList = new ArrayList<>();
    private Handler mHandler;
    private static DecimalFormat mDecimalFormat = new DecimalFormat("#.###");

    /*private static final String REGISTER_URL = "http://localhost/blue-rssi-upload.php";
    private static final String KEY_SSID = "ssid";
    private static final String KEY_RSSI = "rssi";
    private static final String KEY_DISTANCE = "distance";*/

    private String setAP;
    private String name;
    private String rssi;
    private String distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        mHandler = new Handler();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {

            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        mBtnScan = findViewById(R.id.start_blue);
        mBtnStop = findViewById(R.id.stop_blue);
        mBtnStop.setEnabled(false);
        mBtnStop.setBackground(getResources().getDrawable(R.drawable.btn_round_disabled, this.getTheme()));

        if (!BluetoothAdapter.getDefaultAdapter().isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "Multiple advertisement not supported", Toast.LENGTH_SHORT).show();
            mBtnScan.setEnabled(false);
            mBtnScan.setBackground(getResources().getDrawable(R.drawable.btn_round_disabled, this.getTheme()));
            mBtnStop.setEnabled(false);
            mBtnStop.setBackground(getResources().getDrawable(R.drawable.btn_round_disabled, this.getTheme()));
        }

        getBluetoothAdapterAndLeScanner();

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth detected", Toast.LENGTH_SHORT).show();
            finish();
        }

        Switch switchTranScan = findViewById(R.id.trans_receive);
        switchTranScan.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                mBtnScan.setText(getResources().getString(R.string.start_trans));
                mBtnStop.setText(getResources().getString(R.string.stop_trans));
            } else {
                mBtnScan.setText(getResources().getString(R.string.start_scan));
                mBtnStop.setText(getResources().getString(R.string.stop_scan));
            }
        });

        mBtnScan.setOnClickListener(v -> {

            Log.i(TAG, mBtnScan.getText().toString());

            if (mBtnScan.getText().toString().equalsIgnoreCase("start scan")) {

                startScanning(true);

            } else if (mBtnScan.getText().toString().equalsIgnoreCase("start transmit")) {

                transmit();
            }
        });

        mBtnStop.setOnClickListener(v -> {

            if (mBtnStop.getText().toString().equalsIgnoreCase("Stop Scan")) {

                startScanning(false);

            } else if (mBtnStop.getText().toString().equalsIgnoreCase("Stop Transmit")) {

                stopTransmit();
            }
        });

        mListView = findViewById(R.id.listView1);
    }

    private void getBluetoothAdapterAndLeScanner() {

        // Get BluetoothAdapter and BluetoothLeScanner.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    @Override
    protected void onResume() {

        super.onResume();

        // Check low energy support
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {

            if (!mBluetoothAdapter.isEnabled()) {

                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
        startScanning(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {

            finish();
        }

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {

            Toast.makeText(this, "bluetoothManager.getAdapter()==null", Toast.LENGTH_SHORT).show();
            finish();
        }

        getBluetoothAdapterAndLeScanner();
    }

    private void startScanning(final boolean enable) {

        if (enable) {

            mHandler.postDelayed(() -> {

                enableScanBtn();
                mIsScanning = false;
                mBluetoothLeScanner.stopScan(scanCallback);

            }, SCAN_PERIOD);

            disableScanBtn();

            mIsScanning = true;
            mBluetoothLeScanner.startScan(scanCallback);

        } else {

            enableScanBtn();
            mIsScanning = false;
            mBluetoothLeScanner.stopScan(scanCallback);
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            super.onScanResult(callbackType, result);

            Log.i(TAG, "Clear details to refresh the screen for each new scan");
            // Clear details to refresh the screen for each new scan
            if (mList.size() > 0) {
                try {
                    mList.clear();
                    mArrayAdapter.clear();
                    mArrayAdapter.notifyDataSetChanged();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }

            if (result == null || result.getDevice() == null) {
                Log.e(TAG, "-> No devices found");
                return;
            }

            try {

                rssi = String.valueOf(result.getRssi());
                name = result.getDevice().getName();
                distance = String.valueOf(calculateDistance(result.getRssi()));

                String apDetails = "AP: " + name + "\n" +
                        "RSSI: " + rssi + "\n" +
                        "Distance: " + distance + "\n";

                Log.i(TAG, apDetails);

                mList.add(apDetails);

            } catch (NullPointerException e) {

                Log.e(TAG, e.getMessage());
            }

            // Display details in ListView
            mArrayAdapter = new ArrayAdapter<>(getApplicationContext(),
                    android.R.layout.simple_list_item_1, mList);
            mListView.setAdapter(mArrayAdapter);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {

            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {

            super.onScanFailed(errorCode);
            Toast.makeText(BluetoothLEActivity.this, "onScanFailed: " + String.valueOf(errorCode), Toast.LENGTH_LONG).show();
        }
/*
        // Create a StringRequest and add ssid and rssi as the parameters
        StringRequest stringRequest = new StringRequest(Request.Method.POST, REGISTER_URL,
                response -> Toast.makeText(BluetoothLEActivity.this, response, Toast.LENGTH_SHORT).show(),
                error -> Toast.makeText(BluetoothLEActivity.this, error.toString(), Toast.LENGTH_LONG).show()) {
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
            requestQueue.add(stringRequest);*/
    };

    private void transmit() {

        disableScanBtn();

        mBluetoothLeAdvertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        mAdvertiseCallback = new AdvertiseCallback() {

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
            }

            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "Advertising onStartFailure: " + errorCode);
                super.onStartFailure(errorCode);
            }
        };

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private void stopTransmit() {

        enableScanBtn();

        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
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

        return Double.parseDouble(mDecimalFormat.format(distance));
    }

    private void disableScanBtn() {

        mBtnScan.setEnabled(false);
        mBtnScan.setBackground(getResources().getDrawable(R.drawable.btn_round_disabled, this.getTheme()));

        mBtnStop.setEnabled(true);
        mBtnStop.setBackground(getResources().getDrawable(R.drawable.btn_round_enabled, this.getTheme()));
    }

    private void enableScanBtn() {

        mBtnScan.setEnabled(true);
        mBtnScan.setBackground(getResources().getDrawable(R.drawable.btn_round_enabled, this.getTheme()));

        mBtnStop.setEnabled(false);
        mBtnStop.setBackground(getResources().getDrawable(R.drawable.btn_round_disabled, this.getTheme()));
    }
}
