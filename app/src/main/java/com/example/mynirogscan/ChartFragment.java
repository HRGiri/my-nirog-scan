package com.example.mynirogscan;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.mynirogscan.ChartStateAdapter.ChartType;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;

import com.example.mynirogscan.ChartStateAdapter.ChartDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChartFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChartFragment extends Fragment {

    public static final String ARG_PIE_CHART = "PIE_CHART";
    public static final String ARG_LINE_CHART = "LINE_CHART";
    public static final String ARG_CHART_TYPE = "CHART_TYPE";
    public static final String ARG_DEVICE_ID = "DEVICE_ID";

    private String deviceID;

    private PieChart pieChart;
    private Charts charts = new Charts();
    private int total_reads;
    private GlobalData globalData;
    private Map<Number, Map<String, Number>> all_readings_sorted;
    private ChartDataType chartDataType;
    private LineChart lineChart;

    public ChartFragment() {
        // Required empty public constructor
    }

    public static ChartFragment newInstance() {
        return new ChartFragment();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        pieChart = view.findViewById(R.id.reusable_pie_chart);
        lineChart = view.findViewById(R.id.reusable_line_chart);

        deviceID = args.getString(ARG_DEVICE_ID);

        ChartType chartType = ChartType.values()[args.getInt(ARG_CHART_TYPE)];
        if(chartType == ChartType.PieChart) {
            pieChart.setVisibility(View.VISIBLE);
            chartDataType = ChartDataType.values()[args.getInt(ARG_PIE_CHART)];
            charts.setupPieChart(pieChart, Color.WHITE);
        }
        else if(chartType == ChartType.LineChart){
            lineChart.setVisibility(View.VISIBLE);
            chartDataType = ChartDataType.values()[args.getInt(ARG_LINE_CHART)];
            float yAxis_max, yaxis_min;
            int color;
            switch (chartDataType){
                case Spo:
                    yAxis_max = 100f;
                    yaxis_min = 75f;
                    color = android.R.color.secondary_text_light;
                    break;
                case Temperature:
                    yAxis_max = 110f;
                    yaxis_min = 90f;
                    color = android.R.color.holo_purple;
                    break;
                case HeartRate:
                    yAxis_max = 180f;
                    yaxis_min = 20f;
                    color = android.R.color.holo_orange_light;
                    break;
                default:
                    yAxis_max = 100f;
                    yaxis_min = 0f;
                    color = android.R.color.primary_text_light;
                    break;
            }
            charts.setupLineChart(lineChart, Color.TRANSPARENT,11f,color,yAxis_max,yaxis_min);
        }

        globalData = new ViewModelProvider(requireActivity()).get(GlobalData.class);
        globalData.getIsInit().observe(requireActivity(),isInit->{
            if(isInit){
                globalData.getAllReadingsSorted().observe(requireActivity(),sortedReadings->{
                    if(deviceID == null)
                        all_readings_sorted = sortedReadings;
                    else
                        all_readings_sorted = globalData.getDeviceReadingsSorted(deviceID);
                    if(all_readings_sorted.size() > 0) {
                        if (chartType == ChartType.PieChart)
                            populatePieChart();
                        else if (chartType == ChartType.LineChart)
                            populateLineChart();
                    }
                });
            }
        });
    }

    private void populateLineChart() {
        List<Entry> oxygen_entries = new ArrayList<Entry>();
        List<Entry> temperature_entries = new ArrayList<Entry>();
        List<Entry> heartrate_entries = new ArrayList<Entry>();
        charts.extract_reading_history(all_readings_sorted,oxygen_entries,temperature_entries,heartrate_entries);
        if(!(oxygen_entries.isEmpty() || temperature_entries.isEmpty() || temperature_entries.isEmpty())) {

            switch (chartDataType) {
                case Spo:
                    charts.populatelinechart(lineChart, oxygen_entries, "Oxygen", Color.GREEN, Color.WHITE);
                    break;
                case Temperature:
                    charts.populatelinechart(lineChart, temperature_entries, "Temperature", Color.BLUE, Color.WHITE);
                    break;
                case HeartRate:
                    charts.populatelinechart(lineChart, heartrate_entries, "Heart Rate", Color.RED, Color.WHITE);
                    break;
            }
        }



    }

    private void populatePieChart(){

        total_reads = all_readings_sorted.size();
        charts.calculate_healthy_people(all_readings_sorted);

        ArrayList<PieEntry> pie_chart_entries = new ArrayList<>();
        String label = "";

        switch (chartDataType) {
            case Company:
                pie_chart_entries.add(new PieEntry(((float) (charts.green_band) / total_reads) * 100f, "Healthy"));
                pie_chart_entries.add(new PieEntry(((float) (charts.orange_band) / total_reads) * 100f, "Ill"));
                pie_chart_entries.add(new PieEntry(((float) (charts.yellow_band) / total_reads) * 100f, "Unfit"));
                pie_chart_entries.add(new PieEntry(((float) (charts.red_band) / total_reads) * 100f, "Dead"));

                label = "Company Health";
                break;

            case Spo:
                pie_chart_entries.add(new PieEntry(((float) (total_reads - charts.bad_oxy_count) / total_reads) * 100f, "Healthy"));
                pie_chart_entries.add(new PieEntry(((float) (charts.bad_oxy_count) / total_reads) * 100f, "Unhealthy"));
                label = "SPO2";
                break;

            case Temperature:
                pie_chart_entries.add(new PieEntry(((float) (total_reads - charts.bad_temp_count) / total_reads) * 100f, "Healthy"));
                pie_chart_entries.add(new PieEntry(((float) (charts.bad_temp_count) / total_reads) * 100f, "Unhealthy"));
                label = "Temperature";
                break;

            case HeartRate:
                pie_chart_entries.add(new PieEntry(((float) (total_reads - charts.bad_hr_count) / total_reads) * 100f, "Healthy"));
                pie_chart_entries.add(new PieEntry(((float) (charts.bad_hr_count) / total_reads) * 100f, "Unhealthy"));
                label = "Heart Rate";
                break;

            default:
                break;
        }

        charts.populatepiechart(pieChart,pie_chart_entries,label, Color.BLUE);
    }
}