package com.example.soiltest;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;
import java.util.UUID;

public class addFarmer extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.add_farmer, container, false);

        EditText name = view.findViewById(R.id.name_edit);
        EditText village = view.findViewById(R.id.address_edit);
        EditText mobile = view.findViewById(R.id.mobile_text);
        Button submit = view.findViewById(R.id.u_data);

        submit.setOnClickListener(v -> {
            String farmerName = name.getText().toString();
            String farmerVillage = village.getText().toString();
            String mobileNumber = mobile.getText().toString();

            if (isValidMobile(mobileNumber)) {
                // Proceed with form submission
                addFarmerinUser(farmerName, farmerVillage, mobileNumber);

            } else {
                mobile.setError("Invalid mobile number. Please enter a valid 10-digit number.");
            }
        });

        return view;
    }

    void addFarmerinUser(String name, String village, String mobile) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

        String userId = auth.getUid();

        if (userId != null) {

            // Generate a custom UID using UUID

            // Create a new farmer object to store
            Farmer farmer = new Farmer(name, village, mobile);
            String farmerUID = farmer.getName() + "_" + farmer.getMobile().substring(0, 4);

            // Add the farmer with a custom UID
            db.collection("Users").document(userId).collection("Farmers").document(farmerUID)
                    .set(farmer)
                    .addOnSuccessListener(documentReference -> {
                        // Successfully added farmer
                        Toast.makeText(getActivity(), "Farmer added successfully!", Toast.LENGTH_SHORT).show();

                        Bundle bundle = new Bundle();
                        bundle.putString("farmerUID", farmerUID); // Add the farmerUID to the bundle
                        bundle.putString("farmerName", name);

                        // Create an instance of the next fragment and set the arguments
                        FieldFragment fieldFragment = new FieldFragment();
                        fieldFragment.setArguments(bundle); // Set the bundle as arguments

                        // Navigate to the FieldFragment
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .replace(R.id.frag_box, fieldFragment)
//                                .addToBackStack(null)
                                .commit();
                    })
                    .addOnFailureListener(e -> {
                        // Handle the error
                        Toast.makeText(getActivity(), "Error adding farmer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getActivity(), "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }




    private boolean isValidMobile(String mobileNumber) {
        // Check if the mobile number is exactly 10 digits long
        return mobileNumber.matches("\\d{10}");
    }
}
