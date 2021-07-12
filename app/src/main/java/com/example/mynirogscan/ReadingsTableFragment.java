package com.example.mynirogscan;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReadingsTableFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReadingsTableFragment extends Fragment {

    private RecyclerView recyclerView;
    private GlobalData globalData;
    private ArrayList<Map<String,Number>> readings = new ArrayList<>();

    public ReadingsTableFragment() {
        // Required empty public constructor
    }

    public static ReadingsTableFragment newInstance() {
        ReadingsTableFragment fragment = new ReadingsTableFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_readings_table, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.readings_rec_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        globalData = new ViewModelProvider(requireActivity()).get(GlobalData.class);
        globalData.getIsInit().observe(requireActivity(),isInit->{
            if(isInit){
                globalData.getAllReadingsSorted().observe(requireActivity(),sortedReadings->{
                    if(readings.size() == 0) {
                        for (Number timestamp : sortedReadings.keySet()) {
                            Map<String, Number> map = new HashMap<>();
                            map.put("timestamp", timestamp);
                            map.putAll(sortedReadings.get(timestamp));
                            readings.add(map);
                        }
                        ReadingsAdapter readingsAdapter = new ReadingsAdapter(readings);
                        recyclerView.setAdapter(readingsAdapter);
                    }
                });
            }
        });
    }
}