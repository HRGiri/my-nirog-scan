    package com.example.mynirogscan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
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
    Map<Long,String> timestamp_string;



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
        reading_chart = findViewById(R.id.Linechart);

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

    private void populate_reading_history_chart(){

        List<Entry> oxygen_entries = new ArrayList<Entry>();
        List<Entry> temperature_entries = new ArrayList<Entry>();
        List<Entry> heartrate_entries = new ArrayList<Entry>();
        for (Number timestamp: all_readings_sorted.keySet()) {
            Date date = new Date((long)timestamp);
            SimpleDateFormat dateformat = new SimpleDateFormat("DD-MM HH:mm:ss");
            dateformat.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));
            String xaxis_timestamp = dateformat.format(date);
            timestamp_string.put((Long)timestamp,xaxis_timestamp);
            oxygen_entries.add(new Entry((Long)timestamp, all_readings_sorted.get(timestamp).get("oxygen").floatValue()));
            temperature_entries.add(new Entry((Long)timestamp, all_readings_sorted.get(timestamp).get("temperature").floatValue()));
            heartrate_entries.add(new Entry((Long)timestamp, all_readings_sorted.get(timestamp).get("heartrate").floatValue()));
        }
        LineDataSet setOxygen = new LineDataSet(oxygen_entries,"Oxygen");
        setOxygen.setAxisDependency(YAxis.AxisDependency.LEFT);
        LineDataSet setTemperature = new LineDataSet(temperature_entries,"Temperature");
        setTemperature.setAxisDependency(YAxis.AxisDependency.LEFT);
        LineDataSet setHeartrate = new LineDataSet(heartrate_entries,"Heartrate");
        setHeartrate.setAxisDependency(YAxis.AxisDependency.LEFT);

        List<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        dataSets.add(setOxygen);
        dataSets.add(setTemperature);
        dataSets.add(setHeartrate);
        LineData data = new LineData(dataSets);
        reading_chart.setData(data);

        //Format X axis
        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return timestamp_string.get();
            }
        };
        XAxis xAxis = reading_chart.getXAxis();
        xAxis.setGranularity(1f); // minimum axis-step (interval) is 1
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