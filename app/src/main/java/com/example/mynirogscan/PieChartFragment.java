package com.example.mynirogscan;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieEntry;

import com.example.mynirogscan.PieChartStateAdapter.PieChartTypes;

import java.util.ArrayList;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PieChartFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PieChartFragment extends Fragment {

    public static final String ARG_PIE_CHART = "PIE_CHART";
    private PieChart pieChart;
    private Charts charts = new Charts();
    private int total_reads;
    private GlobalData globalData;
    private Map<Number, Map<String, Number>> all_readings_sorted;
    private PieChartTypes pieChartType;

    public PieChartFragment() {
        // Required empty public constructor
    }

    public static PieChartFragment newInstance() {
        return new PieChartFragment();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pie_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        pieChart = view.findViewById(R.id.reusable_pie_chart);
        charts.setupPieChart(pieChart, android.R.color.darker_gray);
        pieChartType = PieChartTypes.values()[args.getInt(ARG_PIE_CHART)];

        globalData = new ViewModelProvider(requireActivity()).get(GlobalData.class);
        globalData.getIsInit().observe(requireActivity(),isInit->{
            if(isInit){
                globalData.getAllReadingsSorted().observe(requireActivity(),sortedReadings->{
                    all_readings_sorted = sortedReadings;
                    populatePieChart();
                });
            }
        });
    }

    private void populatePieChart(){

        total_reads = all_readings_sorted.size();
        charts.calculate_healthy_people(all_readings_sorted);

        ArrayList<PieEntry> pie_chart_entries = new ArrayList<>();
        String label = "";

        switch (pieChartType) {
            case Company:
                pie_chart_entries.add(new PieEntry(((float) (charts.green_band) / total_reads) * 100f, "Healthy"));
                pie_chart_entries.add(new PieEntry(((float) (charts.yellow_band) / total_reads) * 100f, "Unfit"));
                pie_chart_entries.add(new PieEntry(((float) (charts.orange_band) / total_reads) * 100f, "Ill"));
                pie_chart_entries.add(new PieEntry(((float) (charts.red_band) / total_reads) * 100f, "Dead"));
                label = "Company Health";
                break;

            case Spo:
                pie_chart_entries.add(new PieEntry(((float) (total_reads - charts.bad_oxy_count) / total_reads) * 100f, "Healthy"));
                pie_chart_entries.add(new PieEntry(((float) (charts.bad_oxy_count) / total_reads) * 100f, " Unhealthy"));
                label = "SPO2";
                break;

            case Temperature:
                pie_chart_entries.add(new PieEntry(((float) (total_reads - charts.bad_temp_count) / total_reads) * 100f, "Healthy"));
                pie_chart_entries.add(new PieEntry(((float) (charts.bad_temp_count) / total_reads) * 100f, " Unhealthy"));
                label = "Temperature";
                break;

            case HeartRate:
                pie_chart_entries.add(new PieEntry(((float) (total_reads - charts.bad_hr_count) / total_reads) * 100f, "Healthy"));
                pie_chart_entries.add(new PieEntry(((float) (charts.bad_hr_count) / total_reads) * 100f, " Unhealthy"));
                label = "Heart Rate";
                break;

            default:
                break;
        }

        charts.populatepiechart(pieChart,pie_chart_entries,label, android.R.color.black);
    }
}