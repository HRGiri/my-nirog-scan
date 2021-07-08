package com.example.mynirogscan;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends DialogFragment implements AdapterView.OnItemSelectedListener {

    public static final String TAG = "com.example.mynirogscan.SettingsDialog";
    EditText etDeviceName;
    EditText etWifiSSID;
    public EditText etWifiPass;
    Spinner deviceSelector;
    Button submitButton;

    ArrayList<String> deviceList = new ArrayList<>();
    String[] deviceArray = {"Device 1","Device 2","Device 3"};

    public interface SettingsDialogListener {
        public void onSubmit(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    SettingsDialogListener listener;

    public SettingsFragment() {
        // Required empty public constructor

    }

    public SettingsFragment(Fragment fragment){
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

        // Create the instance of ArrayAdapter
        deviceList.addAll(Arrays.asList(deviceArray));
        ArrayAdapter adapter = new ArrayAdapter(getContext(),
                android.R.layout.simple_spinner_item,
                deviceList);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        deviceSelector.setAdapter(adapter);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: upload details
                listener.onSubmit(SettingsFragment.this);
                dismiss();
            }
        });

        return view;
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        //TODO: Update details
        Log.d("Settings","Item selected = " + i);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }
}