package com.example.mynirogscan;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavDirections;
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
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DevicesScreen#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DevicesScreen extends Fragment implements SettingsFragment.SettingsDialogListener, AdapterView.OnItemSelectedListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String TAG = "Device Screen";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private Spinner dropDown;

    ArrayList<Map<String,String>> device_list;
    ArrayList<String> devices = new ArrayList<>();
    String[] deviceArray = {"Device 1","Device 2","Device 3"};
    private Button addDeviceButton;
    private Button configureButton;

    DocumentSnapshot UserData;


    public DevicesScreen() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Devices_Screen.
     */
    // TODO: Rename and change types and number of parameters
    public static DevicesScreen newInstance(String param1, String param2) {
        DevicesScreen fragment = new DevicesScreen();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
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
                DialogFragment newFragment = new SettingsFragment(DevicesScreen.this);
                newFragment.show(getParentFragmentManager(), SettingsFragment.TAG);
            }
        });

        dropDown = view.findViewById(R.id.device_drop_down);
        // Specify the layout to use when the list of choices appears
        dropDown.setOnItemSelectedListener(this);

        // Create the instance of ArrayAdapter
        GlobalData globalData = new ViewModelProvider(requireActivity()).get(GlobalData.class);
        globalData.getGlobalUsersData().observe(requireActivity(), globalUserData->{
            UserData = globalUserData;
        });
        device_list = (ArrayList<Map<String, String>>) UserData.get("device_list");

        for(Map<String,String> device : device_list){
            devices.add(device.get("name"));
        }

        ArrayAdapter adapter = new ArrayAdapter(getContext(),
                android.R.layout.simple_spinner_item,
                devices);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        dropDown.setAdapter(adapter);

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        Bundle args = new Bundle();
        args.putString("DEVICE_ID", device_list.get(i).get("uuid"));
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