package com.bournedev.masterscanner;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class BluetoothActivity extends AppCompatActivity {

    private final String TAG = MainActivity.class.getSimpleName();

    private BluetoothAdapter btAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private static final int REQUEST_ENABLE_BT = 1;
    private Button btnScan;
    private ListView listViewLE;
    private List<BluetoothDevice> listBluetoothDevice;
    private Handler handler;
    private boolean scanning = true;
    private static final long SCAN_PERIOD = 10000;
    private static DecimalFormat df = new DecimalFormat("#.###");

    private static final String REGISTER_URL = "http://localhost/blue-rssi-upload.php";
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
        setContentView(R.layout.activity_bluetooth);
        handler = new Handler();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        getBluetoothAdapterAndLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {

            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        if (btAdapter == null) {
            Toast.makeText(getApplicationContext(), "No Bluetooth detected", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnScan = (Button) findViewById(R.id.start_blue);
        Button btnStop = (Button) findViewById(R.id.stop_blue);

        Switch switchTranScan = (Switch) findViewById(R.id.trans_receive);
        switchTranScan.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked) {
                btnScan.setText(getResources().getString(R.string.start_trans));
                btnStop.setText(getResources().getString(R.string.stop_trans));
            } else {
                btnScan.setText(getResources().getString(R.string.start_scan));
                btnStop.setText(getResources().getString(R.string.stop_scan));
            }
        });

        TextView ap = (TextView) findViewById(R.id.blue_ap_choice);

        btnScan.setOnClickListener(v -> {

            setAP = ap.getText().toString();
            Log.i(TAG, setAP);

            scanLeDevice(scanning);
        });

        listViewLE = (ListView) findViewById(R.id.listView1);

        listBluetoothDevice = new ArrayList<>();
        ListAdapter adapterLeScanResult = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listBluetoothDevice);
        listViewLE.setAdapter(adapterLeScanResult);
    }

    private void getBluetoothAdapterAndLeScanner() {

        // Get BluetoothAdapter and BluetoothLeScanner.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = btAdapter.getBluetoothLeScanner();
    }

    @Override
    protected void onResume() {

        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!btAdapter.isEnabled()) {

            if (!btAdapter.isEnabled()) {

                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        scanLeDevice(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {

            finish();
            return;
        }

        getBluetoothAdapterAndLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (btAdapter == null) {

            Toast.makeText(this, "bluetoothManager.getAdapter()==null", Toast.LENGTH_SHORT).show();
            finish();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            super.onScanResult(callbackType, result);

            Log.i(TAG, result.getDevice().getName());

            addBluetoothDevice(result.getDevice());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {

            super.onBatchScanResults(results);
            for (ScanResult result : results) {

                addBluetoothDevice(result.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {

            super.onScanFailed(errorCode);
            Toast.makeText(BluetoothActivity.this, "onScanFailed: " + String.valueOf(errorCode), Toast.LENGTH_LONG).show();
        }

        private void addBluetoothDevice(BluetoothDevice device) {

            if (!listBluetoothDevice.contains(device)) {

                listBluetoothDevice.add(device);
                listViewLE.invalidateViews();
            }
        }
    };

    private void scanLeDevice(final boolean enable) {

        if (enable) {

            listBluetoothDevice.clear();
            listViewLE.invalidateViews();

            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(() -> {

                bluetoothLeScanner.stopScan(scanCallback);
                listViewLE.invalidateViews();

                Toast.makeText(this, "Scan timeout", Toast.LENGTH_LONG).show();

                scanning = false;
                btnScan.setEnabled(true);

            }, SCAN_PERIOD);

            bluetoothLeScanner.startScan(scanCallback);
            scanning = true;
            btnScan.setEnabled(false);

        } else {

            bluetoothLeScanner.stopScan(scanCallback);
            scanning = false;
            btnScan.setEnabled(true);
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
