package com.example.mynirogscan;

import android.graphics.Color;
import android.util.Log;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class Charts {

    public int bad_oxy_count = 0;
    public int bad_temp_count = 0;
    public int bad_hr_count = 0;

    public int green_band = 0;
    public int yellow_band = 0;
    public int orange_band = 0;
    public int red_band =0;

    final float MIN_OXY_LIM = 93f;
    final float MIN_HEARTRATE = 40f;
    final float MAX_HEARTRATE = 100f;
    final float MAX_TEMPERATURE = 99f;
    final float MIN_TEMPERATURE = 97f;
    Map<Float,String> timestamp_string;

    public void extract_reading_history(Map<Number,Map<String,Number>> readings, List<Entry> oxygen_entries ,List<Entry> temperature_entries ,List<Entry> heartrate_entries){

        timestamp_string = new HashMap<Float, String>();
        Object[] timestamp_keyset = readings.keySet().toArray();
        Number time_diff =  (long)timestamp_keyset[0] - (long)timestamp_keyset[49];
        SimpleDateFormat dateformat;
        if((long)time_diff < (long)(3600*24*1000)){
            dateformat = new SimpleDateFormat("HH:mm");
        }else{
            dateformat = new SimpleDateFormat("dd-MM HH:mm");
        }
        for (float counter = 0f;counter < 50f;counter++) {
            Number timestamp = (Number)timestamp_keyset[49-(int)counter];
            Date date = new Date((long)timestamp);
            dateformat.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));
            String xaxis_timestamp = dateformat.format(date);

            timestamp_string.put((float)counter,xaxis_timestamp);
            oxygen_entries.add(new Entry((float)counter, readings.get(timestamp).get("oxygen").floatValue()));
            temperature_entries.add(new Entry((float)counter, readings.get(timestamp).get("temperature").floatValue()));
            heartrate_entries.add(new Entry((float)counter, readings.get(timestamp).get("heartrate").floatValue()));
        }

    }

    public int check_threshold(float o2val,float tempval, float hrval){
        int red_flags = 0;
        if(o2val < MIN_OXY_LIM){
            bad_oxy_count++;
            red_flags++;
        }
        if(!(tempval > MIN_TEMPERATURE && tempval < MAX_TEMPERATURE)){
            bad_temp_count++;
            red_flags++;
        }
        if(!(hrval > MIN_HEARTRATE && hrval < MAX_HEARTRATE)){
            bad_hr_count++;
            red_flags++;
        }
        return red_flags;
    }

    public void calculate_healthy_people(Map<Number,Map<String,Number>> readings){
        
        green_band = orange_band = yellow_band = red_band = bad_oxy_count = bad_temp_count = 0;
        for(Number ind_timestamp : readings.keySet() ){
            int red_flags = 0;
            float oxygen_val = readings.get(ind_timestamp).get("oxygen").floatValue();
            float temp_val = readings.get(ind_timestamp).get("temperature").floatValue();
            float hr_val = readings.get(ind_timestamp).get("heartrate").floatValue();
            red_flags = check_threshold(oxygen_val,temp_val,hr_val);
            switch (red_flags){
                case 0 :
                    green_band++;
                    break;
                case 1:
                    yellow_band++;
                    break;
                case 2:
                    orange_band++;
                    break;
                case 3:
                    red_band++;
                    break;
            }
        }
    }

    public Map<String, float[]> calculate_daily_healthy_people(Map<Number,Map<String,Number>> readings){

        green_band = orange_band = yellow_band = red_band = bad_oxy_count = bad_temp_count = 0;
        Set<Number> timestamps = readings.keySet();
        Map<String, float[]> entry = new HashMap<String,float[]>();
        SimpleDateFormat format_w_date = new SimpleDateFormat("dd/MM");

        for(Number timestamp : timestamps) {
            Date curr_date = new Date((long) timestamp);
            format_w_date.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));
            String date_string = format_w_date.format(curr_date);
            float oxygen_val = readings.get(timestamp).get("oxygen").floatValue();
            float temp_val = readings.get(timestamp).get("temperature").floatValue();
            float hr_val = readings.get(timestamp).get("heartrate").floatValue();

            int red_flags = check_threshold(oxygen_val, temp_val, hr_val);

            float[] report = {0, 0, 0, 0}; //0 All healthy ; 1 minor issue; 2 something serious; 3 dead
            float healthy = 0f;
            float less_healthy = 0f;
            float minor_issue = 0f;
            float serious = 0f;
            if (entry.containsKey(date_string)) {
                healthy = entry.get(date_string)[0];
                less_healthy = entry.get(date_string)[1];
                minor_issue = entry.get(date_string)[2];
                serious = entry.get(date_string)[3];
            }
            switch (red_flags) {
                case 0:
                    healthy = healthy + 1;
                    break;
                case 1:
                    less_healthy = less_healthy + 1;
                    break;
                case 2:
                    minor_issue = minor_issue + 1;
                    break;
                case 3:
                    serious = serious + 1;
                    break;
            }
            report[0] = healthy;
            report[1] = less_healthy;
            report[2] = minor_issue;
            report[3] = serious;
            entry.put(date_string,report);
        }
        return entry;
    }

    public void setupLineChart(LineChart lineChart,int bg_color,float text_size,int txt_color,float yaxis_max,float yaxis_min){
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerDragEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setBackgroundColor(bg_color);
        XAxis xAxis = lineChart.getXAxis();
        lineChart.animateX(1500);


        xAxis.setTextSize(text_size);
        xAxis.setTextColor(txt_color);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);


        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setTextColor(ColorTemplate.getHoloBlue());
        yAxis.setAxisMaximum(yaxis_max);
        yAxis.setAxisMinimum(yaxis_min);
        yAxis.setDrawGridLines(true);
        yAxis.setGranularityEnabled(true);
    }

    public void setupPieChart(PieChart pieChart,int label_color){
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 5, 5, 5);


        pieChart.setDragDecelerationFrictionCoef(0.95f);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);

        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);

        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(61f);

        pieChart.setDrawCenterText(true);

        pieChart.setRotationAngle(0);
        // enable rotation of the chart by touch
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);


        pieChart.animateY(1400, Easing.EaseInOutQuad);

        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.VERTICAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);

        // entry label styling
        pieChart.setEntryLabelColor(label_color);
        pieChart.setEntryLabelTextSize(12f);

    }

    public void setupBarChart(BarChart barChart){

        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);

        barChart.getDescription().setEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        leftAxis.setSpaceTop(15f);

        Legend l = barChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setForm(Legend.LegendForm.SQUARE);
        l.setFormSize(9f);
        l.setTextSize(11f);
        l.setXEntrySpace(4f);
    }

    public void populateBarChart(BarChart barChart, List<BarEntry> barentries ,String label){

        BarDataSet data_set = new BarDataSet(barentries,label);
        BarData data = new BarData(data_set);
        data.setBarWidth(0.9f);
        barChart.setData(data);
        barChart.invalidate();
    }

    public void populatelinechart(LineChart lineChart, List<Entry> lineentries, String label, int highlight_colors, int text_color){

        LineDataSet setData = new LineDataSet(lineentries,label);
        setData.setAxisDependency(YAxis.AxisDependency.LEFT);
        setData.setLineWidth(1f);
        setData.setDrawCircles(false);
        setData.setFillAlpha(65);
        setData.setFillColor(ColorTemplate.getHoloBlue());
        setData.setHighLightColor(Color.rgb(244, 117, 117));


        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return timestamp_string.get(value);
            }
        };

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(formatter);


        LineData data = new LineData(setData);
        data.setValueTextColor(text_color);
        data.setValueTextSize(9f);

        lineChart.setData(data);

    }

    public void populatepiechart(PieChart piechart, ArrayList<PieEntry> pieentries, String label, int text_color){

        PieDataSet piedataset = new PieDataSet(pieentries,label);

        ArrayList<Integer> colors = new ArrayList<>();

        for (int c : ColorTemplate.VORDIPLOM_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.JOYFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.COLORFUL_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.LIBERTY_COLORS)
            colors.add(c);

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        colors.add(ColorTemplate.getHoloBlue());

        piedataset.setDrawIcons(false);
        piedataset.setSliceSpace(3f);
        piedataset.setSelectionShift(5f);
        piedataset.setColors(colors);

        PieData data = new PieData(piedataset);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(text_color);

        piechart.setData(data);
    }


}
