package com.example.mynirogscan;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import static android.app.Activity.RESULT_OK;

public class SignUpFragment extends Fragment {

    private static final String TAG = "Register";
    private static final int RC_SIGN_IN = 1;
    private EditText orgInput;
    private EditText nameInput;
    private EditText lastnameInput;
    private EditText phoneInput;
    private Button registerButton;
    private ProgressBar progressBar;
    private ConstraintLayout constraintLayout;


    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String token = "";

    public SignUpFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SignUpFragment newInstance() {
        return new SignUpFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_sign_up, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        registerButton = view.findViewById(R.id.button_register);
        orgInput = view.findViewById(R.id.et_organization);
        nameInput = view.findViewById(R.id.et_name);
        lastnameInput = view.findViewById(R.id.et_name_last);
        phoneInput = view.findViewById(R.id.et_phoneNo);
        progressBar = view.findViewById(R.id.progressBar2);
        constraintLayout = view.findViewById(R.id.constraintlayout);


        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().enableEmailLinkSignIn()
                        .setActionCodeSettings(buildActionCodeSettings()).build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // TODO: Create and launch sign-in intent
//        if (AuthUI.canHandleIntent(getIntent())) {
//            Log.d(TAG,"Can Handle Intent");
//            constraintLayout.setVisibility(View.INVISIBLE);
////            verifyEmailSignIn(providers);
//        }


        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
//        mAuth.useEmulator("10.0.2.2", 9099);


        progressBar.setVisibility(View.INVISIBLE);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = nameInput.getText().toString().trim();
                String lastName = lastnameInput.getText().toString().trim();
                String phone = phoneInput.getText().toString().trim();
                String organization = orgInput.getText().toString().trim();

                if (username.length() == 0) {
                    nameInput.setError("Required");
                    return;
                }
                if (lastName.length() == 0) {
                    lastnameInput.setError("Required");
                    return;
                }
                if (phone.length() == 0) {
                    phoneInput.setError("Required");
                    return;
                }
                if (organization.length() == 0) {
                    orgInput.setError("Required");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                //Login using Authenticator
                updateProfile(username);
                addUser(username, lastName, phone, organization);
                createFCMtoken();

                //TODO: Jump to Main Page- Dashboard
//                Intent intent = new Intent(SignUpActivity.this,
//                        MainActivity.class);
//                startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });


    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                //Query Firestore
                firestore.collection("users").document(currentUser.getUid())
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()) {
                                    if (task.getResult().exists()) {
                                        Log.d(TAG, "Found it");
                                        createFCMtoken();
                                        //Jump to Main Page- Dashboard
                                        Intent intent = new Intent(getContext(),
                                                MainActivity.class);
                                        startActivity(intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));

                                    } else {
                                        Log.d(TAG, "Lets register you");
                                        constraintLayout.setVisibility(View.VISIBLE);


                                    }
                                } else  {
                                    Log.d(TAG,"Error getting Data : ", task.getException());
                                }
                            }
                        });
                Log.d(TAG,"THe user UID is "+ currentUser.getUid());

            } else {

                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                Log.d(TAG,"Sign in failed");
            }
        }
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


    //Update auth database to add display name
    private void updateProfile(String name){
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile updated.");
                            Toast.makeText(getContext(),"User Profile updated",Toast.LENGTH_SHORT).show();
//                            sendEmailVerification();
                        }
                    }
                });
    }

    //CHange after every LOgin and update Firestore
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
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Log.d(TAG, "FCM token s! + "+user.toString());
        firestore.collection("users").document(currentUser.getUid())
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

    //Add new user to Database
    private void addUser(String name, String lastname, String phone, String organization){
        // Create a new user with a first and last name
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, Object> user = new HashMap<>();
        user.put("name", name+" "+lastname);
        user.put("email", currentUser.getEmail());
        user.put("organization",organization);
        user.put("phone",phone);
        user.put("device_list",new ArrayList<>());

        Log.d(TAG,"User to be added");
        // Add a new document with a generated ID
        firestore.collection("users").document(currentUser.getUid())
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "user added to database!");
                        Toast.makeText(getContext(),"Account Registered!",Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }
}
