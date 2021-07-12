package com.example.mynirogscan;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ChartStateAdapter extends FragmentStateAdapter {

    public enum ChartDataType {
        Company,
        Spo,
        HeartRate,
        Temperature
    }
    public enum ChartType{
        PieChart(0),        LineChart(1);
        private final int value;
        private ChartType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private ChartType chartType;
    private int NUM_PAGES;
    public ChartStateAdapter(@NonNull Fragment fragment, ChartType chartType) {
        super(fragment);
        this.chartType = chartType;
        if(chartType == ChartType.PieChart)
            NUM_PAGES = 4;
        else
            NUM_PAGES = 3;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Return a NEW fragment instance in createFragment(int)
        Fragment fragment = new ChartFragment();
        Bundle args = new Bundle();
        args.putInt(ChartFragment.ARG_CHART_TYPE,chartType.getValue());
        if(chartType == ChartType.PieChart)
            args.putInt(ChartFragment.ARG_PIE_CHART, position);
        else if(chartType == ChartType.LineChart)
            args.putInt(ChartFragment.ARG_LINE_CHART,position+1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
}
