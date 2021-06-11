package com.example.mynirogscan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "Register";
    private EditText emailInput;
    private EditText passwordInput;
    private EditText rePasswordInput;
    private EditText nameInput;
    private EditText phoneInput;
    private Button registerButton;
    private ProgressBar progressBar;


    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private String token = "";
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
//        mAuth.useEmulator("10.0.2.2", 9099);

        emailInput = findViewById(R.id.et_email);
        passwordInput = findViewById(R.id.et_pass);
        registerButton = findViewById(R.id.button_register);
        rePasswordInput = findViewById(R.id.et_repass);
        nameInput = findViewById(R.id.et_name);
        phoneInput = findViewById(R.id.et_phone);
        progressBar = findViewById(R.id.progressBar2);

        progressBar.setVisibility(View.INVISIBLE);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String username = emailInput.getText().toString().trim();
                String password = rePasswordInput.getText().toString().trim();
                if(!password.equals(passwordInput.getText().toString().trim())){
                    Toast.makeText(getApplicationContext(),"Please enter the same password",Toast.LENGTH_LONG).show();
                    return;
                }

                String name = nameInput.getText().toString().trim();
                String phone = phoneInput.getText().toString().trim();

                if(name.length() == 0){
                    nameInput.setError("Required");
                    return;
                }
                if(username.length() == 0){
                    emailInput.setError("Required");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                //Login using Authenticator
                createAccount(username,password, name);
            }
        });

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.length() < 8){
                    passwordInput.setError("Password must be at least 8 characters long");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        rePasswordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.length() < 8){
                    rePasswordInput.setError("Password must be at least 8 characters long");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void createAccount(String email, String password, String name) {
        // [START create_user_with_email]
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.INVISIBLE);
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            Toast.makeText(SignUpActivity.this, "Create User Success!",
                                    Toast.LENGTH_SHORT).show();
                            currentUser = mAuth.getCurrentUser();
                            //TODO: Upload name and phone. Send verification to email and phone. Proceed to Main Menu
                            updateProfile(currentUser,name);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUpActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            //TODO: Show failure message
                        }
                    }
                });
        // [END create_user_with_email]
    }

    private void updateProfile(FirebaseUser user, String name){
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User profile updated.");
                            Toast.makeText(getApplicationContext(),"User Profile updated",Toast.LENGTH_SHORT).show();
                            sendEmailVerification();
                        }
                    }
                });
    }

    private void sendEmailVerification() {
        // Send verification email
        // [START send_email_verification]
        final FirebaseUser user = mAuth.getCurrentUser();
        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Email sent
                        Toast.makeText(getApplicationContext(),"Please verify your email",Toast.LENGTH_LONG).show();
                        test(currentUser);
                    }
                });
        // [END send_email_verification]
    }

    private void test(FirebaseUser currentUser){
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
                        Log.d(TAG,token);
                        addUser(currentUser.getDisplayName(),currentUser.getEmail());
                    }
                });
    }
    private void addUser(String name, String email){
        // Create a new user with a first and last name
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("FCMToken", token);
        user.put("uid",currentUser.getUid());
        Log.d(TAG,"User to be added");
        // Add a new document with a generated ID
        firestore.collection("users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                        Toast.makeText(getApplicationContext(),"Account Registered!",Toast.LENGTH_SHORT).show();
                        finishActivity(1);
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