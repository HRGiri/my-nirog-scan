package com.example.mynirogscan;

import android.content.ClipData;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;




public class GlobalData extends ViewModel {

    private static final String TAG = "Firebase";

    ListenerRegistration DeviceDataListeners;
    ListenerRegistration ReadingListeners;
    List<DocumentSnapshot> DeviceData;
    List<DocumentSnapshot> DeviceReadings;
    private MutableLiveData<List<DocumentSnapshot>> GlobalDeviceData = new MutableLiveData<List<DocumentSnapshot>>();
    private MutableLiveData<List<DocumentSnapshot>> GlobalDeviceReadings = new MutableLiveData<List<DocumentSnapshot>>();

    public LiveData<List<DocumentSnapshot>> getGlobalDeviceData(FirebaseFirestore firestore, FirebaseUser currentUser){
        if(DeviceData == null){

            set_deviceData_listeners(firestore,currentUser);
        }
        return GlobalDeviceData;
    }

    public LiveData<List<DocumentSnapshot>> getGlobalDeviceReadings(FirebaseFirestore firestore, FirebaseUser currentUser){
        if(DeviceReadings == null){
            set_deviceReadings_listeners(firestore,currentUser);
        }
        return GlobalDeviceReadings;
    }


    private void set_deviceData_listeners(FirebaseFirestore firestore, FirebaseUser currentUser) {
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
                        GlobalDeviceData.setValue(DeviceData);
                    }
                });
    }
    private void set_deviceReadings_listeners(FirebaseFirestore firestore, FirebaseUser currentUser){
        DocumentReference user_document_ref = firestore.collection("users")
                .document(currentUser.getUid());
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
                            GlobalDeviceReadings.setValue(DeviceReadings);
                        }
                        else {
                            Log.d(TAG,"documents not found");
                        }
                    }
                });
    }
}
