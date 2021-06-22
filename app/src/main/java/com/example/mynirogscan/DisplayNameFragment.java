package com.example.mynirogscan;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DisplayNameFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DisplayNameFragment extends Fragment {


    public DisplayNameFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DisplayNameFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DisplayNameFragment newInstance(String param1, String param2) {
        return new DisplayNameFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_display_name, container, false);
        EditText nameInput = view.findViewById(R.id.et_device_name);
        Button displayNameButton = view.findViewById(R.id.button_display_name);
        displayNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddDeviceActivity activity = (AddDeviceActivity) getActivity();
                if(activity != null) {
                    activity.displayName = nameInput.getText().toString().trim();
                }
                getParentFragmentManager().beginTransaction().remove(DisplayNameFragment.this).commit();
            }
        });
        return view;
    }
}