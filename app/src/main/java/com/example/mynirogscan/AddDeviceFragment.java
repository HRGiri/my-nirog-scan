package com.example.mynirogscan;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.customview.widget.Openable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import static com.example.mynirogscan.MainActivity.FIREBASE_TAG;

public class AddDeviceFragment extends Fragment
        implements DisplayNameFragment.DisplayNameDialogListener,
        WiFiSelectFragment.WifiSelectDialogListener {
    private static final String BLE_TAG = "BLE";

    public static final String SERVICE_UUID = "000000bb-0000-1000-8000-00805f9b34fb";
    public static final String WRITE_CHARACTERISTIC_UUID = "0000bb01-0000-1000-8000-00805f9b34fb";
    public static final String NOTIFY_CHARACTERISTIC_UUID = "0000bb02-0000-1000-8000-00805f9b34fb";

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
    public String displayName;
    public String wifiSsid;
    public String wifiPassword;

    private FragmentManager fragmentManager;

    private TextView tvInfo;
    private Button refreshButton;
    private Button retryButton;
    private ProgressBar progressBar;

    public AddDeviceFragment() {
        // Required empty public constructor
    }

    public static AddDeviceFragment newInstance() {
        return new AddDeviceFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getArguments() != null) {
//            FCMtoken = getArguments().getString(HomeFragment.FCM_TOKEN_EXTRA);
//            deviceID = getArguments().getString(HomeFragment.DEVICE_ID_EXTRA);
//        }
        FCMtoken = AddDeviceFragmentArgs.fromBundle(getArguments()).getFCMToken();
        Log.d("Add device fragment","Fcm token : "+ FCMtoken);
        deviceID = AddDeviceFragmentArgs.fromBundle(getArguments()).getDEVICEID();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_add_device, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

//        Intent intent = getIntent();
//        FCMtoken = intent.getStringExtra(MainActivity.FCM_TOKEN_EXTRA);
//        deviceID = intent.getStringExtra(MainActivity.DEVICE_ID_EXTRA);
//        if (savedInstanceState == null) {
//            fragmentManager = getSupportFragmentManager();
//            fragmentManager.beginTransaction()
//                    .setReorderingAllowed(true)
//                    .add(R.id.fragmentContainerView, WiFiSelectFragment.class, null)
//                    .commit();
//        }

        wifiList = view.findViewById(R.id.wifi_list);
        tvInfo = view.findViewById(R.id.tv_add_device_info);
        refreshButton = view.findViewById(R.id.button_refresh);
        retryButton = view.findViewById(R.id.button_retry);
        progressBar = view.findViewById(R.id.add_device_progress_bar);

//        tvInfo.setText("Connecting with Nirog Scan. Please wait...");
        wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        bluetoothManager = (BluetoothManager) getContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                boolean success = intent.getBooleanExtra(
                        WifiManager.EXTRA_RESULTS_UPDATED, false);
                String action = intent.getAction();
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                    wifiList.setVisibility(View.VISIBLE);
                    refreshButton.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);;

                    tvInfo.setText("Please select a WiFi Access Point to configure NirogScan");
                    List<ScanResult> wifiDeviceList = wifiManager.getScanResults();
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
                            wifiList.setVisibility(View.INVISIBLE);

                            DialogFragment newFragment = new WiFiSelectFragment(AddDeviceFragment.this,deviceListCopy.get(position));
                            newFragment.show(getParentFragmentManager(), WiFiSelectFragment.FRAGMENT_TAG);
//                            fragmentManager = getFragmentManager();
//                            Bundle bundle = new Bundle();
//                            bundle.putString("ssid",deviceListCopy.get(position));
//                            fragmentManager.beginTransaction()
//                                    .setReorderingAllowed(true)
//                                    .add(R.id.fragmentContainerView, WiFiSelectFragment.class, bundle)
//                                    .commit();
//                            fragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks,false);

                        }
                    });
                }
            }
        };

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initWifiScan();
            }
        });

        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                retryButton.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                BluetoothGattService service = btGatt.getService(UUID.fromString(SERVICE_UUID));
                service.getCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID)).setValue("RETRY");
                btGatt.writeCharacteristic(service.getCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID)));

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if(bluetoothAdapter.isEnabled())
            startBleScan();
        else
            promptEnableBluetooth();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
                    Toast.makeText(getContext(), "All permissions granted!", Toast.LENGTH_SHORT).show();
                    startBleScan();
                } else {
                    Toast.makeText(getContext(), "permission not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
            case PERMISSIONS_ACCESS_WIFI_STATE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getContext(), "permission granted", Toast.LENGTH_SHORT).show();
                    wifiManager.startScan();
                } else {
                    Toast.makeText(getContext(), "permission not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
        }
    }

    private void checkPermissions() {
        while (true) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_ACCESS_FINE_LOCATION);
            } else if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        getActivity(), new String[]{Manifest.permission.ACCESS_WIFI_STATE}, PERMISSIONS_ACCESS_WIFI_STATE
                );
            } else if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CHANGE_WIFI_STATE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        getActivity(), new String[]{Manifest.permission.CHANGE_WIFI_STATE}, PERMISSIONS_CHANGE_WIFI_STATE
                );
            } else
                break;
        }
    }

    private boolean isLocationPermissionGranted() {
        return (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    private void requestLocationPermission() {
        if (isLocationPermissionGranted()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage("Starting from Android M (6.0), the system requires apps to be granted " +
                "location access in order to scan for BLE devices.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ActivityCompat.requestPermissions(
                                requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_ACCESS_FINE_LOCATION);
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
        if (!isLocationPermissionGranted()) {
            requestLocationPermission();
        } else {

            isDeviceFound = false;
            isBleScanning = false;

            progressBar.setVisibility(View.VISIBLE);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() { tvInfo.setText("Searching for a Nirog Scan...");
                }
            });

            Log.d(BLE_TAG, bluetoothAdapter.getBondedDevices().toString());
            ScanFilter nameFilter = new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString(SERVICE_UUID))
//                        .setDeviceName("T8")
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
                public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
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
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    nirogScanDevice.connectGatt(
                                            getContext(),
                                            false,
                                            gattCallback,
                                            BluetoothDevice.TRANSPORT_LE,
                                            BluetoothDevice.PHY_LE_CODED);
                                }
                                else {
                                    nirogScanDevice.connectGatt(
                                            getContext(),
                                            true,
                                            gattCallback,
                                            BluetoothDevice.TRANSPORT_LE);
                                }
                            }
                        },500);

                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    Log.e(BLE_TAG,"Error Code: " + errorCode);
                    startBleScan();
                }
            };
