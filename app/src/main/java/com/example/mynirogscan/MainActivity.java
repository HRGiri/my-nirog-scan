    package com.example.mynirogscan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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
import java.util.TimeZone;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    public static final String ADD_DEVICE_EXTRA = "com.example.mynirogscan.FCM_TOKEN";
    private static final String TAG = "MAINPAGE";
    public static final String FIREBASE_TAG = "Firebase";
    private static final int RC_SIGN_IN = 1;
    private String token;
    private TextView info;
    private TextView total_visits_value;
    private TextView oxygen_value;
    private TextView heartrate_value;
    private TextView temperature_value;
    private LineChart complete_reading_chart;
    private LineChart oxygen_reading_chart;
    private LineChart temperature_reading_chart;
    private LineChart heartrate_reading_chart;
    private PieChart company_health_chart;
    private PieChart oxygen_pie_chart;
    private PieChart temperature_pie_chart;
    private PieChart heartrate_pie_chart;
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
    Map<Number,Map<String,Number>> all_readings_sorted;
    Float counter;   //Revisit after testing
    XAxis xAxis;
    XAxis oxygen_xAxis;
    XAxis temperature_xAxis;
    XAxis heartrate_xAxis;
    int total_reads;

    final float MIN_OXY_LIM = 93f;
    final float MIN_HEARTRATE = 40f;
    final float MAX_HEARTRATE = 100f;
    final float MAX_TEMPERATURE = 99f;
    final float MIN_TEMPERATURE = 97f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
//        mAuth.useEmulator("10.0.2.2", 9099);

        firestore = FirebaseFirestore.getInstance();
        firestore.enableNetwork();
