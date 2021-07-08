package com.example.mynirogscan;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public class DeviceFragment extends Fragment {

    private GlobalData globalData;
    private List<DocumentSnapshot> AllDeviceData;
    private DocumentSnapshot DeviceData;
    private List<DocumentSnapshot> AllDeviceReadings;
    private DocumentSnapshot DeviceReadings;

    String TAG = "DeviceFragment";

    public DeviceFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DeviceFragment newInstance() {
        return new DeviceFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String deviceId = DeviceFragmentArgs.fromBundle(getArguments()).getDEVICEID();
        globalData = new ViewModelProvider(requireActivity()).get(GlobalData.class);
        globalData.getIsInit().observe(requireActivity(),isInit->{
            if(isInit){

                globalData.getGlobalDeviceData().observe(requireActivity(),  viewDeviceData -> {
                    AllDeviceData = viewDeviceData;
                    for(DocumentSnapshot DeviceDataIter: AllDeviceData){
                        if(deviceId.equals(DeviceDataIter.getId())){
                            DeviceData = DeviceDataIter;
                        }
                    }

                });
                globalData.getGlobalDeviceReadings().observe(requireActivity(), viewDeviceReadings -> {
                    AllDeviceReadings = viewDeviceReadings;
                    for(DocumentSnapshot DeviceReadingsIter: AllDeviceReadings){
                        if(deviceId.equals(DeviceReadingsIter.get("uuid"))){
                            DeviceReadings = DeviceReadingsIter;
                            Log.d(TAG,"Device readings"+DeviceReadings.toString());
                        }
                    }
                });
            }
        });
    }


}
