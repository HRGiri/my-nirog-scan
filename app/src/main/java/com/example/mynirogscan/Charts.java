package com.example.mynirogscan;

import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.utils.ColorTemplate;

public class Charts {

    public void setupLineChart(LineChart lineChart){
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerDragEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setBackgroundColor(Color.TRANSPARENT);
        XAxis xAxis = lineChart.getXAxis();
        lineChart.animateX(1500);


        xAxis.setTextSize(11f);
        xAxis.setTextColor(Color.MAGENTA);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setTextColor(ColorTemplate.getHoloBlue());
        yAxis.setAxisMaximum(100f);
        yAxis.setAxisMinimum(75f);
        yAxis.setDrawGridLines(true);
        yAxis.setGranularityEnabled(true);

    }
}
