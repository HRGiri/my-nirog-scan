package com.example.mynirogscan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddDeviceActivity extends AppCompatActivity {

    private static final String BLE_TAG = "BLE";

    public static final String SERVICE_UUID = "000000bb-0000-1000-8000-00805f9b34fb";
    public static final String WRITE_CHARACTERISTIC_UUID = "0000bb01-0000-1000-8000-00805f9b34fb";

    private static final int PERMISSIONS_ACCESS_FINE_LOCATION = 3;
    private static final int ENABLE_BLUETOOTH_REQUEST_CODE = 4;
    private static final int PERMISSIONS_ACCESS_WIFI_STATE = 5;
    private static final int PERMISSIONS_CHANGE_WIFI_STATE = 6;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private ScanCallback scanCallback;
    private BluetoothDevice nirogScanDevice;
    private BluetoothGatt btGatt;

    private boolean isBleScanning = false;
    private boolean isDeviceFound = false;

    private ListView wifiList;
    private WifiManager wifiManager;
    BroadcastReceiver wifiScanReceiver;

    private String userID;
    private String deviceID;
    private String FCMtoken;
    private String displayName;
    public String wifiSsid;
    public String wifiPassword;

    private FragmentManager fragmentManager;

    private TextView tvInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_device);

//        if (savedInstanceState == null) {
//            fragmentManager = getSupportFragmentManager();
//            fragmentManager.beginTransaction()
//                    .setReorderingAllowed(true)
//                    .add(R.id.fragmentContainerView, WiFiSelectFragment.class, null)
//                    .commit();
//        }
        wifiList = findViewById(R.id.wifi_list);
        tvInfo = findViewById(R.id.tv_add_device_info);
//        tvInfo.setText("Connecting with Nirog Scan. Please wait...");
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                boolean success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
                String action = intent.getAction();
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                    tvInfo.setText("Please select a WiFi Access Point to configure NirogScan");
                    List<android.net.wifi.ScanResult> wifiDeviceList = wifiManager.getScanResults();
                    ArrayList<String> deviceList = new ArrayList<>();
                    for (android.net.wifi.ScanResult scanResult : wifiDeviceList) {
                        deviceList.add(scanResult.SSID /*+ " - " + scanResult.capabilities*/);
                    }
