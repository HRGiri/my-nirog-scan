package com.example.mynirogscan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.BroadcastReceiver;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class AddDeviceFragment extends Fragment {
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

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AddDeviceFragment newInstance() {
        return new AddDeviceFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_add_device, container, false);
    }
}