//        emulatorSettings();

        info = findViewById(R.id.tv_main);

        total_visits_value = findViewById(R.id.total_visitors_card);
        oxygen_value = findViewById(R.id.oxygen_card);
        temperature_value = findViewById(R.id.temperature_card);
        heartrate_value = findViewById(R.id.heartrate_card);

        /*Reading Chart settings*/
        complete_reading_chart = findViewById(R.id.complete_reading_history_chart);
        oxygen_reading_chart = findViewById(R.id.oxygen_reading_history_chart);
        temperature_reading_chart = findViewById(R.id.temperature_reading_history_chart);
        heartrate_reading_chart = findViewById(R.id.heartrate_reading_history_chart);

        //Complete Reading History chart settings
        // enable scaling and dragging
        complete_reading_chart.setDragEnabled(true);
        complete_reading_chart.setScaleEnabled(true);
        complete_reading_chart.setDrawGridBackground(false);
        complete_reading_chart.setHighlightPerDragEnabled(true);
        complete_reading_chart.setPinchZoom(true);
        complete_reading_chart.setBackgroundColor(Color.TRANSPARENT);
        xAxis = complete_reading_chart.getXAxis();
        complete_reading_chart.animateX(1500);


        xAxis.setTextSize(11f);
        xAxis.setTextColor(Color.MAGENTA);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);

        YAxis leftAxis = complete_reading_chart.getAxisLeft();
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setAxisMaximum(120f);
        leftAxis.setAxisMinimum(90f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(true);

        YAxis rightAxis = complete_reading_chart.getAxisRight();
        rightAxis.setTextColor(Color.RED);
        rightAxis.setAxisMaximum(200f);
        rightAxis.setAxisMinimum(20f);
        rightAxis.setDrawGridLines(false);
        rightAxis.setDrawZeroLine(false);
        rightAxis.setGranularityEnabled(true);

        //Oxygen Reading History chart settings
        // enable scaling and dragging
        oxygen_reading_chart.setDragEnabled(true);
        oxygen_reading_chart.setScaleEnabled(true);
        oxygen_reading_chart.setDrawGridBackground(false);
        oxygen_reading_chart.setHighlightPerDragEnabled(true);
        oxygen_reading_chart.setPinchZoom(true);
        oxygen_reading_chart.setBackgroundColor(Color.TRANSPARENT);
        oxygen_xAxis = oxygen_reading_chart.getXAxis();
        oxygen_reading_chart.animateX(1500);


        oxygen_xAxis.setTextSize(11f);
        oxygen_xAxis.setTextColor(Color.MAGENTA);
        oxygen_xAxis.setDrawGridLines(false);
        oxygen_xAxis.setDrawAxisLine(false);

        YAxis OxyleftAxis = oxygen_reading_chart.getAxisLeft();
        OxyleftAxis.setTextColor(ColorTemplate.getHoloBlue());
        OxyleftAxis.setAxisMaximum(100f);
        OxyleftAxis.setAxisMinimum(75f);
        OxyleftAxis.setDrawGridLines(true);
        OxyleftAxis.setGranularityEnabled(true);


        //Temperature Reading History chart settings
        // enable scaling and dragging
        temperature_reading_chart.setDragEnabled(true);
        temperature_reading_chart.setScaleEnabled(true);
        temperature_reading_chart.setDrawGridBackground(false);
        temperature_reading_chart.setHighlightPerDragEnabled(true);
        temperature_reading_chart.setPinchZoom(true);
        temperature_reading_chart.setBackgroundColor(Color.TRANSPARENT);
        temperature_xAxis = temperature_reading_chart.getXAxis();
        temperature_reading_chart.animateX(1500);


        temperature_xAxis.setTextSize(11f);
        temperature_xAxis.setTextColor(Color.MAGENTA);
        temperature_xAxis.setDrawGridLines(false);
        temperature_xAxis.setDrawAxisLine(false);

        YAxis templeftAxis = temperature_reading_chart.getAxisLeft();
        templeftAxis.setTextColor(ColorTemplate.getHoloBlue());
        templeftAxis.setAxisMaximum(120f);
        templeftAxis.setAxisMinimum(90f);
        templeftAxis.setDrawGridLines(true);
        templeftAxis.setGranularityEnabled(true);

        //Heartrate Reading History chart settings
        // enable scaling and dragging
        heartrate_reading_chart.setDragEnabled(true);
        heartrate_reading_chart.setScaleEnabled(true);
        heartrate_reading_chart.setDrawGridBackground(false);
        heartrate_reading_chart.setHighlightPerDragEnabled(true);
        heartrate_reading_chart.setPinchZoom(true);
        heartrate_reading_chart.setBackgroundColor(Color.TRANSPARENT);
        heartrate_xAxis = heartrate_reading_chart.getXAxis();
        heartrate_reading_chart.animateX(1500);


        heartrate_xAxis.setTextSize(11f);
        heartrate_xAxis.setTextColor(Color.MAGENTA);
        heartrate_xAxis.setDrawGridLines(false);
        heartrate_xAxis.setDrawAxisLine(false);

        YAxis hrleftAxis = heartrate_reading_chart.getAxisLeft();
        hrleftAxis.setTextColor(ColorTemplate.getHoloBlue());
        hrleftAxis.setAxisMaximum(200f);
        hrleftAxis.setAxisMinimum(20f);
        hrleftAxis.setDrawGridLines(true);
        hrleftAxis.setGranularityEnabled(true);


        //############## Pie Chart settings
        //########## Company Health Chart
        company_health_chart = findViewById(R.id.company_health_chart);

        company_health_chart.setUsePercentValues(true);
        company_health_chart.getDescription().setEnabled(false);
        company_health_chart.setExtraOffsets(5, 5, 5, 5);


        company_health_chart.setDragDecelerationFrictionCoef(0.95f);

        company_health_chart.setDrawHoleEnabled(true);
        company_health_chart.setHoleColor(Color.WHITE);

        company_health_chart.setTransparentCircleColor(Color.WHITE);
        company_health_chart.setTransparentCircleAlpha(110);

        company_health_chart.setHoleRadius(58f);
        company_health_chart.setTransparentCircleRadius(61f);

        company_health_chart.setDrawCenterText(true);

        company_health_chart.setRotationAngle(0);
        // enable rotation of the chart by touch
        company_health_chart.setRotationEnabled(true);
        company_health_chart.setHighlightPerTapEnabled(true);


        company_health_chart.animateY(1400, Easing.EaseInOutQuad);

        Legend l = company_health_chart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);

        // entry label styling
        company_health_chart.setEntryLabelColor(Color.WHITE);
        company_health_chart.setEntryLabelTextSize(12f);

        //##### Oxygen Chart

        oxygen_pie_chart = findViewById(R.id.oxygen_chart);
        oxygen_pie_chart.setUsePercentValues(true);
        oxygen_pie_chart.getDescription().setEnabled(false);
        oxygen_pie_chart.setExtraOffsets(5, 5, 5, 5);
        oxygen_pie_chart.setDragDecelerationFrictionCoef(0.95f);
        oxygen_pie_chart.setDrawHoleEnabled(true);
        oxygen_pie_chart.setHoleColor(Color.WHITE);
        oxygen_pie_chart.setTransparentCircleColor(Color.WHITE);
        oxygen_pie_chart.setTransparentCircleAlpha(110);
        oxygen_pie_chart.setHoleRadius(58f);
        oxygen_pie_chart.setTransparentCircleRadius(61f);
        oxygen_pie_chart.setDrawCenterText(true);
        oxygen_pie_chart.setRotationAngle(0);
        // enable rotation of the chart by touch
        oxygen_pie_chart.setRotationEnabled(true);
        oxygen_pie_chart.setHighlightPerTapEnabled(true);
        oxygen_pie_chart.animateY(1400, Easing.EaseInOutQuad);
        Legend oxygen_legend = oxygen_pie_chart.getLegend();
        oxygen_legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        oxygen_legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        oxygen_legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        oxygen_legend.setDrawInside(false);
        oxygen_legend.setXEntrySpace(7f);
        oxygen_legend.setYEntrySpace(0f);
        oxygen_legend.setYOffset(0f);


        //##### Temperature Chart

        temperature_pie_chart = findViewById(R.id.temperature_chart);
        temperature_pie_chart.setUsePercentValues(true);
        temperature_pie_chart.getDescription().setEnabled(false);
        temperature_pie_chart.setExtraOffsets(5, 5, 5, 5);
        temperature_pie_chart.setDragDecelerationFrictionCoef(0.95f);
        temperature_pie_chart.setDrawHoleEnabled(true);
        temperature_pie_chart.setHoleColor(Color.TRANSPARENT);
        temperature_pie_chart.setTransparentCircleColor(Color.BLUE);
        temperature_pie_chart.setTransparentCircleAlpha(110);
        temperature_pie_chart.setHoleRadius(58f);
        temperature_pie_chart.setTransparentCircleRadius(61f);
        temperature_pie_chart.setDrawCenterText(true);
        temperature_pie_chart.setRotationAngle(0);
        // enable rotation of the chart by touch
        temperature_pie_chart.setRotationEnabled(true);
        temperature_pie_chart.setHighlightPerTapEnabled(true);
        temperature_pie_chart.animateY(1400, Easing.EaseInOutQuad);
        Legend temperature_legend = temperature_pie_chart.getLegend();
        temperature_legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        temperature_legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        temperature_legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        temperature_legend.setDrawInside(false);
        temperature_legend.setXEntrySpace(7f);
        temperature_legend.setYEntrySpace(0f);
        temperature_legend.setYOffset(0f);

        // entry label styling
        temperature_pie_chart.setEntryLabelColor(Color.WHITE);
        temperature_pie_chart.setEntryLabelTextSize(12f);

        //##### Heartrate Chart

        heartrate_pie_chart = findViewById(R.id.heartrate_chart);
        heartrate_pie_chart.setUsePercentValues(true);
        heartrate_pie_chart.getDescription().setEnabled(false);
        heartrate_pie_chart.setExtraOffsets(5, 5, 5, 5);
        heartrate_pie_chart.setDragDecelerationFrictionCoef(0.95f);
        heartrate_pie_chart.setDrawHoleEnabled(true);
        heartrate_pie_chart.setHoleColor(Color.WHITE);
        heartrate_pie_chart.setTransparentCircleColor(Color.WHITE);
        heartrate_pie_chart.setTransparentCircleAlpha(110);
        heartrate_pie_chart.setHoleRadius(58f);
        heartrate_pie_chart.setTransparentCircleRadius(61f);
        heartrate_pie_chart.setDrawCenterText(true);
        heartrate_pie_chart.setRotationAngle(0);
        // enable rotation of the chart by touch
        heartrate_pie_chart.setRotationEnabled(true);
        heartrate_pie_chart.setHighlightPerTapEnabled(true);
        heartrate_pie_chart.animateY(1400, Easing.EaseInOutQuad);
        Legend heartrate_legend = heartrate_pie_chart.getLegend();
        heartrate_legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        heartrate_legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        heartrate_legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        heartrate_legend.setDrawInside(false);
        heartrate_legend.setXEntrySpace(7f);
        heartrate_legend.setYEntrySpace(0f);
        heartrate_legend.setYOffset(0f);

        // entry label styling
        heartrate_pie_chart.setEntryLabelColor(Color.BLACK);
        heartrate_pie_chart.setEntryLabelTextSize(12f);


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
            update_data();

        }

    }

    @Override
    protected void onStart() {
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
    protected void onStop() {
        super.onStop();
        if(registration != null)                  //To be moved to onpause()
            registration.remove();
//        if(DocumentDataListeners != null)       //Might not be required.
//            DocumentDataListeners.remove();
        if(DeviceDataListeners != null)
            DeviceDataListeners.remove();
        if(ReadingListeners != null)
            ReadingListeners.remove();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                                        Intent intent = new Intent(MainActivity.this,
                                                SignUpActivity.class);
                                        startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
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


    private void extract_data_from_document(){

        Map<Number,Map<String,Number>> all_readings = new HashMap<Number,Map<String,Number>>();
        for(DocumentSnapshot curr_doc : DeviceReadings){
            Map<String,Map<String,Number>> curr_device_readings = (Map<String,Map<String,Number>>)curr_doc.get("previous_readings");
            for(String key: curr_device_readings.keySet())
                {
                    Long int_key = Long.parseLong(key);
                    if(all_readings.containsKey(int_key)){
                        int_key = int_key + 1;
                    }
                    all_readings.put(int_key,curr_device_readings.get(key));
                }
        }
         all_readings_sorted = new TreeMap<Number,Map<String,Number>>(all_readings);

        update_top_table();
        populate_reading_history_chart();
        populate_company_health_chart();
    }


    private void initShowLatestReading(DocumentReference docRef, String activeDeviceID){


        registration = docRef.collection("devices").document(activeDeviceID)
                .collection("readings")
                .orderBy("time", Query.Direction.DESCENDING).limit(1)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        for (QueryDocumentSnapshot doc : value) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                }
                            });
                        }
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


    private void update_data(){
        ArrayList<DocumentSnapshot> device_data;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        DocumentDataListeners = firestore.collection("users").document(currentUser.getUid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            DocumentData = snapshot;
                        } else {
                            Log.d(TAG, "No Data");
                        }

                        add_devices_listeners();

                    }
                });   
    }


    private void update_top_table(){
        /* Update Total Read, Last readings */
        //Assumption : all_reading_sorted contains only the latest readings
        total_reads = all_readings_sorted.size();
        Number last_read_timestamp = (Number)all_readings_sorted.keySet().toArray()[total_reads-1];
        total_visits_value.setText(String.format("%d",total_reads));
        oxygen_value.setText(String.format("%.1f %%",all_readings_sorted.get(last_read_timestamp).get("oxygen").floatValue()));
        temperature_value.setText(String.format("%.1f \u00B0 F",all_readings_sorted.get(last_read_timestamp).get("temperature").floatValue()));
        heartrate_value.setText(String.format("%d bpm",all_readings_sorted.get(last_read_timestamp).get("heartrate").intValue()));

    }

    private void populate_company_health_chart(){

        int bad_oxy_count = 0;
        int bad_temp_count = 0;
        int bad_hr_count = 0;

        int green_band = 0;
        int yellow_band = 0;
        int orange_band = 0;
        int red_band =0;
        for(Number ind_timestamp : all_readings_sorted.keySet() ){
            int red_flags = 0;
            float oxygen_val = all_readings_sorted.get(ind_timestamp).get("oxygen").floatValue();
            float temp_val = all_readings_sorted.get(ind_timestamp).get("temperature").floatValue();
            float hr_val = all_readings_sorted.get(ind_timestamp).get("heartrate").floatValue();
            if(oxygen_val < MIN_OXY_LIM){
                bad_oxy_count++;
                red_flags++;
            }
            if(!(temp_val > MIN_TEMPERATURE && temp_val < MAX_TEMPERATURE)){
                bad_temp_count++;
                red_flags++;
            }
            if(!(hr_val > MIN_HEARTRATE && hr_val < MAX_TEMPERATURE)){
                bad_hr_count++;
                red_flags++;
            }
            switch (red_flags){
                case 0 :
                    green_band++;
                    break;
                case 1:
                    yellow_band++;
                    break;
                case 2:
                    orange_band++;
                    break;
                case 3:
                    red_band++;
                    break;
            }
        }

        Log.d(TAG,"Bad Oxygen  "+bad_oxy_count);
        Log.d(TAG,"Bad Heartrate "+bad_hr_count);
        Log.d(TAG,"Bad temperature"+bad_temp_count);
        Log.d(TAG,"Bands"+green_band);
        Log.d(TAG,"Total Reads"+total_reads);


        ArrayList<PieEntry> pie_comp_health_chart_entries = new ArrayList<>();
        pie_comp_health_chart_entries.add(new PieEntry(((float)(green_band)/total_reads)*100f,"Healthy"));
        pie_comp_health_chart_entries.add(new PieEntry(((float)(yellow_band)/total_reads)*100f,"Unfit"));
        pie_comp_health_chart_entries.add(new PieEntry(((float)(orange_band)/total_reads)*100f,"Ill"));
        pie_comp_health_chart_entries.add(new PieEntry(((float)(red_band)/total_reads)*100f,"Dead"));

        ArrayList<PieEntry> pie_oxy_chart_entries = new ArrayList<>();
        pie_oxy_chart_entries.add(new PieEntry(((float)(total_reads - bad_oxy_count)/total_reads)*100f,"Healthy"));
        pie_oxy_chart_entries.add(new PieEntry(((float)(bad_oxy_count)/total_reads)*100f," Unhealthy"));

        ArrayList<PieEntry> pie_temp_chart_entries = new ArrayList<>();
        pie_temp_chart_entries.add(new PieEntry(((float)(total_reads - bad_temp_count)/total_reads)*100f,"Healthy"));
        pie_temp_chart_entries.add(new PieEntry(((float)(bad_temp_count)/total_reads)*100f," Unhealthy"));

        ArrayList<PieEntry> pie_hr_chart_entries = new ArrayList<>();
        pie_hr_chart_entries.add(new PieEntry(((float)(total_reads - bad_hr_count)/total_reads)*100f,"Healthy"));
        pie_hr_chart_entries.add(new PieEntry(((float)(bad_hr_count)/total_reads)*100f," Unhealthy"));


        PieDataSet oxy_dataSet = new PieDataSet(pie_oxy_chart_entries, "Oxygen Reading Report");
        PieDataSet temp_dataSet = new PieDataSet(pie_temp_chart_entries, "Temperature Reading Report");
        PieDataSet hr_dataSet = new PieDataSet(pie_hr_chart_entries, "Heart Rate Reading Report");
        PieDataSet comp_health_dataSet = new PieDataSet(pie_comp_health_chart_entries, "Company Health Report");


        Log.d(TAG,"Oxygen entries "+pie_oxy_chart_entries.toString());
        Log.d(TAG,"heartrate entries "+pie_hr_chart_entries.toString());
        Log.d(TAG,"temperature entries "+pie_temp_chart_entries.toString());
        Log.d(TAG,"Comp entries"+pie_comp_health_chart_entries.toString());
        // Common colors initialization

        ArrayList<Integer> colors = new ArrayList<>();

        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        colors.add(ColorTemplate.getHoloBlue());

        // Oxygen chart Settings
        oxy_dataSet.setDrawIcons(false);
        oxy_dataSet.setSliceSpace(3f);
        oxy_dataSet.setIconsOffset(new MPPointF(0, 40));
        oxy_dataSet.setSelectionShift(5f);

        oxy_dataSet.setColors(colors);

        PieData oxy_data = new PieData(oxy_dataSet);
        oxy_data.setValueFormatter(new PercentFormatter());
        oxy_data.setValueTextSize(11f);
        oxy_data.setValueTextColor(Color.WHITE);
        oxygen_pie_chart.setData(oxy_data);

        // Temperature chart Settings
        temp_dataSet.setDrawIcons(false);
        temp_dataSet.setSliceSpace(3f);
        temp_dataSet.setIconsOffset(new MPPointF(0, 40));
        temp_dataSet.setSelectionShift(5f);

        temp_dataSet.setColors(colors);

        PieData temp_data = new PieData(temp_dataSet);
        temp_data.setValueFormatter(new PercentFormatter());
        temp_data.setValueTextSize(11f);
        temp_data.setValueTextColor(Color.BLUE);
        temperature_pie_chart.setData(temp_data);

        // HeartRate chart Settings
        hr_dataSet.setDrawIcons(false);
        hr_dataSet.setSliceSpace(3f);
        hr_dataSet.setIconsOffset(new MPPointF(0, 40));
        hr_dataSet.setSelectionShift(5f);

        hr_dataSet.setColors(colors);

        PieData hr_data = new PieData(hr_dataSet);
        hr_data.setValueFormatter(new PercentFormatter());
        hr_data.setValueTextSize(11f);
        hr_data.setValueTextColor(Color.MAGENTA);
        heartrate_pie_chart.setData(hr_data);

        // Company Health chart Settings
        comp_health_dataSet.setDrawIcons(false);
        comp_health_dataSet.setSliceSpace(3f);
        comp_health_dataSet.setIconsOffset(new MPPointF(0, 40));
        comp_health_dataSet.setSelectionShift(5f);

        comp_health_dataSet.setColors(colors);

        PieData comp_health_data = new PieData(comp_health_dataSet);
        comp_health_data.setValueFormatter(new PercentFormatter());
        comp_health_data.setValueTextSize(11f);
        comp_health_data.setValueTextColor(Color.LTGRAY);
        company_health_chart.setData(comp_health_data);

        // undo all highlights
        company_health_chart.highlightValues(null);
        oxygen_pie_chart.highlightValues(null);
        temperature_pie_chart.highlightValues(null);
        heartrate_pie_chart.highlightValues(null);

        company_health_chart.invalidate();
        oxygen_pie_chart.invalidate();
        temperature_pie_chart.invalidate();
        heartrate_pie_chart.invalidate();


    }

    private void populate_reading_history_chart(){

        List<Entry> oxygen_entries = new ArrayList<Entry>();
        List<Entry> temperature_entries = new ArrayList<Entry>();
        List<Entry> heartrate_entries = new ArrayList<Entry>();
        Map<Float,String> timestamp_string = new HashMap<Float, String>();
        counter = 0f;
        for (Number timestamp: all_readings_sorted.keySet()) {
            Date date = new Date((long)timestamp);
            SimpleDateFormat dateformat = new SimpleDateFormat("dd-MM HH:mm");
            dateformat.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));
            String xaxis_timestamp = dateformat.format(date);

            timestamp_string.put((float)counter,xaxis_timestamp);
            oxygen_entries.add(new Entry((float)counter, all_readings_sorted.get(timestamp).get("oxygen").floatValue()));
            temperature_entries.add(new Entry((float)counter, all_readings_sorted.get(timestamp).get("temperature").floatValue()));
            heartrate_entries.add(new Entry((float)counter, all_readings_sorted.get(timestamp).get("heartrate").floatValue()));
            counter = counter + 1f;
        }
        counter = 0f;
        LineDataSet setOxygen = new LineDataSet(oxygen_entries,"Oxygen");
        setOxygen.setAxisDependency(YAxis.AxisDependency.RIGHT);
        setOxygen.setColor(ColorTemplate.getHoloBlue());
        setOxygen.setCircleColor(Color.WHITE);
        setOxygen.setLineWidth(2f);
        setOxygen.setCircleRadius(3f);
        setOxygen.setFillAlpha(65);
        setOxygen.setFillColor(ColorTemplate.getHoloBlue());
        setOxygen.setHighLightColor(Color.rgb(244, 117, 117));
        setOxygen.setDrawCircleHole(false);

        LineDataSet setTemperature = new LineDataSet(temperature_entries,"Temperature");
        setTemperature.setAxisDependency(YAxis.AxisDependency.LEFT);
        setTemperature.setColor(Color.RED);
        setTemperature.setCircleColor(Color.WHITE);
        setTemperature.setLineWidth(2f);
        setTemperature.setCircleRadius(3f);
        setTemperature.setFillAlpha(65);
        setTemperature.setFillColor(Color.RED);
        setTemperature.setDrawCircleHole(false);
        setTemperature.setHighLightColor(Color.rgb(244, 117, 117));

        LineDataSet setHeartrate = new LineDataSet(heartrate_entries,"Heart Rate");
        setHeartrate.setAxisDependency(YAxis.AxisDependency.RIGHT);
        setHeartrate.setColor(Color.YELLOW);
        setHeartrate.setCircleColor(Color.WHITE);
        setHeartrate.setLineWidth(2f);
        setHeartrate.setCircleRadius(3f);
        setHeartrate.setFillAlpha(65);
        setHeartrate.setFillColor(ColorTemplate.colorWithAlpha(Color.YELLOW, 200));
        setHeartrate.setDrawCircleHole(false);
        setHeartrate.setHighLightColor(Color.rgb(244, 117, 117));

        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(setOxygen);
        dataSets.add(setTemperature);
        dataSets.add(setHeartrate);
        LineData data = new LineData(dataSets);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(9f);
        complete_reading_chart.setData(data);
        //Format X axis
        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                counter = counter + 1f;
                return timestamp_string.get(value);
            }
        };

        xAxis.setLabelRotationAngle(90f);
        xAxis.setGranularity(3600000f);
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setValueFormatter(formatter);



        complete_reading_chart.invalidate(); // refresh
    }

    private void add_devices_listeners(){
        DocumentReference user_document_ref = firestore.collection("users")
                .document(currentUser.getUid());
        DeviceDataListeners = user_document_ref.collection("devices")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }
                        DeviceData = snapshots.getDocuments();
                    }
                });

        ReadingListeners = user_document_ref.collection("Readings")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }
                        if(snapshots!=null) {
                            DeviceReadings = snapshots.getDocuments();
                            extract_data_from_document();
                        }
                        else {
                        Log.d(TAG,"documents not found");
                        }
                    }
                });
    }

    /**
     * Method to add a new device
     */
    private void addDevice(){
        String fcmToken = DocumentData.get("FCMToken").toString();
        Log.d(TAG,fcmToken.length() + "");
        Intent intent = new Intent(MainActivity.this,AddDeviceActivity.class);
        intent.putExtra(ADD_DEVICE_EXTRA,fcmToken);
        startActivity(intent);
    }
}