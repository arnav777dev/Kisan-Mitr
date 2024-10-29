package com.example.soiltest;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.soiltest.sensor_reading.DataActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class addField extends Fragment {
    private String farmerUID;
    private String farmerName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.add_field, container, false);
        if(getArguments() != null){
            farmerUID = getArguments().getString("farmerUID");;
        }
//        Log.d(TAG, "farmerUID: " + farmerUID);

        EditText name = view.findViewById(R.id.field_name); // EditText for field name
        Button submit = view.findViewById(R.id.u_data); // Button to submit

        submit.setOnClickListener(v -> {
            String fieldName = name.getText().toString();

            // Proceed with form submission
            addFieldInUser(fieldName);
        });

        return view;
    }

    void addFieldInUser(String name) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        String userId = auth.getUid();

        if (userId != null) {
            // Create a new field object to store
            Field field = new Field(name, farmerUID, "0", "0", "0", "");
            String fieldUID = field.getName() + "_" + field.getFarmerUID();
//            Log.d(TAG, "userId: " + userId);
//            Log.d(TAG, "farmerUID: " + farmerUID);
//            Log.d(TAG, "fieldUID: " + fieldUID);

            db.collection("Users").document(userId)
                    .collection("Farmers").document(farmerUID).collection("Fields").document(fieldUID)
                    .set(field)
                    .addOnSuccessListener(documentReference -> {
                        // Successfully added field
                        Toast.makeText(getActivity(), "Field added successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getActivity(), DataActivity.class);
                        intent.putExtra("farmerUID", farmerUID);
                        intent.putExtra("fieldUID", fieldUID);
                        intent.putExtra("name", name);
                        intent.putExtra("farmerName", farmerName);
                        onBackPress();
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        // Handle the error
                        Toast.makeText(getActivity(), "Error adding field: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Error adding field: " + e.getMessage());
                    });
        } else {
            Toast.makeText(getActivity(), "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }

    void onBackPress() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

}