//                bleScanner.stopScan(scanCallback);
            bleScanner.startScan(filters, scanSettings, scanCallback);
            isBleScanning = true;
        }
    }

    public void stopBleScan() {
        bleScanner.stopScan(scanCallback);
        isBleScanning = false;
    }

    private void initWifiScan(){
        //TODO: Enable Location if not enabled
//        checkPermissions();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        getActivity().registerReceiver(wifiScanReceiver, intentFilter);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
                refreshButton.setVisibility(View.INVISIBLE);
                tvInfo.setText("Searching for available WiFi networks");
            }
        });


        if (!wifiManager.isWifiEnabled()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getContext(), "Turning WiFi ON...", Toast.LENGTH_LONG).show();
                }
            });
            wifiManager.setWifiEnabled(true);
        }
        getWifi();
    }

    private void getWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "location turned off", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(getActivity(), new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE
                }, PERMISSIONS_ACCESS_WIFI_STATE);
            } else {
                wifiManager.startScan();
            }
        } else {
            Toast.makeText(getContext(), "scanning", Toast.LENGTH_SHORT).show();
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
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            btGatt.discoverServices();
                        }
                    },500);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w(BLE_TAG, "Successfully disconnected from " + deviceAddress);
                    gatt.close();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Navigation.findNavController(requireActivity(),R.id.main_activity_nav_host).navigateUp();
                        }
                    });
//                    getParentFragmentManager().beginTransaction().remove(AddDeviceFragment.this).commit();
                }
            } else {
                Log.w(BLE_TAG, "Error " + status + " encountered for " + deviceAddress + "! Disconnecting...");
                isDeviceFound = false;
                gatt.close();
                startBleScan();
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
                Log.i(BLE_TAG, "Data = " + characteristic.getStringValue(0));
            }
            else {
                Log.i(BLE_TAG, "Characteristic: " + characteristic.toString() + " write failed :( with status: " + status);
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String response = characteristic.getStringValue(0).trim();
            Log.i(BLE_TAG, "Notify characteristic " + characteristic.getUuid() + ":\t" + response);
            switch (response){
                case "CONNECTED":
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            wifiList.setVisibility(View.INVISIBLE);
                            refreshButton.setVisibility(View.INVISIBLE);
                            progressBar.setVisibility(View.VISIBLE);
                            Toast.makeText(getContext(),"WiFi connected!",Toast.LENGTH_SHORT).show();
                            tvInfo.setText("We are almost done!\nJust finishing up!");
                        }
                    });
                    break;
                case "AUTH_FAIL":
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getContext(),"Not connected! Please enter a correct password for WiFi",Toast.LENGTH_SHORT).show();
                        }
                    });
