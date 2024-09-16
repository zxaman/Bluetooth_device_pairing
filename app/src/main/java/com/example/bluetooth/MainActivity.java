package com.example.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private DeviceAdapter deviceAdapter;

    // UI Components
    private RecyclerView availableDevicesRecyclerView;
    private Button scanButton, stopScanButton;
    private ProgressBar scanningProgressBar;

    // UUID for secure Bluetooth connection (SPP)
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        setupUI(); // Initialize UI components
        checkAndRequestPermissions(); // Check for necessary permissions
    }

    private void setupUI() {
        // Initialize the buttons, progress bar, and RecyclerView
        scanButton = findViewById(R.id.scanButton);
        stopScanButton = findViewById(R.id.stopScanButton);
        scanningProgressBar = findViewById(R.id.scanningProgressBar);

        // RecyclerView for displaying available devices
        availableDevicesRecyclerView = findViewById(R.id.deviceRecyclerView);
        availableDevicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize the DeviceAdapter with an empty list
        deviceAdapter = new DeviceAdapter(this, discoveredDevices, this::connectToDevice);
        availableDevicesRecyclerView.setAdapter(deviceAdapter);

        // Set click listeners for the buttons
        scanButton.setOnClickListener(v -> {
            if (bluetoothAdapter.isEnabled()) {
                startDeviceScan();
            } else {
                notifyBluetoothDisabled();
            }
        });

        stopScanButton.setOnClickListener(v -> stopDeviceScan());
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_PERMISSIONS);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_PERMISSIONS);
        } else {
            ensureBluetoothIsEnabled();
        }
    }

    private void ensureBluetoothIsEnabled() {
        if (!bluetoothAdapter.isEnabled()) {
            notifyBluetoothDisabled();
        }
    }

    private void notifyBluetoothDisabled() {
        Toast.makeText(this, "Bluetooth is disabled. Please enable Bluetooth to scan.", Toast.LENGTH_SHORT).show();
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private void startDeviceScan() {
        // Check for Bluetooth Scan permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_PERMISSIONS);
            return;
        }

        // Clear previously discovered devices
        discoveredDevices.clear();
        deviceAdapter.notifyDataSetChanged();

        // Start Bluetooth discovery
        bluetoothAdapter.startDiscovery();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(deviceFoundReceiver, filter);

        // Show progress bar and hide scan button
        scanningProgressBar.setVisibility(View.VISIBLE);
        scanButton.setVisibility(View.GONE);
        stopScanButton.setVisibility(View.VISIBLE);
    }

    private void stopDeviceScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Hide progress bar and show scan button
        scanningProgressBar.setVisibility(View.GONE);
        scanButton.setVisibility(View.VISIBLE);
        stopScanButton.setVisibility(View.GONE);
    }

    // BroadcastReceiver to discover devices
    private final BroadcastReceiver deviceFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // Add discovered unpaired devices to the list
                    if (!discoveredDevices.contains(device)) {
                        discoveredDevices.add(device);
                        deviceAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        stopDeviceScan(); // Stop scanning when a device is selected
        new ConnectTask(device).execute();
    }

    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {
        private final BluetoothDevice device;
        private BluetoothSocket bluetoothSocket;

        ConnectTask(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return false; // Return false if permission is not granted
                }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                bluetoothSocket.connect();
                return true;
            } catch (IOException e) {
                Log.e("Bluetooth", "Connection failed", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                Toast.makeText(MainActivity.this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Failed to connect to " + device.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(deviceFoundReceiver);
    }
}
