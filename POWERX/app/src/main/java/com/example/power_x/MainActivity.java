package com.example.power_x;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 100;
    private static final int REQUEST_ENABLE_BLUETOOTH = 101;

    private BluetoothAdapter bluetoothAdapter;
    private DeviceAdapter deviceListAdapter;

    private boolean locationPrompted = false;

    private TextView bluetoothStatus, locationStatus;
    private ListView deviceListView;
    private Handler handler = new Handler();
    private Runnable connectionCheckRunnable;
    private boolean isNavigatingToDetails = false;



    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    String name = device.getName() != null ? device.getName() : "Unknown Device";
                    int icon = getDeviceIcon(device);
                    Device newDevice = new Device(name, icon);
                    boolean deviceExists = false;
                    for (int i = 0; i < deviceListAdapter.getCount(); i++) {
                        Device existingDevice = deviceListAdapter.getItem(i);
                        if (existingDevice != null && existingDevice.getName().equals(newDevice.getName())) {
                            deviceExists = true;
                            break;
                        }
                    }
                    if (!deviceExists) {
                        deviceListAdapter.add(newDevice);
                        deviceListAdapter.notifyDataSetChanged();
                    }
                }
            }
        }
    };

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateStatusIndicators();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.parseColor("#1F1F1F"));
        }


        connectionCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isNavigatingToDetails) return;

                BluetoothDevice connected = getConnectedBluetoothDevice();
                if (connected != null && connected.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO) {
                    isNavigatingToDetails = true;
                    Intent i = new Intent(MainActivity.this, DeviceDetailsActivity.class);
                    i.putExtra("device_name", connected.getName());
                    i.putExtra("device_address", connected.getAddress());
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                } else {
                    handler.postDelayed(this, 2000);
                }
            }
        };

        ImageView refreshButton = findViewById(R.id.refresh_button);

        refreshButton.setOnClickListener(v -> {
            v.animate()
                    .rotationBy(360f)
                    .setDuration(600)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .start();
        });



        handler.post(connectionCheckRunnable);


        BluetoothDevice connected = getConnectedBluetoothDevice();
        if (connected != null && connected.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO) {
            Intent i = new Intent(this, DeviceDetailsActivity.class);
            i.putExtra("device_name", connected.getName());
            i.putExtra("device_address", connected.getAddress());
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }



        bluetoothStatus = findViewById(R.id.bluetooth_status);
        locationStatus = findViewById(R.id.location_status);
        refreshButton = findViewById(R.id.refresh_button);
        deviceListView = findViewById(R.id.device_list);

        deviceListAdapter = new DeviceAdapter(this, new ArrayList<>());
        deviceListView.setAdapter(deviceListAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        updateStatusIndicators();
        checkPermissionsAndStart();

        refreshButton.setOnClickListener(v -> {

            requestBluetoothEnable();
            v.animate()
                    .rotationBy(360f)
                    .setDuration(600)
                    .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator())
                    .start();

        });

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        });
        
        Toolbar toolbar = findViewById(R.id.tb);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("POWER - X");
    }
    @Override
    public void onBackPressed() {
        BluetoothDevice connectedDevice = getConnectedBluetoothDevice();
        if (connectedDevice != null) {
            BluetoothClass deviceClass = connectedDevice.getBluetoothClass();
            if (deviceClass != null && deviceClass.getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO) {
                Intent intent = new Intent(MainActivity.this, DeviceDetailsActivity.class);
                intent.putExtra("device_name", connectedDevice.getName());
                intent.putExtra("device_address", connectedDevice.getAddress());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Connect to Pairable device.", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onBackPressed();
        }
    }


    private BluetoothDevice getConnectedBluetoothDevice() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                int connectionState = bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET);
                if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                    return device;
                }
            }
        }
        return null;
    }


    private void checkPermissionsAndStart() {
        updateStatusIndicators();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            checkLocationAndStartScan();
        }
    }

    private void requestBluetoothEnable() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        }
        checkPermissionsAndStart();
    }

    private void checkLocationAndStartScan() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean locationEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!locationEnabled && !locationPrompted) {
            locationPrompted = true;
            new AlertDialog.Builder(this)
                    .setTitle("Enable Location")
                    .setMessage("To scan for Bluetooth devices, location must be enabled.")
                    .setPositiveButton("Settings", (dialog, which) ->
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            startBluetoothScan();
        }

        updateStatusIndicators();
    }

    private void startBluetoothScan() {
        deviceListAdapter.clear();
        registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return;
        }


        bluetoothAdapter.startDiscovery();
    }

    private int getDeviceIcon(BluetoothDevice device) {
        if (device.getBluetoothClass() != null) {
            int deviceClass = device.getBluetoothClass().getDeviceClass();

            if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                    deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                    deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE) {
                return R.drawable.ic_headphones;
            } else if (deviceClass == BluetoothClass.Device.COMPUTER_LAPTOP ||
                    deviceClass == BluetoothClass.Device.COMPUTER_DESKTOP) {
                return R.drawable.ic_laptop;
            } else if (deviceClass == BluetoothClass.Device.PHONE_SMART ||
                    deviceClass == BluetoothClass.Device.PHONE_CELLULAR) {
                return R.drawable.ic_phone;
            }
        }
        return R.drawable.ic_launcher_foreground;
    }

    private void updateStatusIndicators() {
        boolean bluetoothOn = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean locationOn = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);


        bluetoothStatus.setVisibility(bluetoothOn ? View.GONE : View.VISIBLE);
        locationStatus.setVisibility(locationOn ? View.GONE : View.VISIBLE);

        LinearLayout ll = findViewById(R.id.llhide);
        if (bluetoothOn && locationOn) {
            ll.setVisibility(View.GONE);
        }
        else{
            ll.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        locationPrompted = false;
        updateStatusIndicators();
        registerReceiver(locationReceiver, new IntentFilter("android.location.PROVIDERS_CHANGED"));

        connectionCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isNavigatingToDetails) return;

                BluetoothDevice connected = getConnectedBluetoothDevice();
                if (connected != null && connected.getBluetoothClass().getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO) {
                    isNavigatingToDetails = true;
                    Intent i = new Intent(MainActivity.this, DeviceDetailsActivity.class);
                    i.putExtra("device_name", connected.getName());
                    i.putExtra("device_address", connected.getAddress());
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                } else {
                    handler.postDelayed(this, 2000);
                }
            }
        };


        handler.post(connectionCheckRunnable);


        BluetoothDevice connectedDevice = getConnectedBluetoothDevice();
        if (connectedDevice != null) {
            Intent intent = new Intent(MainActivity.this, DeviceDetailsActivity.class);
            intent.putExtra("device_name", connectedDevice.getName());
            intent.putExtra("device_address", connectedDevice.getAddress());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(locationReceiver);
        handler.removeCallbacks(connectionCheckRunnable);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothReceiver);
        handler.removeCallbacks(connectionCheckRunnable);

    }
}
