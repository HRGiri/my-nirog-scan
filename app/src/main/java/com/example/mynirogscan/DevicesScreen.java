package com.example.mynirogscan;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DevicesScreen#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DevicesScreen extends Fragment implements SettingsFragment.SettingsDialogListener, AdapterView.OnItemSelectedListener {

    private Spinner dropDown;

    ArrayList<String> deviceNames = new ArrayList<>();
    ArrayList<Map<String,String>> deviceList;
    String[] deviceArray = {"Device 1","Device 2","Device 3"};
    private String deviceId;
    private Button addDeviceButton;
    private Button configureButton;
    private GlobalData globalData;


    public DevicesScreen() {
        // Required empty public constructor
    }

    public static DevicesScreen newInstance() {
        return new DevicesScreen();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_devices_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        addDeviceButton = view.findViewById(R.id.add_device_btn);

        addDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DevicesScreenDirections.ActionDevicesScreenToAddDeviceFragment action = DevicesScreenDirections.actionDevicesScreenToAddDeviceFragment();
                //TODO: Set arguments
                action.setDEVICEID(null);
                action.setFCMToken(null);
                Navigation.findNavController(getActivity(),R.id.main_activity_nav_host)
                        .navigate(action);
            }
        });

        configureButton = view.findViewById(R.id.device_settings_btn);
        configureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialogFragment newFragment = new SettingsFragment(DevicesScreen.this, deviceId);
                newFragment.show(getParentFragmentManager(), SettingsFragment.TAG);
            }
        });

        dropDown = view.findViewById(R.id.device_drop_down);
        // Specify the layout to use when the list of choices appears
        dropDown.setOnItemSelectedListener(this);

        globalData = new ViewModelProvider(requireActivity()).get(GlobalData.class);
        globalData.getIsInit().observe(requireActivity(),isInit->{
            if(isInit){
                globalData.getGlobalUserData().observe(requireActivity(),userData->{
                    deviceList = (ArrayList<Map<String, String>>) userData.get(Constants.DEVICE_LIST_FIELD_NAME);
                    deviceNames = new ArrayList<>();
                    for(Map<String,String> map:deviceList){
                        deviceNames.add(map.get(Constants.NAME_FIELD_NAME));
                    }
                    if(deviceNames.size()>0) {
                        view.findViewById(R.id.devices_nav_host).setVisibility(View.VISIBLE);
                        // Create the instance of ArrayAdapter
                        ArrayAdapter adapter = new ArrayAdapter(requireContext(),
                                android.R.layout.simple_spinner_item,
                                deviceNames);

                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        // Apply the adapter to the spinner
                        dropDown.setAdapter(adapter);
                    }
                    else {
                        view.findViewById(R.id.devices_nav_host).setVisibility(View.GONE);
                    }
                });
            }
        });

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Bundle args = new Bundle();
        deviceId = deviceList.get(i).get("uuid");
        args.putString("DEVICE_ID",deviceList.get(i).get("uuid"));
//        Navigation.findNavController(getActivity(),R.id.devices_nav_host).setGraph(R.navigation.device_nav_graph,args);
//        Navigation
//                .findNavController(getActivity(),R.id.main_activity_nav_host)
//                .navigate(DevicesScreenDirections.actionDevicesScreenToDeviceNavGraph());
        NavHostFragment finalHost = NavHostFragment.create(R.navigation.device_nav_graph,args);
        getFragmentManager().beginTransaction()
                .replace(R.id.devices_nav_host, finalHost)
                .setPrimaryNavigationFragment(finalHost) // equivalent to app:defaultNavHost="true"
                .commit();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onSubmit(DialogFragment dialog) {
        SettingsFragment fragment = (SettingsFragment)dialog;
        Log.d("Settings",fragment.etWifiPass.getText().toString());
    }
}