package com.example.mynirogscan;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.DocumentsContract;
import android.text.style.TabStopSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Bytes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TimeZone;

import static android.app.Activity.RESULT_OK;
import static com.example.mynirogscan.Constants.CREATED_FIELD_NAME;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReadingsTableFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReadingsTableFragment extends Fragment {

    private static final String TAG = "Readings";
    private static final String DATE_PICKER_TAG = "com.example.mynirogscan.GenerateReportDatePicker";
    private RecyclerView recyclerView;
    private GlobalData globalData;
    private ArrayList<Map<String,Number>> readings = new ArrayList<>();
    private Button generateCSVButton;
    String fileName;

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
        String deviceId = ReadingsTableFragmentArgs.fromBundle(getArguments()).getDEVICEID();
        generateCSVButton = view.findViewById(R.id.btn_generate_csv);
        generateCSVButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchDatePicker();
            }
        });

        globalData = new ViewModelProvider(requireActivity()).get(GlobalData.class);
        globalData.getIsInit().observe(requireActivity(),isInit->{
            if(isInit){
                if(deviceId == null) {
                    globalData.getAllReadingsSorted().observe(requireActivity(), sortedReadings -> {
                        readings = new ArrayList<>();
                        for (Number timestamp : sortedReadings.keySet()) {
                            Map<String, Number> map = new HashMap<>();
                            map.put("timestamp", timestamp);
                            map.putAll(sortedReadings.get(timestamp));
                            readings.add(map);
                        }
                        ReadingsAdapter readingsAdapter = new ReadingsAdapter(readings);
                        recyclerView.setAdapter(readingsAdapter);
                    });
                }else{
                    globalData.getAllReadingsSorted().observe(requireActivity(), sortedReadings -> {
                        Map<Number, Map<String, Number>> devieSortedReadings = globalData.getDeviceReadingsSorted(deviceId);
                        readings = new ArrayList<>();
                        for (Number timestamp : devieSortedReadings.keySet()) {
                            Map<String, Number> map = new HashMap<>();
                            map.put("timestamp", timestamp);
                            map.putAll(sortedReadings.get(timestamp));
                            readings.add(map);
                        }
                        ReadingsAdapter readingsAdapter = new ReadingsAdapter(readings);
                        recyclerView.setAdapter(readingsAdapter);
                    });
                }
            }
        });
    }

    private void launchDatePicker() {
        MaterialDatePicker<Pair<Long, Long>> dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select dates")
                .setSelection(new Pair<>(
                                MaterialDatePicker.thisMonthInUtcMilliseconds(),
                                MaterialDatePicker.todayInUtcMilliseconds()
                        )
                )
                .build();
        dateRangePicker.addOnPositiveButtonClickListener((MaterialPickerOnPositiveButtonClickListener<Pair<Long,Long>>) selection -> {


            globalData.getGlobalDeviceReadings().observe(requireActivity(),deviceReadings->{
                // Check if the timestamp exists in current document
                boolean toFetch = false;
                for(DocumentSnapshot curr_doc : deviceReadings){
                    Long created = Long.decode(String.valueOf(curr_doc.get(CREATED_FIELD_NAME)));
                    if (created > selection.first){
                        toFetch = true;
                    }
                }
                if(toFetch){
                    // Fetch readings matching the time period
                    getDateRangeReadings();
                    Log.d(TAG,"Need to fetch more data");
                }
                try {
                    generateCSV();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
        dateRangePicker.show(getParentFragmentManager(),DATE_PICKER_TAG);
    }

    private void getDateRangeReadings() {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        DocumentReference user_document_ref = firestore.collection("users")
                .document(currentUser.getUid());
//        user_document_ref.collection(READINGS_DOCUMENT_NAME)
//                .whereGreaterThan(CREATED_FIELD_NAME,)
    }


    private void generateCSV() throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy HH:mm");
        Date date = new Date((long) Calendar.getInstance().getTimeInMillis());
        fileName = "readings" +date + ".csv";
        createFile(getContext().getFilesDir().toURI(),fileName);


    }

    private String generateCsvContent(ArrayList<Map<String,Number>> readingsList){
        String content = "";
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy HH:mm");
        StringJoiner stringJoiner = new StringJoiner(",");
        stringJoiner.add("Timestamp");
        stringJoiner.add("uuid");
        stringJoiner.add("temperature");
        stringJoiner.add("oxygen");
        stringJoiner.add("heartrate");
        content += stringJoiner.toString() + "\n";
        for(Map<String,Number> map:readingsList){
            stringJoiner = new StringJoiner(",");
            Date date = new Date((long) map.get("timestamp"));
            sdf.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));
            stringJoiner.add("" + sdf.format(date));
            stringJoiner.add("" + map.get("uuid"));
            stringJoiner.add("" + map.get("temperature"));
            stringJoiner.add("" + map.get("oxygen"));
            stringJoiner.add("" + map.get("heartrate"));
            content += stringJoiner.toString() + "\n";
        }
        return content;
    }

    // Request code for creating a CSV document.
    private static final int CREATE_FILE = 1;

    private void createFile(URI pickerInitialUri, String filename) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, filename);

        // Optionally, specify a URI for the directory that should be opened in
        // the system file picker when your app creates the document.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, CREATE_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case CREATE_FILE:
                if(resultCode == RESULT_OK){
                    try {
                        String content = generateCsvContent(readings);
                        Log.d(TAG,content);
                        FileDescriptor fd = requireActivity().getContentResolver().openFileDescriptor(data.getData(),"w").getFileDescriptor();
                        FileOutputStream fos = new FileOutputStream(fd);
                        fos.write((content).getBytes(StandardCharsets.UTF_8));
                        fos.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
        }
    }

    // Request code for selecting a PDF document.
    private static final int PICK_PDF_FILE = 2;

    private void openFile(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);

        startActivityForResult(intent, PICK_PDF_FILE);
    }
}