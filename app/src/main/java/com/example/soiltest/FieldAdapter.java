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

public class FieldAdapter extends ArrayAdapter<Field> {

    public FieldAdapter(@NonNull Context context, @NonNull List<Field> fields) {
        super(context, 0, fields);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get the Field object for this position
        Field field = getItem(position);

        // Check if the existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_field, parent, false);
        }

        // Lookup view for data population
        TextView fieldName = convertView.findViewById(R.id.field_name_list);
        TextView nitrogen = convertView.findViewById(R.id.N_value_list);
        TextView phosphorous = convertView.findViewById(R.id.P_value_list);
        TextView potassium = convertView.findViewById(R.id.K_value_list);
        TextView date = convertView.findViewById(R.id.date_npk);

        // Use a default value if field is null or its attributes are null
        if (field != null) {
            fieldName.setText(field.getName() != null ? "Khet: " + field.getName() : "N/A");
            nitrogen.setText(field.getN_value() != null ? field.getN_value() : "0");
            phosphorous.setText(field.getP_value() != null ? field.getP_value() : "0");
            potassium.setText(field.getK_value() != null ? field.getK_value() : "0");
            date.setText(field.getDate() != null ? field.getDate() : "N/A");
        } else {
            fieldName.setText("Unknown Field");
            nitrogen.setText("0");
            phosphorous.setText("0");
            potassium.setText("0");
            date.setText("N/A");
        }

        // Return the completed view to render on screen
        return convertView;
    }
}
