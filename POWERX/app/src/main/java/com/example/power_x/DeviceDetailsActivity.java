package com.example.power_x;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import java.lang.reflect.Method;
import java.util.Set;

public class DeviceDetailsActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private TextView leftBatteryTextView;
    private TextView deviceNameTextView;
    private TextView deviceAddressTextView;

    private boolean btnstatus = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.parseColor("#1F1F1F"));
        }

        leftBatteryTextView = findViewById(R.id.left_battery_status);
        deviceAddressTextView = findViewById(R.id.device_address);

        String deviceName = getIntent().getStringExtra("device_name");
        String deviceAddress = getIntent().getStringExtra("device_address");

        if (getSupportActionBar() != null) {
            TextView titleTextView = new TextView(this);
            titleTextView.setText(deviceName != null ? deviceName : "Unknown Device");
            titleTextView.setTextColor(Color.WHITE);
            titleTextView.setTextSize(20);
            titleTextView.setPadding((int) (25 * getResources().getDisplayMetrics().density), 0, 0, 0);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setCustomView(titleTextView);
        }


        deviceAddressTextView.setText(deviceAddress);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, 1);
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is off", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Location is off", Toast.LENGTH_SHORT).show();
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
        if (device == null) {
            Toast.makeText(this, "Device not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        loadGestureInfo();

        Button btn1 = findViewById(R.id.equalizer_btn);
        btn1.setOnClickListener(v -> {
            Intent intent = new Intent(DeviceDetailsActivity.this, EqualizerActivity.class);
            startActivity(intent);
        });

        getBatteryLevels(device);

        Button btn = findViewById(R.id.connect_disconnect_button);
        BluetoothDevice connectedDevice = getConnectedBluetoothDevice();
        if (connectedDevice != null) {
            btn.setText("Disconnect");
        }

        btn.setOnClickListener(v -> {
            if (btnstatus){
                btn.setText("Connect");
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                }
                btnstatus = false;
            }else{
                startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                bluetoothAdapter.enable();
                btn.setText("Disconnect");
                btnstatus = true;
            }
        });
    }

    private void loadGestureInfo() {
        String[][] gestures = {
                {"Volume Up", "Press & hold right earbud"},
                {"Volume Down", "Press & hold left earbud"},
                {"Play/Pause Music", "Single tap on either earbud"},
                {"Next Track", "Press & hold right earbud"},
                {"Previous Track", "Press & hold left earbud"},
                {"Answer/End Call", "Single tap during call"},
                {"Reject Call", "Double tap during incoming call"},
                {"Activate Voice Assistant", "Triple tap on left earbud to trigger Google Assistant or Siri"},
                {"Call most recent", "Triple tap on right earbud to call most recent person"}
        };

        LinearLayout container = findViewById(R.id.gesture_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String[] gesture : gestures) {
            View card = inflater.inflate(R.layout.gesture_item, container, false);

            TextView title = card.findViewById(R.id.gesture_title);
            TextView desc = card.findViewById(R.id.gesture_description);

            title.setText(gesture[0]);
            desc.setText(gesture[1]);
            desc.setVisibility(View.GONE);

            title.setOnClickListener(v -> {
                if (desc.getVisibility() == View.GONE) {
                    desc.setVisibility(View.VISIBLE);
                    desc.setAlpha(0f);
                    desc.setTranslationY(-desc.getHeight() / 2f);
                    desc.animate()
                            .alpha(1f)
                            .translationY(0)
                            .setDuration(300)
                            .withStartAction(() -> desc.setVisibility(View.VISIBLE))
                            .start();
                } else {
                    desc.animate()
                            .alpha(0f)
                            .translationY(-desc.getHeight() / 2f)
                            .setDuration(300)
                            .withEndAction(() -> desc.setVisibility(View.GONE))
                            .start();
                }
            });

            container.addView(card);
        }
    }



    @Override
    public void onBackPressed() {
        BluetoothDevice connectedDevice = getConnectedBluetoothDevice();
        if (connectedDevice != null) {
            BluetoothClass deviceClass = connectedDevice.getBluetoothClass();
            Button btn = findViewById(R.id.connect_disconnect_button);
            if (deviceClass != null && deviceClass.getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO) {
                if (btnstatus) {
                    btn.setText("Connect");
                    btnstatus = false;
                }else{
                    btn.setText("Connect");
                    btnstatus = true;
                }
            } else {
                btn.setText("Connect");
                btnstatus = false;
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

    private void getBatteryLevels(BluetoothDevice device) {
        try {
            BluetoothDevice leftEarbud = bluetoothAdapter.getRemoteDevice(device.getAddress());
            if (isDeviceConnected(leftEarbud)) {
                int leftBatteryLevel = getBatteryLevelForEarbud(leftEarbud);
                leftBatteryTextView.setText("Battery: " + leftBatteryLevel + "%");
            } else {
                leftBatteryTextView.setText("Battery: Not Connected");
            }

        } catch (Exception e) {
            e.printStackTrace();
            leftBatteryTextView.setText("Battery: Disconnected");
            Toast.makeText(this, "Battery level fetch failed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isDeviceConnected(BluetoothDevice earbud) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            return false;
        }
        return bluetoothAdapter.getBondedDevices().contains(earbud);
    }

    private int getBatteryLevelForEarbud(BluetoothDevice earbud) {
        try {
            Class<?> bluetoothDeviceClass = earbud.getClass();
            Method getBatteryLevelMethod = bluetoothDeviceClass.getMethod("getBatteryLevel");
            return (int) getBatteryLevelMethod.invoke(earbud);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }
}
