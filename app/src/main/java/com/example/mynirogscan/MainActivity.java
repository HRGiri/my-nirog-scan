package com.example.mynirogscan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static final String ADD_DEVICE_EXTRA = "com.example.mynirogscan.FCM_TOKEN";
    private static final String TAG = "MAINPAGE";
    public static final String FIREBASE_TAG = "Firebase";
    private static final int RC_SIGN_IN = 1;
    private String token;
    private TextView info;
    private TextView info2;
    private CardView cardLastReading;
    private TextView tvLastRead;
    private TextView tvTemperature;
    private TextView tvHeartRate;
    private TextView tvSpO2;
    public FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private ListenerRegistration registration;
    private DocumentSnapshot DocumentData;
    private ArrayList<DocumentSnapshot> DeviceData = new ArrayList<DocumentSnapshot>();

    String LAST_READ = "last_read";
    String PREVIOUS_READINGS = "previous readings";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
//        mAuth.useEmulator("10.0.2.2", 9099);

        firestore = FirebaseFirestore.getInstance();
//        emulatorSettings();

        info = findViewById(R.id.tv_main);
        info2 = findViewById(R.id.tv_info);
        cardLastReading = findViewById(R.id.card_last_reading);
        tvLastRead = findViewById(R.id.tv_last_read);
        tvTemperature = findViewById(R.id.tv_temp_val);
        tvHeartRate = findViewById(R.id.tv_hr_val);
        tvSpO2 = findViewById(R.id.tv_spo_val);

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
//            get_device_data("Device1");
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
        if(registration != null)
            registration.remove();
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
    private void getDevices(){
        ArrayList<Map<String,String>> device_list = (ArrayList<Map<String, String>>) DocumentData.get("device_list");

        for(Map<String,String> value : device_list){
            Log.d(TAG,"Device Name :"+ value.get("name"));
        }
    }

    private void initShowLatestReading(DocumentReference docRef, String activeDeviceID){
        cardLastReading.setVisibility(View.VISIBLE);

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
                                    tvLastRead.setText("Last read: " +  doc.get("time"));
                                    tvTemperature.setText(doc.get("temperature") + " C");
                                    tvHeartRate.setText(doc.get("heart_rate") + " bpm");
                                    tvSpO2.setText(doc.get("spo") + "%");
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
                .setPersistenceEnabled(false)
                .build();
        firestore.setFirestoreSettings(settings);
        // [END fs_emulator_connect]
    }

//   void get_device_data(){
//        ArrayList<Map<String,String>> device_name = DocumentData.get("device_list/");
//        Log.d(TAG,"Device data is :"+ device_data[0]);
//   }


    private void update_data(){
        ArrayList<DocumentSnapshot> device_data;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        firestore.collection("users").document(currentUser.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                             DocumentData = task.getResult();
                            if (DocumentData.exists()) {
                                Log.d(TAG, "DocumentSnapshot data: " + DocumentData.getData());
                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                        }
                        DocumentReference user_document_ref = firestore.collection("users")
                                .document(currentUser.getUid());
                        ArrayList<Map<String,String>> device_list = (ArrayList<Map<String, String>>) DocumentData.get("device_list");

                        for(Map<String,String> device : device_list){
                            user_document_ref.collection("devices/" + device.get("uuid") + "/Readings")
                                    .document("reading")
                                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if(task.isSuccessful()) {
                                        DeviceData.add(task.getResult());
                                    }
//                                    getDevices();
                                }

                            });
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