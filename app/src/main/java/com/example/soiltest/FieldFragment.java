package com.example.soiltest;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.soiltest.sensor_reading.DataActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FieldFragment extends Fragment {

    private ListView listView;
    private FieldAdapter fieldAdapter; // Custom adapter for Field objects
    private List<Field> fieldList; // List to hold Field objects
    private List<Field> filteredList; // List for filtered results
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private EditText getField; // EditText for search input
    private String farmerUID; // Farmer UID passed from previous fragment
    private String farmerName;
    Button addfield;
    private ProgressBar progressBar;

    // Maintain a list of selected items
    List<Field> selectedFields = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.field_box, container, false);

        // Get farmerUID from the arguments
        if (getArguments() != null) {
            farmerUID = getArguments().getString("farmerUID");
            farmerName = getArguments().getString("farmerName");
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        progressBar = view.findViewById(R.id.overlay);

        // Fetch and populate fields
        fetchFieldsFromFirestore();

        // Initialize ListView and adapter
        listView = view.findViewById(R.id.list_fields);
        getField = view.findViewById(R.id.get_field);
        addfield = view.findViewById(R.id.add_field);
        fieldList = new ArrayList<>();
        filteredList = new ArrayList<>(); // Initialize filtered list
        fieldAdapter = new FieldAdapter(getActivity(), filteredList); // Use filtered list for the adapter
        listView.setAdapter(fieldAdapter);


        addField AddField = new addField();
        Bundle bundle = new Bundle();
        bundle.putString("farmerUID", farmerUID);
        bundle.putString("farmerName", farmerName );
        AddField.setArguments(bundle);

        addfield.setOnClickListener(v -> {
            // Handle the button click
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frag_box, AddField)
                    .addToBackStack(null)
                    .commit();
        });


        // Set up search functionality
        getField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence query, int i, int i1, int i2) {
                filterFields(query.toString()); // Call filter method
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });


        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the clicked field
                Field clickedField = filteredList.get(position);

                // Toggle selection
                if (selectedFields.contains(clickedField)) {
                    selectedFields.remove(clickedField); // Deselect if already selected
                    view.setBackgroundColor(Color.TRANSPARENT); // Reset background color
                } else {
                    selectedFields.add(clickedField); // Select
                    view.setBackgroundColor(Color.LTGRAY); // Change background color for selected item
                }

                // Show sharing options if any fields are selected
                if (!selectedFields.isEmpty()) {
                    AlertDialog dialog = new AlertDialog.Builder(requireContext())
                            .setTitle("Share Selected Fields")
                            .setMessage("Do you want to share the selected fields?")
                            .setPositiveButton("Share", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    shareSelectedFields();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .create();

// Set button text colors to ensure visibility in both dark and light modes
                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialogInterface) {
                            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

                            // Set color for both dark and light modes (replace with your color resources)
                            positiveButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                            negativeButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                        }
                    });

                    dialog.show();

                }

                // Return true to indicate that the long click was handled
                return true;
            }
        });



        // Set item click listener for the ListView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Field clickedField = filteredList.get(position);
                String fieldUID = clickedField.getName() + "_" + farmerUID; // Assuming field name is unique

                String name = clickedField.getName();
                Intent intent = new Intent(getActivity(), DataActivity.class);
                intent.putExtra("farmerUID", farmerUID);
                intent.putExtra("fieldUID", fieldUID);
                intent.putExtra("name", name);
                intent.putExtra("farmerName", farmerName);

                startActivity(intent);

            }
        });

        return view;
    }

    private void shareSelectedFields() {
        if (selectedFields.isEmpty()) {
            Toast.makeText(requireContext(), "No fields selected to share", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder shareMessage = new StringBuilder();

        for (Field field : selectedFields) {
            shareMessage.append("Field Name: ").append(field.getName()).append("\n")
                    .append("Date: ").append(field.getDate()).append("\n\n");

            shareMessage.append("Nutrient          |   Actual Value | Ideal Range\n")
                    .append(String.format("Nitrogen (N):       %-5s mg/kg  (100–200)\n", field.getN_value()))
                    .append(String.format("Phosphorus (P): %-5s mg/kg  (25–50)\n", field.getP_value()))
                    .append(String.format("Potassium (K):    %-5s mg/kg  (100–150)\n", field.getK_value()));
            shareMessage.append("\n--------------------------\n"); // Separator for readability

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage.toString());
            startActivity(Intent.createChooser(shareIntent, "Share via"));

            // Clear the selection after sharing
            selectedFields.clear();
            fieldAdapter.notifyDataSetChanged();
        }
    }




    private void fetchFieldsFromFirestore() {
        String userId = mAuth.getUid();
        if (userId != null) {
            progressBar.setVisibility(View.VISIBLE);
            db.collection("Users").document(userId)
                    .collection("Farmers").document(farmerUID).collection("Fields")
                    .get()
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            fieldList.clear(); // Clear the list before adding new data
                            for (DocumentSnapshot document : task.getResult()) {
                                String fieldName = document.getString("name");
                                String N_value = document.getString("n_value");
                                String K_value = document.getString("k_value");
                                String P_value = document.getString("p_value");
                                String date = document.getString("date");


                                N_value = (N_value == null || N_value.isEmpty()) ? "0" : N_value;
                                K_value = (K_value == null || K_value.isEmpty()) ? "0" : K_value;
                                P_value = (P_value == null || P_value.isEmpty()) ? "0" : P_value;
                                date = (date == null || date.isEmpty()) ? "" : "     " + date;

                                if (fieldName != null) {
                                    // Create a Field object and add it to the list
                                    fieldList.add(new Field(fieldName, farmerUID, N_value, P_value, K_value, date));
                                }
//                                Log.d("FieldFragment", "Number of fields fetched: " + fieldList.size());


                            }
                            // Initially populate the filtered list with all fields
                            filteredList.addAll(fieldList);
                            fieldAdapter.notifyDataSetChanged(); // Notify the adapter about the data change
                        } else {
                            // Handle the error
                            Toast.makeText(getActivity(), "Error getting fields: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getActivity(), "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterFields(String query) {
        filteredList.clear(); // Clear the filtered list
        if (query.isEmpty()) {
            filteredList.addAll(fieldList); // If query is empty, show all fields
        } else {
            for (Field field : fieldList) {
                // Check if the field's name or crop type matches the query (case-insensitive)
                if (field.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(field); // Add matching field to filtered list
                }
            }
        }
        fieldAdapter.notifyDataSetChanged(); // Notify the adapter about the data change
    }


}
