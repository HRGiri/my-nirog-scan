package com.example.mynirogscan;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import java.util.HashMap;
import java.util.Map;

import static com.example.mynirogscan.Constants.*;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    private EditText etName;
    private EditText etOrganization;
    private EditText etPhone;
    private Button btnUpdate;
    private Button btnSignOut;
    private GlobalData globalData;
    private Map<String, Object> userData;

    public ProfileFragment() {
        // Required empty public constructor
    }


    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etName = view.findViewById(R.id.et_profile_name);
        etOrganization = view.findViewById(R.id.et_profile_organization);
        etPhone = view.findViewById(R.id.et_profile_phone);
        btnUpdate = view.findViewById(R.id.btn_profile_update);
        btnSignOut = view.findViewById(R.id.btn_signout);

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String,Object> map = new HashMap<>();
                String name = etName.getText().toString().trim();
                if(!name.equals(userData.get(NAME_FIELD_NAME)))
                    map.put(NAME_FIELD_NAME,name);
                name = etOrganization.getText().toString().trim();
                if(!name.equals(userData.get(ORGANIZATION_FIELD_NAME)))
                    map.put(ORGANIZATION_FIELD_NAME,name);
                name = etPhone.getText().toString().trim();
                if(!name.equals(userData.get(PHONE_FIELD_NAME)))
                    map.put(PHONE_FIELD_NAME,name);
                if(map.size()>0)
                    globalData.uploadUserData(map);
            }
        });
        btnUpdate.setEnabled(false);

        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                globalData.signOut();
                Navigation.findNavController(getActivity(),R.id.main_activity_nav_host)
                        .navigate(ProfileFragmentDirections.actionProfileFragmentToHomeFragment());
            }
        });

        globalData = new ViewModelProvider(requireActivity()).get(GlobalData.class);
        globalData.getIsInit().observe(requireActivity(),isInit->{
            if(isInit){
                globalData.getGlobalUserData().observe(requireActivity(),userData->{
                    this.userData = userData;
                    etName.setText("" + userData.get(NAME_FIELD_NAME));
                    etOrganization.setText("" + userData.get(ORGANIZATION_FIELD_NAME));
                    etPhone.setText("" + userData.get(PHONE_FIELD_NAME));
                    btnUpdate.setEnabled(true);
                });
            }
        });
    }
}