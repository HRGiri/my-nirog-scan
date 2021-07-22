package com.example.mynirogscan;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.example.mynirogscan.Constants.*;


public class GlobalData extends ViewModel {

    private static final String TAG = "Firebase";

    public Context context;
    ListenerRegistration deviceDataListeners;
    ListenerRegistration readingListeners;
    List<DocumentSnapshot> deviceDataDocuments;
    List<DocumentSnapshot> deviceReadingsDocuments;
    private MutableLiveData<List<DocumentSnapshot>> globalDeviceData = new MutableLiveData<List<DocumentSnapshot>>();
    private MutableLiveData<List<DocumentSnapshot>> globalDeviceReadings = new MutableLiveData<List<DocumentSnapshot>>();
    private MutableLiveData<Map<String,Object>> globalUserData = new MutableLiveData<>();
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private ListenerRegistration documentDataListeners;
    private Map<String,Object> userData;
    private Map<Number, Map<String, Number>> all_readings_sorted;
    private MutableLiveData<Map<Number, Map<String, Number>>> globalAllReadingsSorted = new MutableLiveData<>();
    private Boolean isInit = false;
    private MutableLiveData<Boolean> globalIsInit = new MutableLiveData<>(false);

    public void init(){
        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser() != null) {
            firestore = FirebaseFirestore.getInstance();
            firestore.enableNetwork();
            if (documentDataListeners == null)
                update_data();
        }
    }

    public LiveData<Boolean> getIsInit(){
        return globalIsInit;
    }
    public LiveData<Map<String,Object>> getGlobalUserData(){
        return globalUserData;
    }

    public LiveData<List<DocumentSnapshot>> getGlobalDeviceData(){
//        if(deviceData == null){
//            set_deviceData_listeners(firestore,currentUser);
//        }
        return globalDeviceData;
    }

    public LiveData<List<DocumentSnapshot>> getGlobalDeviceReadings(){
//        if(deviceReadings == null){
//            set_deviceReadings_listeners(firestore,currentUser);
//        }
        return globalDeviceReadings;
    }

    public LiveData<Map<Number,Map<String,Number>>> getAllReadingsSorted(){
        return globalAllReadingsSorted;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        removeListeners();
    }

    private void update_data(){
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        documentDataListeners = firestore.collection(USER_DOCUMENT_NAME).document(currentUser.getUid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            userData = snapshot.getData();
                            globalUserData.setValue(userData);
                        } else {
                            Log.d(TAG, "No Data");
                        }

                        if(deviceDataListeners == null)
                            set_deviceData_listeners(firestore,FirebaseAuth.getInstance().getCurrentUser());
                        if(readingListeners == null)
                            set_deviceReadings_listeners(firestore,FirebaseAuth.getInstance().getCurrentUser());

                    }
                });
    }

    private void set_deviceData_listeners(FirebaseFirestore firestore, FirebaseUser currentUser) {
        DocumentReference user_document_ref = firestore.collection("users")
                .document(currentUser.getUid());
        deviceDataListeners = user_document_ref.collection("devices")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }
                        deviceDataDocuments = snapshots.getDocuments();
                        globalDeviceData.setValue(deviceDataDocuments);
                    }
                });
    }
    private void set_deviceReadings_listeners(FirebaseFirestore firestore, FirebaseUser currentUser){
        DocumentReference user_document_ref = firestore.collection("users")
                .document(currentUser.getUid());
        readingListeners = user_document_ref.collection("Readings")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }
                        if(snapshots!=null) {
                            deviceReadingsDocuments = snapshots.getDocuments();
                            globalDeviceReadings.setValue(deviceReadingsDocuments);
                            extract_data_from_document();
                        }
                        else {
                            Log.d(TAG,"documents not found");
                        }
                    }
                });
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
                            Toast.makeText(context,"Profile updated successfully",Toast.LENGTH_SHORT).show();
//                            sendEmailVerification();
                        }
                    }
                });
    }

    public void signOut(){
        mAuth.signOut();
        isInit = false;
        globalIsInit.setValue(isInit);
        removeListeners();
    }

    public void uploadUserData(Map<String,Object> data){
        firestore.collection(USER_DOCUMENT_NAME)
                .document(mAuth.getCurrentUser().getUid())
                .set(data,SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG,"Success");
                        if(data.containsKey(NAME_FIELD_NAME)){
                            updateProfile("" + data.get(NAME_FIELD_NAME));
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG,"Failure");
                        Toast.makeText(context,"Failed to update details!",Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void uploadDeviceData(Map<String,Object> deviceData){
        uploadDeviceData(deviceData,0);
    }
    public void uploadDeviceData(Map<String,Object> deviceData,int position){
        Log.d(TAG,"Uploading...");
        firestore = FirebaseFirestore.getInstance();
        DocumentReference docRef = firestore.collection(USER_DOCUMENT_NAME)
                .document(mAuth.getCurrentUser().getUid())
                .collection(DEVICE_COLLECTION_NAME)
                .document("" + deviceData.get(DEVICE_ID_FIELD_NAME));
        docRef.set(deviceData, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG,"Success");
                        if(deviceData.containsKey(DEVICE_NAME_FIELD_NAME)) {
                            ArrayList<Map<String, String>> deviceList = (ArrayList<Map<String, String>>) userData.get(DEVICE_LIST_FIELD_NAME);
                            deviceList.get(position).replace(NAME_FIELD_NAME,"" + deviceData.get(DEVICE_NAME_FIELD_NAME));
                            firestore.collection(USER_DOCUMENT_NAME)
                                    .document(mAuth.getCurrentUser().getUid())
                                    .update(DEVICE_LIST_FIELD_NAME,deviceList)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Log.d(TAG,"Success");
                                            Toast.makeText(context,"Details updated successfully!",Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG,"Failure");
                                            Toast.makeText(context,"Failed to update details!",Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG,"Failure");
                    }
                });
    }

    private void extract_data_from_document(){

        Map<Number, Map<String,Number>> all_readings = new HashMap<Number,Map<String,Number>>();
        for(DocumentSnapshot curr_doc : deviceReadingsDocuments){
            Map<String,Map<String,Number>> curr_device_readings = (Map<String,Map<String,Number>>)curr_doc.get("previous_readings");
            if(curr_device_readings == null)
                return;
            for(String key: curr_device_readings.keySet())
            {
                Long int_key = Long.parseLong(key);
                if(all_readings.containsKey(int_key)){
                    int_key = int_key + 1;
                }
                all_readings.put(int_key,curr_device_readings.get(key));
            }
        }
        all_readings_sorted = new TreeMap<Number,Map<String,Number>>(all_readings).descendingMap();
//        Log.d(TAG,"sorted list "+all_readings_sorted.keySet());
        globalAllReadingsSorted.setValue(all_readings_sorted);
        globalIsInit.setValue(true);
    }

    public Map<Number,Map<String,Number>> getDeviceReadingsSorted(String deviceID){
        Map<Number,Map<String,Number>> deviceReadings = new TreeMap<Number,Map<String,Number>>().descendingMap();
        for(Number key : all_readings_sorted.keySet()){
            if(String.format("" + all_readings_sorted.get(key).get("uuid")).equals(deviceID))
                deviceReadings.put(key,all_readings_sorted.get(key));
        }
        return deviceReadings;
    }

    public void removeListeners(){
        if(documentDataListeners != null) {
            documentDataListeners.remove();
            documentDataListeners = null;
        }
        if(deviceDataListeners != null) {
            deviceDataListeners.remove();
            deviceDataListeners = null;
        }
        if(readingListeners != null) {
            readingListeners.remove();
            readingListeners = null;
        }

    }
}