//                gatt.disconnect();
                    initWifiScan();
                    break;
                case "COMPLETED":
                    btGatt.disconnect();
                    break;
                case "CONN_SUCCESS":
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.INVISIBLE);
                        }
                    });
                    uploadDeviceDetails();
                    break;
                case "UPLOAD_FAIL":
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            retryButton.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(getContext(),"Could not establish connection to Database. Please try again!",Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.w(BLE_TAG, "Successfully changed mtu to " + mtu);

            BluetoothGattService service = btGatt.getService(UUID.fromString(SERVICE_UUID));
            btGatt.setCharacteristicNotification(service.getCharacteristic(UUID.fromString(NOTIFY_CHARACTERISTIC_UUID)),true);
//            initWifiScan();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvInfo.setText("Please enter a nickname/display name for your NirogScan");
                    progressBar.setVisibility(View.INVISIBLE);
                }
            });

            DialogFragment newFragment = new DisplayNameFragment(AddDeviceFragment.this);
            newFragment.show(getParentFragmentManager(), DisplayNameFragment.TAG);

//            fragmentManager = getFragmentManager();
//            fragmentManager.beginTransaction()
//                    .setReorderingAllowed(true)
//                    .add(R.id.fragmentContainerView, DisplayNameFragment.class,null)
//                    .commit();
//            fragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks,false);
        }
    };

    FragmentManager.FragmentLifecycleCallbacks fragmentLifecycleCallbacks = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            super.onFragmentDestroyed(fm, f);
            fragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks);
            if(f instanceof WiFiSelectFragment ) {
                Log.d(BLE_TAG, "SSID: " + wifiSsid + " Password: " + wifiPassword);
                getActivity().unregisterReceiver(wifiScanReceiver);
//            userID = deviceID = FCMtoken = displayName = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
                userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
//                deviceID = userID + "-01";
//                displayName = "xxxxxxxxxxxxxxx";
                String message = userID + "," + deviceID + "," + FCMtoken + "," + displayName + "," + wifiSsid + "," + wifiPassword;
//            byte[] payload = message.getBytes(StandardCharsets.UTF_8);
                BluetoothGattService service = btGatt.getService(UUID.fromString(SERVICE_UUID));

                service.getCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID))
                        .setValue(message);
                btGatt.writeCharacteristic(service.getCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID)));
            }
            else if(f instanceof DisplayNameFragment){
                Log.d(BLE_TAG,displayName);
                initWifiScan();
            }
        }
    };

    private void createReadingsDocument(){
        Map<String,Object> readingDoc = new HashMap<>();
        readingDoc.put("created","" + Calendar.getInstance().getTimeInMillis());
        readingDoc.put("uuid",deviceID);

        // Add a new document with a generated ID
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(userID)
                .collection("Readings")
                .add(readingDoc)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(FIREBASE_TAG, "DocumentSnapshot add with id " + documentReference.getId());
                        BluetoothGattService service = btGatt.getService(UUID.fromString(SERVICE_UUID));
                        service.getCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID)).setValue("SUCCESS");
                        btGatt.writeCharacteristic(service.getCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID)));
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvInfo.setText("Configuration complete!");
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception e) {
                        Log.w(FIREBASE_TAG, "Error adding document", e);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvInfo.setText("Failed to upload to database. Please try again!");
                            }
                        });
                    }
                });

    }
    private void uploadDeviceDetails() {
        // Create a new user with a first and last name
        Map<String, Object> user = new HashMap<>();
        Map<String, String> device = new HashMap<>();
        device.put("name",displayName);
        device.put("uuid",deviceID);

        // Add a new document with a generated ID
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(userID)
                .update("device_list", FieldValue.arrayUnion(device))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void mVoid) {
                        Log.d(FIREBASE_TAG, "DocumentSnapshot updated successfully");
                        createReadingsDocument();
//                        btGatt.disconnect();
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                tvInfo.setText("Configuration complete!");
//                            }
//                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception e) {
                        Log.w(FIREBASE_TAG, "Error adding document", e);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvInfo.setText("Failed to upload to database. Please try again!");
                            }
                        });
                    }
                });
    }

    @Override
    public void onSubmitDisplayName(DialogFragment dialog) {
        Log.d(BLE_TAG,displayName);
        initWifiScan();
    }

    @Override
    public void onSubmitWifiPassword(DialogFragment dialog) {
        Log.d(BLE_TAG, "SSID: " + wifiSsid + " Password: " + wifiPassword);
        getActivity().unregisterReceiver(wifiScanReceiver);
//            userID = deviceID = FCMtoken = displayName = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
//                deviceID = userID + "-01";
//                displayName = "xxxxxxxxxxxxxxx";
        String message = userID + "," + deviceID + "," + FCMtoken + "," + displayName + "," + wifiSsid + "," + wifiPassword;
//            byte[] payload = message.getBytes(StandardCharsets.UTF_8);
        BluetoothGattService service = btGatt.getService(UUID.fromString(SERVICE_UUID));

        service.getCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID))
                .setValue(message);
        btGatt.writeCharacteristic(service.getCharacteristic(UUID.fromString(WRITE_CHARACTERISTIC_UUID)));
    }
}
