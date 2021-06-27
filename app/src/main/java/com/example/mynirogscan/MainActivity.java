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
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
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
import com.google.firebase.firestore.Source;
import com.google.firebase.messaging.FirebaseMessaging;

import java.math.BigDecimal;
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
    private LineChart reading_chart;
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
        reading_chart = findViewById(R.id.Linechart);

        // enable scaling and dragging
        reading_chart.setDragEnabled(true);
        reading_chart.setScaleEnabled(true);
        reading_chart.setDrawGridBackground(false);
        reading_chart.setHighlightPerDragEnabled(true);
        reading_chart.setPinchZoom(true);
        reading_chart.setBackgroundColor(Color.TRANSPARENT);
        xAxis = reading_chart.getXAxis();
        reading_chart.animateX(1500);


        xAxis.setTextSize(11f);
        xAxis.setTextColor(Color.MAGENTA);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);

        YAxis leftAxis = reading_chart.getAxisLeft();
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setAxisMaximum(120f);
        leftAxis.setAxisMinimum(90f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularityEnabled(true);

        YAxis rightAxis = reading_chart.getAxisRight();
        rightAxis.setTextColor(Color.RED);
        rightAxis.setAxisMaximum(200f);
        rightAxis.setAxisMinimum(20f);
        rightAxis.setDrawGridLines(false);
        rightAxis.setDrawZeroLine(false);
        rightAxis.setGranularityEnabled(true);


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
        heartrate_pie_chart.setEntryLabelColor(Color.WHITE);
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

    private void populate_company_health_chart(){
        ArrayList<PieEntry> entries = new ArrayList<>();



        int count = 5;
        int range = 5;
        String[] parties = new String[]{"one","two","three","four","five"};
        //####Company health
        for (int i = 0; i < count ; i++) {
            entries.add(new PieEntry((float) ((Math.random() * range) + range / 5),
                    parties[i % parties.length]));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Election Results");

        dataSet.setDrawIcons(false);

        dataSet.setSliceSpace(3f);
        dataSet.setIconsOffset(new MPPointF(0, 40));
        dataSet.setSelectionShift(5f);

        // add a lot of colors

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

        dataSet.setColors(colors);
        //dataSet.setSelectionShift(0f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(Color.WHITE);
        company_health_chart.setData(data);
        oxygen_pie_chart.setData(data);
        temperature_pie_chart.setData(data);
        heartrate_pie_chart.setData(data);
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
        reading_chart.setData(data);
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



        reading_chart.invalidate(); // refresh
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