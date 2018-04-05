package com.bournedev.masterscanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BluetoothStandardActivity extends AppCompatActivity {

    private String TAG = BluetoothStandardActivity.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothScanReceiver mBluetoothScanReceiver;

    private Button mBtnScan;
    private Button mBtnStop;
    private ListView mListView;
    private IntentFilter mIntentFilter;

    private ArrayAdapter<String> mArrayAdapter;
    private List<String> mList = new ArrayList<>();
    private static DecimalFormat mDecimalFormat = new DecimalFormat("#.###");

    private final int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 123;

    private static final String REGISTER_URL = "https://host/blue-rssi-upload.php";
    private static final String KEY_SSID = "ssid";
    private static final String KEY_RSSI = "rssi";
    private static final String KEY_DISTANCE = "distance";

    private DatabaseReference mDatabase;

    private String name;
    private String rssi;
    private String distance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Get permissions to access the location
        requestPermission();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        mListView = findViewById(R.id.listView1);

        // Initializes Bluetooth adapter.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // If bluetooth is not switched on
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            // turn on BT
            turnOnBT();
        }

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth detected", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Register Receiver and filters
        mBluetoothScanReceiver = new BluetoothScanReceiver();
        mIntentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mBluetoothScanReceiver, mIntentFilter);
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mBluetoothScanReceiver, mIntentFilter);

        mBtnScan = findViewById(R.id.start_blue);
        mBtnStop = findViewById(R.id.stop_blue);
        mBtnStop.setEnabled(false);
        mBtnStop.setBackground(getResources().getDrawable(R.drawable.btn_round_disabled, this.getTheme()));

        mBtnScan.setOnClickListener(v -> {

            disableScanBtn();
            startScan();
        });

        mBtnStop.setOnClickListener(v -> {

            enableScanBtn();
            stopScan();
        });
    }

    class BluetoothScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {

                // Let user know that a scan is in the process
                Log.i(TAG, " Scan Started");

                // Get the details of the device that is been scanned
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.i(TAG, "-> Device found: " + device.getName());

                // Check that we are getting the device we want
                /*if (getBluetoothDevice(device.getName())) {*/

                    // Let user know the device has been added
                    Toast.makeText(getApplicationContext(), "Device Added " + " : " + device.getName(), Toast.LENGTH_SHORT).show();

                    // Force name for null values stop app crashing
                    if (device.getName() == null) {
                        name = "Bluetooth device";
                    } else {
                        name = device.getName();
                    }
                    rssi = Short.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE));
                    distance = String.valueOf(calculateDistance(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)));

                    String apDetails = name + "\n" + rssi + "\n" + distance + "\n";

                    // Add to List that will be displayed to user
                    mList.add(apDetails);
                /*}*/

                // Display details in ListView
                mArrayAdapter = new ArrayAdapter<>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, mList);
                mListView.setAdapter(mArrayAdapter);

                writeNewRead(name, rssi);


/*
                // Create a StringRequest and add ssid and rssi as the parameters
                StringRequest stringRequest = new StringRequest(Request.Method.POST, REGISTER_URL,
                        response -> Toast.makeText(BluetoothStandardActivity.this, response, Toast.LENGTH_SHORT).show(),
                        error -> Toast.makeText(BluetoothStandardActivity.this, error.toString(), Toast.LENGTH_LONG).show()) {
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

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

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

                // Let user know that the scan has completed
                Log.i(TAG, "Scan Finished");
                // Call onPause to release the receiver and stop discovery
                onPause();

                // Start the process again with the start button which we
                // click programmatically
                mBtnScan.performClick();
            }
        }
    }

    private void writeNewRead(String trackedDevice, String rssi) {

        UwbBleHybrid device = new UwbBleHybrid(trackedDevice, rssi);

        mDatabase.child("network-1").child("tag-2").setValue(device);
    }

    /**
     * Method to allow user to access a single access point
     *
     * @param device Bluetooth Device name
     * @return boolean
     */
    public boolean getBluetoothDevice(String device) {

        if (device == null) {
            device = "Bluetooth device";
        }
        return device.equalsIgnoreCase("Bluetooth device");

    }

    /**
     * Method to refresh the filters and Receiver this hack is needed as
     * we will continuously loop the Discovery() method which Android
     * does not like when it comes to Bluetooth
     */
    private void startScan() {

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mBluetoothScanReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mBluetoothScanReceiver, filter);

        mBluetoothAdapter.startDiscovery();
    }

    private void stopScan() {

        onPause();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast receiver
        try {
            unregisterReceiver(mBluetoothScanReceiver);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "-> " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
        registerReceiver(mBluetoothScanReceiver, mIntentFilter);
    }

    /**
     * Method to open Bluetooth setting to allow user to
     * switch on Bluetooth
     */
    protected void turnOnBT() {

        int REQUEST_ENABLE_BT = 1;
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    /**
     * Get user to give access to location for app
     */
    public void requestPermission() {

        List<String> permissionsNeeded = new ArrayList<>();

        final List<String> permissionsList = new ArrayList<>();
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_FINE_LOCATION))
            permissionsNeeded.add("GPS");

        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                StringBuilder message = new StringBuilder("You need to grant access to " + permissionsNeeded.get(0));
                for (int i = 1; i < permissionsNeeded.size(); i++)
                    message.append(", ").append(permissionsNeeded.get(i));
                showMessageOKCancel(message.toString(),
                        (dialog, which) -> requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                                REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS));
                return;
            }
            requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        }
    }

    private boolean addPermission(List<String> permissionsList, String permission) {

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            // Check for Rationale Option
            return shouldShowRequestPermissionRationale(permission);
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {

            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS: {
                Map<String, Integer> perms = new HashMap<>();
                // Initial
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                // Fill with results
                for (int i = 0; i < permissions.length; i++)
                    perms.put(permissions[i], grantResults[i]);
                // Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted

                    Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
                } else {
                    // Permission Denied
                    Toast.makeText(BluetoothStandardActivity.this, "Some Permission is Denied", Toast.LENGTH_SHORT)
                            .show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(BluetoothStandardActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    /**
     * Method to calculate rssi into distance in meters
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
