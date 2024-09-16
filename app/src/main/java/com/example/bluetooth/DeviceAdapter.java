package com.example.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private ArrayList<BluetoothDevice> devices;
    private OnDeviceClickListener listener;
    private Context context; // To check permissions

    // Interface for handling click events on devices
    public interface OnDeviceClickListener {
        void onDeviceClick(BluetoothDevice device);
    }

    // Constructor to initialize the adapter with the list of devices, context, and listener
    public DeviceAdapter(Context context, ArrayList<BluetoothDevice> devices, OnDeviceClickListener listener) {
        this.context = context;
        this.devices = devices;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each device in the list
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        // Get the Bluetooth device at the current position
        BluetoothDevice device = devices.get(position);

        // Check for BLUETOOTH_CONNECT permission (Android 12+)
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Set the device name and address in the TextView
        holder.deviceName.setText(device.getName() != null ? device.getName() : "Unknown Device");
        holder.deviceAddress.setText(device.getAddress());

        // Set a click listener for the item
        holder.itemView.setOnClickListener(v -> listener.onDeviceClick(device));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        // TextView to display the device name and address
        TextView deviceName;
        TextView deviceAddress;

        public DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize the TextViews to display Bluetooth device name and address
            deviceName = itemView.findViewById(android.R.id.text1);
            deviceAddress = itemView.findViewById(android.R.id.text2);
        }
    }
}
