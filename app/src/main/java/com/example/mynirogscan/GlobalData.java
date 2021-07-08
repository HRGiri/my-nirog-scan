package com.example.mynirogscan;

import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class GlobalData extends ViewModel {

    private static final String TAG = "Firebase";

    ListenerRegistration deviceDataListeners;
    ListenerRegistration readingListeners;
    List<DocumentSnapshot> deviceData;
    List<DocumentSnapshot> deviceReadings;
    private MutableLiveData<List<DocumentSnapshot>> globalDeviceData = new MutableLiveData<List<DocumentSnapshot>>();
    private MutableLiveData<List<DocumentSnapshot>> globalDeviceReadings = new MutableLiveData<List<DocumentSnapshot>>();
    private MutableLiveData<DocumentSnapshot> GlobalUsersData = new MutableLiveData<>();
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private ListenerRegistration documentDataListeners;
    private DocumentSnapshot usersData;
    private Map<Number, Map<String, Number>> all_readings_sorted;
    private MutableLiveData<Map<Number, Map<String, Number>>> globalAllReadingsSorted = new MutableLiveData<>();
    private Boolean isInit = false;
    private MutableLiveData<Boolean> globalIsInit = new MutableLiveData<>();

    public void init(){
        firestore = FirebaseFirestore.getInstance();
        firestore.enableNetwork();
        if(documentDataListeners == null)
            update_data();
    }

    public LiveData<Boolean> getIsInit(){
        return globalIsInit;
    }
    public LiveData<DocumentSnapshot> getGlobalUsersData(){
        return GlobalUsersData;
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
        documentDataListeners = firestore.collection(Constants.USER_DOCUMENT_NAME).document(currentUser.getUid())
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        if (snapshot != null && snapshot.exists()) {
                            usersData = snapshot;
                            GlobalUsersData.setValue(usersData);
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
                        deviceData = snapshots.getDocuments();
                        globalDeviceData.setValue(deviceData);
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
                            deviceReadings = snapshots.getDocuments();
                            globalDeviceReadings.setValue(deviceReadings);
                            extract_data_from_document();
                        }
                        else {
                            Log.d(TAG,"documents not found");
                        }
                    }
                });
    }

    private void extract_data_from_document(){

        Map<Number, Map<String,Number>> all_readings = new HashMap<Number,Map<String,Number>>();
        for(DocumentSnapshot curr_doc : deviceReadings){
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
        all_readings_sorted = new TreeMap<Number,Map<String,Number>>(all_readings).descendingMap();
        Log.d(TAG,"sorted list "+all_readings_sorted.keySet());
        globalAllReadingsSorted.setValue(all_readings_sorted);
        globalIsInit.setValue(true);
    }

    public void removeListeners(){
        if(documentDataListeners != null)       //Might not be required.
            documentDataListeners.remove();
        if(deviceDataListeners != null)
            deviceDataListeners.remove();
        if(readingListeners != null)
            readingListeners.remove();

    }
}
