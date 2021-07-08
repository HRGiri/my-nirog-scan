package com.example.mynirogscan;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import static android.app.Activity.RESULT_OK;
import static com.example.mynirogscan.Constants.CREATED_FIELD_NAME;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    public static final String FCM_TOKEN_EXTRA = "com.example.mynirogscan.FCM_TOKEN";
    private static final String TAG = "MAINPAGE";
    public static final String FIREBASE_TAG = "Firebase";
    private static final int RC_SIGN_IN = 1;
    private static final String DATE_PICKER_TAG = "com.example.mynirogscan.GenerateReportDatePicker";
    public static final String DEVICE_ID_EXTRA = "com.example.mynirogscan.DEVICE_ID";
    private String token;
    private TextView info;
    private TextView total_visits_value;
    private TextView oxygen_value;
    private TextView heartrate_value;
    private TextView temperature_value;
    private Button generateCSVButton;
    private LineChart oxygen_reading_chart;
    private LineChart temperature_reading_chart;
    private LineChart heartrate_reading_chart;
    private PieChart company_health_chart;
    private PieChart oxygen_pie_chart;
    private PieChart temperature_pie_chart;
    private PieChart heartrate_pie_chart;
    private BarChart daily_visit_chart;
    public FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private ListenerRegistration registration;
    private DocumentSnapshot DocumentData;
    private List<DocumentSnapshot> DeviceData;
    private List<DocumentSnapshot> DeviceReadings;
    ListenerRegistration ReadingListeners;
    ListenerRegistration DeviceDataListeners;
    ListenerRegistration DocumentDataListeners;
    Map<Number, Map<String,Number>> all_readings_sorted;
    Float counter;   //Revisit after testing
    int total_reads;

    private PieChartStateAdapter pieChartAdapter;
    private ViewPager2 viewPager;


    Charts chart = new Charts();
    private GlobalData globalData;


    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance() {
        return new HomeFragment();
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

        mAuth = FirebaseAuth.getInstance();
//        mAuth.useEmulator("10.0.2.2", 9099);

//        firestore = FirebaseFirestore.getInstance();
//        firestore.enableNetwork();
//        emulatorSettings();


        info = view.findViewById(R.id.tv_main);

        generateCSVButton = view.findViewById(R.id.generate_csv_btn);
        generateCSVButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchDatePicker();
            }
        });

        total_visits_value = view.findViewById(R.id.total_visitors_card);
        oxygen_value = view.findViewById(R.id.oxygen_card);
        temperature_value = view.findViewById(R.id.temperature_card);
        heartrate_value = view.findViewById(R.id.heartrate_card);

        /*Daily visit chart*/
        daily_visit_chart = view.findViewById(R.id.visit_chart);

        /*Reading Chart settings*/
        oxygen_reading_chart = view.findViewById(R.id.oxygen_reading_history_chart);
        temperature_reading_chart = view.findViewById(R.id.temperature_reading_history_chart);
        heartrate_reading_chart = view.findViewById(R.id.heartrate_reading_history_chart);

        //Reading History chart settings
        chart.setupLineChart(oxygen_reading_chart,Color.TRANSPARENT,11f,android.R.color.primary_text_dark,100f,75f);
        chart.setupLineChart(temperature_reading_chart,Color.TRANSPARENT,11f,android.R.color.holo_purple,110f,90f);
        chart.setupLineChart(heartrate_reading_chart,Color.TRANSPARENT,11f,android.R.color.holo_orange_light,180f,20f);


        pieChartAdapter = new PieChartStateAdapter(this);
        viewPager = view.findViewById(R.id.pieChartPager);
        viewPager.setAdapter(pieChartAdapter);
        viewPager.setOffscreenPageLimit(2);
        //############## Pie Chart settings
        company_health_chart = view.findViewById(R.id.company_health_chart);
        oxygen_pie_chart = view.findViewById(R.id.oxygen_chart);
        temperature_pie_chart = view.findViewById(R.id.temperature_chart);
        heartrate_pie_chart = view.findViewById(R.id.heartrate_chart);

        chart.setupPieChart(company_health_chart, android.R.color.darker_gray);
        chart.setupPieChart(oxygen_pie_chart, android.R.color.darker_gray);
        chart.setupPieChart(temperature_pie_chart, android.R.color.darker_gray);
        chart.setupPieChart(heartrate_pie_chart, android.R.color.darker_gray);
        //##############Bar chart Settings
        daily_visit_chart = view.findViewById(R.id.visit_chart);
        chart.setupBarChart(daily_visit_chart);
        // Check if user is signed in (non-null) and update UI accordingly.
        currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            //TODO: Proceed to SignIn Menu
