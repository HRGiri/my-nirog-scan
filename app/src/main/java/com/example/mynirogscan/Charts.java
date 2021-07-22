package com.example.mynirogscan;

import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;

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
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

public class Charts {

    private static final String TAG = "Charts";
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

    final int[] colorArray = {
            Color.rgb(192, 255, 140),
            Color.rgb(255, 208, 140),
            Color.rgb(255, 247, 140),
            Color.rgb(255, 80, 90)
    };

    public void extract_reading_history(Map<Number,Map<String,Number>> readings, List<Entry> oxygen_entries ,List<Entry> temperature_entries ,List<Entry> heartrate_entries){

            timestamp_string = new HashMap<Float, String>();
            Object[] timestamp_keyset = readings.keySet().toArray();
            int readings_length = readings.keySet().size();
            if(readings_length > 0){
                if(readings_length > 50) readings_length = 50;
                Number time_diff =  (long)timestamp_keyset[0] - (long)timestamp_keyset[readings_length-1];
                SimpleDateFormat dateformat;
                if ((long) time_diff < (long) (3600 * 24 * 1000)) {
                    dateformat = new SimpleDateFormat("HH:mm");
                } else {
                    dateformat = new SimpleDateFormat("dd-MM HH:mm");
                }
                for (float counter = 0f;counter < readings_length;counter++) {
                    Number timestamp = (Number)timestamp_keyset[readings_length-1-(int)counter];
                    Date date = new Date((long) timestamp);
                    dateformat.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));
                    String xaxis_timestamp = dateformat.format(date);

                    timestamp_string.put((float) counter, xaxis_timestamp);
                    oxygen_entries.add(new Entry((float) counter, readings.get(timestamp).get("oxygen").floatValue()));
                    temperature_entries.add(new Entry((float) counter, readings.get(timestamp).get("temperature").floatValue()));
                    heartrate_entries.add(new Entry((float) counter, readings.get(timestamp).get("heartrate").floatValue()));
                }
                Log.d(TAG,timestamp_string.toString());
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


//        xAxis.setTextSize(text_size);
//        xAxis.setTextColor(txt_color);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);



        lineChart.getAxisRight().setEnabled(false);
        YAxis yAxis = lineChart.getAxisLeft();
//        yAxis.setTextColor(ColorTemplate.getHoloBlue());
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

        pieChart.setDrawHoleEnabled(false);
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
        l.setTextColor(Color.BLACK);
        // entry label styling
        pieChart.setDrawEntryLabels(false);
//        pieChart.setEntryLabelColor(label_color);
//        pieChart.setEntryLabelTextSize(12f);

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

        barChart.getAxisRight().setEnabled(false);
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
        l.setEnabled(true);
    }

    public void populateBarChart(BarChart barChart, List<BarEntry> barentries ,String label,Map <Float,String> xvalue){

        BarDataSet data_set = new BarDataSet(barentries,label);
        data_set.setColors(colorArray);
        data_set.setValueTextColor(Color.BLUE);

        Log.d("CHARTS","x value : "+xvalue.toString());
        XAxis xAxis = barChart.getXAxis();
        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                String label;
                if(value < 0) label = "";
                else if(value > xvalue.size()-1) label = "";
                else label = xvalue.get(value);
                return label;
            }
        };
        xAxis.setLabelCount(xvalue.size());
        xAxis.setValueFormatter(formatter);

        BarData data = new BarData(data_set);
        data.setBarWidth(0.3f);
        barChart.setVisibleXRangeMaximum(50);
        barChart.setFitBars(true);
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
        setData.setColor(highlight_colors);
        setData.setHighLightColor(Color.rgb(244, 117, 117));
//        setData.setHighLightColor(highlight_colors);

        ValueFormatter formatter = new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                String label;
                if(value < 0) label = "";
                else if(value >= timestamp_string.size()) label = "";
                else label = timestamp_string.get(value);
                return label;
            }
        };
//        lineChart.setVisibleXRange(0f,50f);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setGranularity(1f);
//        xAxis.setLabelCount(timestamp_string.size(),true);
        xAxis.setValueFormatter(formatter);

        LineData data = new LineData(setData);
        data.setDrawValues(false);
        data.setValueTextColor(text_color);
        data.setValueTextSize(9f);

        lineChart.setOnChartGestureListener(new OnChartGestureListener() {
            @Override
            public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

            }

            @Override
            public void onChartLongPressed(MotionEvent me) {

            }

            @Override
            public void onChartDoubleTapped(MotionEvent me) {

            }

            @Override
            public void onChartSingleTapped(MotionEvent me) {

            }

            @Override
            public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

            }

            @Override
            public void onChartScale(MotionEvent me, float scaleX, float scaleY) {
                if((scaleX > 1.5) || (scaleY > 1.5)){
                    lineChart.getLineData().setDrawValues(true);
                }
                else {
                    lineChart.getLineData().setDrawValues(false);
                }
            }

            @Override
            public void onChartTranslate(MotionEvent me, float dX, float dY) {

            }
        });
        lineChart.setData(data);
        lineChart.invalidate();

    }

    public void populatepiechart(PieChart piechart, ArrayList<PieEntry> pieentries, String label, int text_color){

        PieDataSet piedataset = new PieDataSet(pieentries,label);

        ArrayList<Integer> colors = new ArrayList<>();

//        for (int c : ColorTemplate.VORDIPLOM_COLORS)
//            colors.add(c);

//        for (int c : ColorTemplate.JOYFUL_COLORS)
//            colors.add(c);

//        for (int c : ColorTemplate.COLORFUL_COLORS)
//            colors.add(c);

//        for (int c : ColorTemplate.LIBERTY_COLORS)
//            colors.add(c);

//        for (int c : ColorTemplate.PASTEL_COLORS)
//            colors.add(c);
//        int[] colorArray = new int[]{Color.GREEN,Color.YELLOW, ColorTemplate.rgb("ff8800"),Color.RED};
        for(int c : colorArray)
            colors.add(c);

        piedataset.setDrawIcons(false);
        piedataset.setSliceSpace(3f);
        piedataset.setSelectionShift(5f);
        piedataset.setColors(colors);
        piedataset.setValueTextColor(text_color);


        PieData data = new PieData(piedataset);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(11f);
        data.setValueTextColor(text_color);


        piechart.setData(data);
        piechart.invalidate();
    }


}
