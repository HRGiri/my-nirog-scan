package com.example.mynirogscan;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;
import com.google.type.DateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ReadingsAdapter extends RecyclerView.Adapter<ReadingsAdapter.ViewHolder> {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView timeTextView;
        private final TextView nameTextView;
        private final TextView tempTextView;
        private final TextView spoTextView;
        private final TextView hrTextView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            timeTextView = (TextView) view.findViewById(R.id.tv_readings_date);
            nameTextView = (TextView) view.findViewById(R.id.tv_readings_device_name);
            tempTextView = (TextView) view.findViewById(R.id.tv_readings_temperature);
            spoTextView = (TextView) view.findViewById(R.id.tv_readings_spo);
            hrTextView = (TextView) view.findViewById(R.id.tv_readings_hr);
        }

        public TextView getTimeTextView() {
            return timeTextView;
        }
        public TextView getNameTextView() {
            return nameTextView;
        }
        public TextView getTempTextView() {
            return tempTextView;
        }
        public TextView getSpoTextView() {
            return spoTextView;
        }
        public TextView getHrTextView() {
            return hrTextView;
        }
    }

    private ArrayList<Map<String,Number>> localData;

    public ReadingsAdapter(ArrayList<Map<String, Number>> data){
        localData = data;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.readings_list_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SimpleDateFormat dateformat;
        Number timestamp = localData.get(position).get("timestamp");
        Number time_diff =  Calendar.getInstance().getTimeInMillis() - (long)timestamp;
        if((long)time_diff < (long)(3600*24*1000)){
            dateformat = new SimpleDateFormat("HH:mm");
        }else{
            dateformat = new SimpleDateFormat("dd-MM HH:mm");
        }
            Date date = new Date((long)timestamp);
            dateformat.setTimeZone(TimeZone.getTimeZone("GMT+5:30"));
            String str_timestamp = dateformat.format(date);

            holder.getTimeTextView().setText(str_timestamp);
//            holder.getNameTextView().setText(localData.get(position).get("uuid") + "");
            holder.getSpoTextView().setText(String.format("%.1f %%",localData.get(position).get(Constants.OXYGEN_FIELD_NAME).floatValue()));
            holder.getTempTextView().setText(String.format("%.1f \u2109",localData.get(position).get(Constants.TEMPERATURE_FIELD_NAME).floatValue()));
            holder.getHrTextView().setText(String.format("%d bpm",localData.get(position).get(Constants.HEART_RATE_FIELD_NAME).intValue()));
    }

    @Override
    public int getItemCount() {
        return localData.size();
    }
}
