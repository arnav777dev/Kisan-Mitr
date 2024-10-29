package com.example.soiltest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class FarmerAdapter extends ArrayAdapter<Farmer> {
    private final Context context;
    private final List<Farmer> farmers;

    public FarmerAdapter(Context context, List<Farmer> farmers) {
        super(context, R.layout.list_item_farmer, farmers);
        this.context = context;
        this.farmers = farmers;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Inflate the custom layout if convertView is null
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.list_item_farmer, parent, false);
        }

        // Get the current farmer
        Farmer currentFarmer = farmers.get(position);

        // Get references to the TextViews in the custom layout
        TextView farmerNameTextView = convertView.findViewById(R.id.farmer_name);
        TextView villageTextView = convertView.findViewById(R.id.village_name);
        TextView mobileTextView = convertView.findViewById(R.id.mobile_no);

        // Set the farmer's details in the TextViews
        farmerNameTextView.setText( "Name: " + currentFarmer.getName());
        villageTextView.setText("Village: " + currentFarmer.getVillage());
        mobileTextView.setText("Mobile No." + currentFarmer.getMobile());

        return convertView;
    }
}
