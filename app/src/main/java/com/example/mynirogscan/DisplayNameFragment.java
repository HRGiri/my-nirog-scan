package com.example.mynirogscan;

import android.os.Bundle;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

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
public class DisplayNameFragment extends DialogFragment {

    public interface DisplayNameDialogListener {
        void onSubmitDisplayName(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    DisplayNameDialogListener listener;

    public static final String TAG = "DisplayNameFragment";
    AddDeviceFragment parentFragment;

    public DisplayNameFragment(){
        // Required empty constructor
    }
    public DisplayNameFragment(Fragment parent) {
        parentFragment = (AddDeviceFragment) parent;
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (DisplayNameDialogListener) parent;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException("Must implement SettingsDialogListener");
        }
    }
    public static DisplayNameFragment newInstance() {
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
                if(parentFragment != null) {
                    parentFragment.displayName = nameInput.getText().toString().trim();
                }
                listener.onSubmitDisplayName(DisplayNameFragment.this);
                dismiss();
//                getParentFragmentManager().beginTransaction().remove(DisplayNameFragment.this).commit();
            }
        });
        return view;
    }
}