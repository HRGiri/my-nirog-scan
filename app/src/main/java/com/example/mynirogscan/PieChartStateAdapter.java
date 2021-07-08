package com.example.mynirogscan;

import android.os.Bundle;

import com.example.mynirogscan.PieChartFragment;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class PieChartStateAdapter extends FragmentStateAdapter {

    public enum PieChartTypes {
        Company,
        Spo,
        HeartRate,
        Temperature
    }
    public PieChartStateAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return a NEW fragment instance in createFragment(int)
        Fragment fragment = new PieChartFragment();
        Bundle args = new Bundle();
        // Our object is just an integer :-P
        args.putInt(PieChartFragment.ARG_PIE_CHART, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