//                    Toast.makeText(c, "Scan Complete", Toast.LENGTH_SHORT).show();
                    final ArrayList<String> deviceListCopy = new ArrayList<>(deviceList);
                    ArrayAdapter arrayAdapter = new ArrayAdapter(c, R.layout.wifi_list_item, deviceList.toArray());
                    wifiList.setAdapter(arrayAdapter);
                    wifiList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            tvInfo.setText("Please enter the password for " + deviceListCopy.get(position) + " to continue");
                            fragmentManager = getSupportFragmentManager();
                            Bundle bundle = new Bundle();
                            bundle.putString("ssid",deviceListCopy.get(position));
                            fragmentManager.beginTransaction()
                                    .setReorderingAllowed(true)
                                    .add(R.id.fragmentContainerView, WiFiSelectFragment.class, bundle)
                                    .commit();
                            fragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks,false);

                        }
                    });
                }
            }
        };

    }

    @Override
    protected void onStart() {
        super.onStart();
//        initWifiScan();
        if(bluetoothAdapter.isEnabled())
            startBleScan();
        else
            promptEnableBluetooth();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ENABLE_BLUETOOTH_REQUEST_CODE:
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth();
                }
                else startBleScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(AddDeviceActivity.this, "All permissions granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AddDeviceActivity.this, "permission not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            case PERMISSIONS_ACCESS_WIFI_STATE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(AddDeviceActivity.this, "permission granted", Toast.LENGTH_SHORT).show();
                    wifiManager.startScan();
                } else {
                    Toast.makeText(AddDeviceActivity.this, "permission not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
//        receiverWifi = new WiFiReceiver(wifiManager, wifiList);
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
//        registerReceiver(wifiScanReceiver, intentFilter);
//        initWifiScan();
    }
    @Override
    protected void onPause() {
        super.onPause();
//        unregisterReceiver(wifiScanReceiver);
    }


    private void checkPermissions() {
        while (true) {
            if (ActivityCompat.checkSelfPermission(AddDeviceActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        AddDeviceActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_ACCESS_FINE_LOCATION);
            } else if (ActivityCompat.checkSelfPermission(AddDeviceActivity.this, Manifest.permission.ACCESS_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        AddDeviceActivity.this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, PERMISSIONS_ACCESS_WIFI_STATE
                );
            } else if (ActivityCompat.checkSelfPermission(AddDeviceActivity.this, Manifest.permission.CHANGE_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        AddDeviceActivity.this, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, PERMISSIONS_CHANGE_WIFI_STATE
                );
            } else
                break;
        }
    }

    private boolean isLocationPermissionGranted() {
        return (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    private void requestLocationPermission() {
        if (isLocationPermissionGranted()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(AddDeviceActivity.this);
        builder.setMessage("Starting from Android M (6.0), the system requires apps to be granted " +
                "location access in order to scan for BLE devices.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ActivityCompat.requestPermissions(
                                AddDeviceActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_ACCESS_FINE_LOCATION);
                    }
                });
        //Creating dialog box
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE);
        }
    }

    public void startBleScan(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isLocationPermissionGranted()) {
                requestLocationPermission();
            } else {
                isDeviceFound = false;
                isBleScanning = false;
                Log.d(BLE_TAG, bluetoothAdapter.getBondedDevices().toString());
                ScanFilter nameFilter = new ScanFilter.Builder()
//                        .setServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
                        .setDeviceName("T8")
                        .build();
                List<ScanFilter> filters = new ArrayList<>();
                filters.add(nameFilter);
                ScanSettings scanSettings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
//                        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
//                        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                        .build();

                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
                scanCallback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        super.onScanResult(callbackType, result);
//                        Log.i("ScanCallback", "Found BLE device! Name: " + result.getDevice().getName() + ", address: " + result.getDevice().getAddress());
                        if(!isDeviceFound) {
                            isDeviceFound = true;
                            Log.i(BLE_TAG, "Found BLE device! Name: " + result.getDevice().getName() + ", address: " + result.getDevice().getAddress());
                            if (isBleScanning) {
                                isBleScanning = false;
                                stopBleScan();
                            }
                            nirogScanDevice = result.getDevice();
                            Log.w(BLE_TAG, "Connecting to " + nirogScanDevice.getAddress());
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        nirogScanDevice.connectGatt(
                                                getApplicationContext(),
                                                false,
                                                gattCallback,
                                                BluetoothDevice.TRANSPORT_LE,
                                                BluetoothDevice.PHY_LE_CODED);
                                    }
                                    else {
                                        nirogScanDevice.connectGatt(
                                                getApplicationContext(),
                                                true,
                                                gattCallback,
                                                BluetoothDevice.TRANSPORT_LE);
                                    }
                                }
                            });

                        }
                    }

                    @Override
                    public void onScanFailed(int errorCode) {
                        super.onScanFailed(errorCode);
                        Log.e(BLE_TAG,"Error Code: " + errorCode);
                        startBleScan();
                    }
                };
                bleScanner.startScan(filters, scanSettings, scanCallback);
                isBleScanning = true;
            }
        }
    }

    public void stopBleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bleScanner.stopScan(scanCallback);
        }
        isBleScanning = false;
    }

    private void initWifiScan(){
//        checkPermissions();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);

        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }
        getWifi();
    }

    private void getWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(AddDeviceActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(AddDeviceActivity.this, "location turned off", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(AddDeviceActivity.this, new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE
                }, PERMISSIONS_ACCESS_WIFI_STATE);
            } else {
                wifiManager.startScan();
            }
        } else {
            Toast.makeText(AddDeviceActivity.this, "scanning", Toast.LENGTH_SHORT).show();
            wifiManager.startScan();
        }
    }


    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            String deviceAddress = gatt.getDevice().getAddress();

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w(BLE_TAG, "Successfully connected to " + deviceAddress);
                    btGatt = gatt;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            btGatt.discoverServices();
                        }
                    });
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w(BLE_TAG, "Successfully disconnected from " + deviceAddress);
                    gatt.close();
                }
            } else {
                Log.w(BLE_TAG, "Error " + status + " encountered for " + deviceAddress + "! Disconnecting...");
                isDeviceFound = false;
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.w(BLE_TAG, "Discovered " + gatt.getServices().size() + " services for " + nirogScanDevice.getAddress());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                gatt.requestMtu(500);
            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if(status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(BLE_TAG, "Characteristic: " + characteristic.toString() + " write success! :)");
                gatt.disconnect();
            }
            else {
                Log.i(BLE_TAG, "Characteristic: " + characteristic.toString() + " write failed :( with status: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.w(BLE_TAG, "Successfully changed mtu to " + mtu);
            initWifiScan();

        }
    };

    FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentDestroyed(fm, f);
            Log.d(BLE_TAG,"SSID: " + wifiSsid + " Password: " + wifiPassword);
            fragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
            unregisterReceiver(wifiScanReceiver);
            userID = deviceID = FCMtoken = displayName = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
            String message = userID + "," + deviceID + "," + FCMtoken + "," + displayName + "," + wifiSsid + "," + wifiPassword;
//            byte[] payload = message.getBytes(StandardCharsets.UTF_8);
            btGatt.getService(UUID.fromString(SERVICE_UUID))
                    .getCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID))
                    .setValue(message);
            btGatt.writeCharacteristic(btGatt
                    .getService(UUID.fromString(SERVICE_UUID))
                    .getCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID)));
        }
    };

}