//            startActivity(new Intent(getApplicationContext(),LoginActivity.class));
            // Choose authentication providers
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().enableEmailLinkSignIn()
                            .setActionCodeSettings(buildActionCodeSettings()).build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build());


            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);

        }
        else {
//            update_data();
            globalData = new ViewModelProvider(requireActivity()).get(GlobalData.class);
            globalData.init();
            globalData.getIsInit().observe(requireActivity(),isInit->{
                if(isInit){
                    globalData.getGlobalUsersData().observe(requireActivity(),usersData->{
                        DocumentData = usersData;
                    });
                    globalData.getGlobalDeviceData().observe(requireActivity(),  viewDeviceData -> {
                        DeviceData = viewDeviceData;
                    });
                    globalData.getGlobalDeviceReadings().observe(requireActivity(), viewDeviceReadings -> {
                        DeviceReadings = viewDeviceReadings;
                    });
                    globalData.getAllReadingsSorted().observe(requireActivity(),sortedReadings->{
                        all_readings_sorted = sortedReadings;
                        update_top_table();
                        populate_reading_history_chart();
                        populate_company_health_chart();
                        populate_daily_visit_chart();
                    });
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(currentUser != null) {
            info.setText("Hello " + currentUser.getDisplayName());

            Log.d(TAG,"Got the current user : "+ currentUser.getDisplayName());
        } else {
            info.setText("PLease wait... " );  //           Add animation
            currentUser = mAuth.getCurrentUser();
            if(currentUser != null){
                info.setText("Successfully Logged in");
                info.setText("Hello " + currentUser.getDisplayName());
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
//        if(registration != null)                  //To be moved to onpause()
//            registration.remove();
////        if(DocumentDataListeners != null)       //Might not be required.
////            DocumentDataListeners.remove();
//        if(DeviceDataListeners != null)
//            DeviceDataListeners.remove();
//        if(ReadingListeners != null)
//            ReadingListeners.remove();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                firestore.collection("users").document(user.getUid())
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()) {
                                    if (task.getResult().exists()) {
                                        Log.d(TAG, "Google Found it");
                                        createFCMtoken();

                                    } else {
                                        Log.d(TAG, "Google sign In Lets register you");
//                                        Intent intent = new Intent(getContext(),
//                                                SignUpActivity.class);
//                                        startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                        HomeFragmentDirections.ActionHomeFragmentToSignUpFragment action =
                                                HomeFragmentDirections
                                                        .actionHomeFragmentToSignUpFragment();
                                        action.setIsDeepLink(false);
                                        Navigation.findNavController(getActivity(),R.id.main_activity_nav_host).navigate(action);
                                    }
                                } else  {
                                    Log.d(TAG,"Error getting Data : ", task.getException());
                                }
                            }
                        });
                Log.d(TAG,user.getEmail());
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                Log.d(TAG,"Sign in failed");
            }
        }
    }

    private void createFCMtoken(){
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        token = task.getResult();
                        Log.d(TAG,"Token created"+token);
                        updateFCMToken();
                    }
                });
    }

    //Update FCM Token in DataBase.
    private void updateFCMToken(){
        Map<String, String> user = new HashMap<>();
        user.put("FCMToken", token);
        Log.d(TAG, "FCM token s! + "+user.toString());
        FirebaseUser curruser = FirebaseAuth.getInstance().getCurrentUser();
        firestore.collection("users").document(curruser.getUid())
                .set(user, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "FCM token updated! + "+token);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

    }



    public ActionCodeSettings buildActionCodeSettings() {
        // [START auth_build_action_code_settings]
        ActionCodeSettings actionCodeSettings =
                ActionCodeSettings.newBuilder()
                        // URL you want to redirect back to. The domain (www.example.com) for this
                        // URL must be whitelisted in the Firebase Console.
                        .setUrl("https://nirogindia.net")
                        // This must be true
                        .setHandleCodeInApp(true)
                        .setAndroidPackageName(
                                "com.example.mynirogscan",
                                true, /* installIfNotAvailable */
                                "12"    /* minimumVersion */)
                        .build();
        // [END auth_build_action_code_settings]
        return actionCodeSettings;
    }


    public void emulatorSettings() {
        // [START fs_emulator_connect]
        // 10.0.2.2 is the special IP address to connect to the 'localhost' of
        // the host computer from an Android emulator.
        firestore = FirebaseFirestore.getInstance();
        firestore.useEmulator("10.0.2.2", 8080);

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
//                .setPersistenceEnabled(false)
                .build();
        firestore.setFirestoreSettings(settings);
        // [END fs_emulator_connect]
    }


    private void update_top_table(){
        /* Update Total Read, Last readings */
        //Assumption : all_reading_sorted contains only the latest readings
        total_reads = all_readings_sorted.size();
        Number last_read_timestamp = (Number)all_readings_sorted.keySet().toArray()[0];
        total_visits_value.setText(String.format("%d",total_reads));
        oxygen_value.setText(String.format("%.1f %%",all_readings_sorted.get(last_read_timestamp).get("oxygen").floatValue()));
        temperature_value.setText(String.format("%.1f \u00B0 F",all_readings_sorted.get(last_read_timestamp).get("temperature").floatValue()));
        heartrate_value.setText(String.format("%d bpm",all_readings_sorted.get(last_read_timestamp).get("heartrate").intValue()));

    }

    private void populate_company_health_chart(){

        Charts piechart_obj = new Charts();

        piechart_obj.calculate_healthy_people(all_readings_sorted);


        ArrayList<PieEntry> pie_comp_health_chart_entries = new ArrayList<>();
        pie_comp_health_chart_entries.add(new PieEntry(((float)(piechart_obj.green_band)/total_reads)*100f,"Healthy"));
        pie_comp_health_chart_entries.add(new PieEntry(((float)(piechart_obj.yellow_band)/total_reads)*100f,"Unfit"));
        pie_comp_health_chart_entries.add(new PieEntry(((float)(piechart_obj.orange_band)/total_reads)*100f,"Ill"));
        pie_comp_health_chart_entries.add(new PieEntry(((float)(piechart_obj.red_band)/total_reads)*100f,"Dead"));

        ArrayList<PieEntry> pie_oxy_chart_entries = new ArrayList<>();
        pie_oxy_chart_entries.add(new PieEntry(((float)(total_reads - piechart_obj.bad_oxy_count)/total_reads)*100f,"Healthy"));
        pie_oxy_chart_entries.add(new PieEntry(((float)(piechart_obj.bad_oxy_count)/total_reads)*100f," Unhealthy"));

        ArrayList<PieEntry> pie_temp_chart_entries = new ArrayList<>();
        pie_temp_chart_entries.add(new PieEntry(((float)(total_reads - piechart_obj.bad_temp_count)/total_reads)*100f,"Healthy"));
        pie_temp_chart_entries.add(new PieEntry(((float)(piechart_obj.bad_temp_count)/total_reads)*100f," Unhealthy"));

        ArrayList<PieEntry> pie_hr_chart_entries = new ArrayList<>();
        pie_hr_chart_entries.add(new PieEntry(((float)(total_reads - piechart_obj.bad_hr_count)/total_reads)*100f,"Healthy"));
        pie_hr_chart_entries.add(new PieEntry(((float)(piechart_obj.bad_hr_count)/total_reads)*100f," Unhealthy"));

        piechart_obj.populatepiechart(heartrate_pie_chart,pie_hr_chart_entries,"Heart Rate", android.R.color.black);
        piechart_obj.populatepiechart(temperature_pie_chart,pie_temp_chart_entries,"Temperature", android.R.color.black);
        piechart_obj.populatepiechart(oxygen_pie_chart,pie_oxy_chart_entries,"Oxygen", android.R.color.black);
        piechart_obj.populatepiechart(company_health_chart,pie_comp_health_chart_entries,"Company Health", android.R.color.black);
    }

    private void populate_reading_history_chart(){

        Charts linechart_obj = new Charts();
        List<Entry> oxygen_entries = new ArrayList<Entry>();
        List<Entry> temperature_entries = new ArrayList<Entry>();
        List<Entry> heartrate_entries = new ArrayList<Entry>();
        linechart_obj.extract_reading_history(all_readings_sorted,oxygen_entries,temperature_entries,heartrate_entries);

        linechart_obj.populatelinechart(oxygen_reading_chart,oxygen_entries,"Oxygen",Color.CYAN,Color.BLACK);
        linechart_obj.populatelinechart(temperature_reading_chart,temperature_entries,"Temperature",Color.BLUE,Color.BLACK);
        linechart_obj.populatelinechart(heartrate_reading_chart,heartrate_entries,"Heart Rate",Color.MAGENTA,Color.BLACK);
    }

    public void populate_daily_visit_chart(){

        Charts bar_chart_obj = new Charts();
        Map<String, float[]> bar_entry = bar_chart_obj.calculate_daily_healthy_people(all_readings_sorted);
        List<BarEntry> entries = new ArrayList<>();
        float count = 0f;
        for(String day : bar_entry.keySet()){
            entries.add(new BarEntry(count,bar_entry.get(day)));
            count = count + 1f;
        }
        bar_chart_obj.populateBarChart(daily_visit_chart,entries,"Daily Record");
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
            Log.d(TAG,selection.toString());

            // Check if the timestamp exists in current document
            boolean toFetch = false;
            for(DocumentSnapshot curr_doc : DeviceReadings){
                Long created = Long.decode(String.valueOf(curr_doc.get(CREATED_FIELD_NAME)));
                if (created > selection.first){
                    toFetch = true;
                }
            }
            if(toFetch){
                // Fetch readings matching the time period
                getDateRangeReadings();
            }
            generateCSV();

        });
        dateRangePicker.show(getParentFragmentManager(),DATE_PICKER_TAG);
    }

    private void getDateRangeReadings() {
        DocumentReference user_document_ref = firestore.collection("users")
                .document(currentUser.getUid());
//        user_document_ref.collection(READINGS_DOCUMENT_NAME)
//                .whereGreaterThan(CREATED_FIELD_NAME,)
    }

    private void generateCSV() {
        // TODO: Implement csv generation
    }
    /**
     * Method to add a new device
     */
    private void addDevice(){
        String deviceId = generateDeviceId();
        Log.d(TAG,deviceId);
        String fcmToken = DocumentData.get("FCMToken").toString();
        Log.d(TAG,fcmToken.length() + "");
//        FragmentManager fragmentManager = getParentFragmentManager();
//        Bundle bundle = new Bundle();
//        bundle.putString(FCM_TOKEN_EXTRA,fcmToken);
//        bundle.putString(DEVICE_ID_EXTRA,deviceId);
//        fragmentManager.beginTransaction()
//                .setReorderingAllowed(true)
//                .add(R.id.fragmentContainerView2, AddDeviceFragment.class, bundle)
//                .commit();
//        Intent intent = new Intent(MainActivity.this,AddDeviceActivity.class);
//        intent.putExtra(FCM_TOKEN_EXTRA,fcmToken);
//        intent.putExtra(DEVICE_ID_EXTRA,deviceId);
//        startActivity(intent);
    }

    /**
     * Method to generate a new id
     */
    private String generateDeviceId(){
        ArrayList<Map<String,String>> deviceList = (ArrayList<Map<String, String>>) DocumentData.get("device_list");
        if(deviceList == null){
            return currentUser.getUid() + "-01";
        }
        else {
            Log.d(TAG, String.valueOf(deviceList.size()));

            return String.format(currentUser.getUid() + "-%02d",deviceList.size() + 1);
        }
    }
}