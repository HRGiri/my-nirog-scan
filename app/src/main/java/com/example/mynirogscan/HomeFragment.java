package com.example.mynirogscan;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.mynirogscan.ChartStateAdapter.ChartType;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

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
    private Button viewReadingsButton;
    private BarChart daily_visit_chart;
    public FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private DocumentSnapshot DocumentData;
    Map<Number, Map<String,Number>> all_readings_sorted;
    Float counter;   //Revisit after testing
    int total_reads;

    private ChartStateAdapter pieChartAdapter;
    private ViewPager2 pieViewPager;


    Charts chart = new Charts();
    private GlobalData globalData;
    private ChartStateAdapter lineChartAdapter;
    private ViewPager2 lineViewPager;


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

        info = view.findViewById(R.id.tv_main);

        viewReadingsButton = view.findViewById(R.id.btn_view_readings);
        viewReadingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                launchDatePicker();
                Navigation.findNavController(getActivity(),R.id.main_activity_nav_host)
                        .navigate(HomeFragmentDirections.actionHomeFragmentToReadingsTableFragment());
            }
        });

        total_visits_value = view.findViewById(R.id.total_visitors_card);
        oxygen_value = view.findViewById(R.id.oxygen_card);
        temperature_value = view.findViewById(R.id.temperature_card);
        heartrate_value = view.findViewById(R.id.heartrate_card);

        /*Daily visit chart*/
        daily_visit_chart = view.findViewById(R.id.visit_chart);

        //############## Pie Chart settings
        pieChartAdapter = new ChartStateAdapter(this, ChartType.PieChart);
        pieViewPager = view.findViewById(R.id.pieChartPager);
        pieViewPager.setAdapter(pieChartAdapter);
        pieViewPager.setPageTransformer(new ZoomOutPageTransformer());
//        viewPager.setOffscreenPageLimit(3);

        lineChartAdapter = new ChartStateAdapter(this, ChartType.LineChart);
        lineViewPager = view.findViewById(R.id.lineChartPager);
        lineViewPager.setAdapter(lineChartAdapter);
        lineViewPager.setPageTransformer(new ZoomOutPageTransformer());

        //##############Bar chart Settings
        daily_visit_chart = view.findViewById(R.id.visit_chart);
        chart.setupBarChart(daily_visit_chart);
        // Check if user is signed in (non-null) and update UI accordingly.
        mAuth = FirebaseAuth.getInstance();
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
                    globalData.getAllReadingsSorted().observe(requireActivity(),sortedReadings->{
                        all_readings_sorted = sortedReadings;
                        update_top_table();
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


    //TODO: Move this method to DevicesScreen
    /**
     * Method to generate a new id
     */
    private String generateDeviceId(){
        ArrayList<Map<String,String>> deviceList = (ArrayList<Map<String, String>>) DocumentData.get("device_list");
        if(deviceList == null){
            return currentUser.getUid() + "-001";
        }
        else {
            Log.d(TAG, String.valueOf(deviceList.size()));

            return String.format(currentUser.getUid() + "-%03d",deviceList.size() + 1);
        }
    }
}