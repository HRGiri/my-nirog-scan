package com.example.mynirogscan;

import android.os.Bundle;

import androidx.annotation.Nullable;
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
public class WiFiSelectFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "ssid";
//    private static final String ARG_PARAM2 = "param2";
//
//    // TODO: Rename and change types of parameters
    private String ssid;
//    private String mParam2;
    private static final String FRAGMENT_TAG = "WiFiFragment";

    public WiFiSelectFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment WiFiSelectFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WiFiSelectFragment newInstance(String param1) {
        WiFiSelectFragment fragment = new WiFiSelectFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ssid = getArguments().getString(ARG_PARAM1);
//            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wi_fi_select, container, false);
        TextView wifiSSID = view.findViewById(R.id.tv_wifi_ssid);
        wifiSSID.setText(ssid);
        EditText passInput = view.findViewById(R.id.et_wifi_password);
        Button sendConfigButton = view.findViewById(R.id.button_send);
        sendConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(FRAGMENT_TAG,passInput.getText().toString().trim());
                AddDeviceActivity activity = (AddDeviceActivity) getActivity();
                if(activity != null) {
                    activity.wifiSsid = ssid;
                    activity.wifiPassword = passInput.getText().toString().trim();
                }
                getParentFragmentManager().beginTransaction().remove(WiFiSelectFragment.this).commit();
            }
        });
        return view;
    }
}