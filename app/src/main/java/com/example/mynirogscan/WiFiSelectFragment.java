package com.example.mynirogscan;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WiFiSelectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WiFiSelectFragment extends DialogFragment {

    public interface WifiSelectDialogListener {
        void onSubmitWifiPassword(DialogFragment dialog);
    }
    WifiSelectDialogListener listener;
    public static final String FRAGMENT_TAG = "WiFiFragment";
    private String ssid;
    private AddDeviceFragment parentFragment;

    public WiFiSelectFragment() {
        // Required empty public constructor
    }
    public WiFiSelectFragment(Fragment parent, String ssid){
        this.ssid = ssid;
        parentFragment = (AddDeviceFragment) parent;
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (WifiSelectDialogListener) parent;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException("Must implement SettingsDialogListener");
        }
    }

    public static WiFiSelectFragment newInstance() {
        WiFiSelectFragment fragment = new WiFiSelectFragment();
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wi_fi_select, container, false);
        TextView wifiSSID = view.findViewById(R.id.tv_wifi_ssid);
        wifiSSID.setText(ssid);
        EditText passInput = view.findViewById(R.id.et_password_input);
        Button sendConfigButton = view.findViewById(R.id.button_wifi_password);
        sendConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(FRAGMENT_TAG,passInput.getText().toString().trim());
                if(parentFragment != null) {
                    parentFragment.wifiSsid = ssid;
                    parentFragment.wifiPassword = passInput.getText().toString().trim();
                }
                listener.onSubmitWifiPassword(WiFiSelectFragment.this);
                dismiss();
//                getParentFragmentManager().beginTransaction().remove(WiFiSelectFragment.this).commit();
            }
        });
        return view;
    }
}