package com.example.mynirogscan;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.mynirogscan.Constants.*;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends DialogFragment implements AdapterView.OnItemSelectedListener {

    public static final String TAG = "com.example.mynirogscan.SettingsDialog";
    private String deviceId;
    EditText etDeviceName;
    EditText etWifiSSID;
    public EditText etWifiPass;
    Spinner deviceSelector;
    Button submitButton;

    ArrayList<String> deviceNameList = new ArrayList<>();
    String[] deviceArray = {"Device 1","Device 2","Device 3"};
    private GlobalData globalData;
    private ArrayList<Map<String, String>> deviceList;
    private List<DocumentSnapshot> deviceDataList;
    private DocumentSnapshot deviceData;
    private int position = 0;
    private Observer<? super Map<String, Object>> userDataObserver = new Observer<Map<String, Object>>() {
        @Override
        public void onChanged(Map<String, Object> userData) {
            deviceList = (ArrayList<Map<String, String>>) userData.get(Constants.DEVICE_LIST_FIELD_NAME);
            for(Map<String,String> map:deviceList){
                String name = map.get(Constants.NAME_FIELD_NAME);
                deviceNameList.add(name);
                if(deviceId.equals("" + map.get("uuid"))){
                    position = deviceNameList.indexOf(name);
                }

            }
            // Create the instance of ArrayAdapter
            ArrayAdapter adapter = new ArrayAdapter(getContext(),
                    android.R.layout.simple_spinner_item,
                    deviceNameList);

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            deviceSelector.setAdapter(adapter);
            deviceSelector.setSelection(position);
        }
    };

    public interface SettingsDialogListener {
        public void onSubmit(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    SettingsDialogListener listener;

    public SettingsFragment() {
        // Required empty public constructor

    }

    public SettingsFragment(Fragment fragment, String deviceId){
        this.deviceId = deviceId;
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (SettingsDialogListener) fragment;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException("Must implement SettingsDialogListener");
        }
    }

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        // Verify that the host activity implements the callback interface
//        try {
//            // Instantiate the NoticeDialogListener so we can send events to the host
//            Log.d("Settings",context.toString());
//            listener = (SettingsDialogListener) context;
//        } catch (ClassCastException e) {
//            // The activity doesn't implement the interface, throw exception
//            throw new ClassCastException("Must implement SettingsDialogListener");
//        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        etDeviceName = view.findViewById(R.id.et_settings_device_name);
        etWifiSSID = view.findViewById(R.id.et_settings_wifi_ssid);
        etWifiPass = view.findViewById(R.id.et_settings_wifi_password);
        deviceSelector = view.findViewById(R.id.settings_device_dropdown);
        submitButton = view.findViewById(R.id.settings_device_conf_btn);



        deviceSelector.setOnItemSelectedListener(this);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                listener.onSubmit(SettingsFragment.this);
                Map<String,Object> map = new HashMap<>();
                map.put(DEVICE_ID_FIELD_NAME,deviceId);
                String name = etDeviceName.getText().toString().trim();
                if(!name.equals(deviceData.get(DEVICE_NAME_FIELD_NAME)))
                    map.put(DEVICE_NAME_FIELD_NAME,etDeviceName.getText().toString().trim());
                name = etWifiSSID.getText().toString().trim();
                if(!name.equals(deviceData.get(DEVICE_WIFI_SSID_FIELD_NAME)))
                    map.put(DEVICE_WIFI_SSID_FIELD_NAME,etWifiSSID.getText().toString().trim());
                name = etWifiPass.getText().toString().trim();
                if(!name.equals(deviceData.get(DEVICE_WIFI_PASSWORD_FIELD_NAME)))
                    map.put(DEVICE_WIFI_PASSWORD_FIELD_NAME,etWifiPass.getText().toString().trim());
                if(map.size()>1)
                    globalData.uploadDeviceData(map, position);
                globalData.getGlobalUserData().removeObserver(userDataObserver);
                dismiss();
            }
        });



        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        globalData = new ViewModelProvider(requireActivity()).get(GlobalData.class);
        globalData.getIsInit().observe(requireActivity(),isInit->{
            if(isInit){
                globalData.getGlobalUserData().observe(requireActivity(),userDataObserver);
                globalData.getGlobalDeviceData().observe(requireActivity(),deviceData->{
                    deviceDataList = deviceData;
                    updateFields();
                });
            }
        });
    }

    private void updateFields() {
        for (DocumentSnapshot doc:deviceDataList){
            if(doc.getId().equals(deviceId))
                this.deviceData = doc;
        }
        etDeviceName.setText("" + this.deviceData.get(DEVICE_NAME_FIELD_NAME));
        etWifiSSID.setText("" + this.deviceData.get(Constants.DEVICE_WIFI_SSID_FIELD_NAME));
        etWifiPass.setText("" + this.deviceData.get(Constants.DEVICE_WIFI_PASSWORD_FIELD_NAME));
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Log.d("Settings","Item selected = " + i);
        deviceId = deviceList.get(i).get(getString(R.string.device_id));
        updateFields();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}