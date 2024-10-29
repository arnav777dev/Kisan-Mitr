package com.example.soiltest;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
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
                // Get the clicked farmer
                Field clickedField = filteredList.get(position);
                String fieldUID = clickedField.getName() + "_" + farmerUID;

                AlertDialog dialog = new AlertDialog.Builder(requireContext()) // Ensure you're in a Fragment
                        .setTitle("Delete Field")
                        .setMessage("Are you sure you want to delete this field?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Step 1: Remove the item from your local list
                                filteredList.remove(position);

                                fieldAdapter.notifyDataSetChanged();

                                mAuth = FirebaseAuth.getInstance();
                                String UID = mAuth.getUid();

                                FirebaseFirestore db = FirebaseFirestore.getInstance();

                                db.collection("Users").document(UID).collection("Farmers").document(farmerUID)
                                        .delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d("Firestore", "Document successfully deleted!");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.w("Firestore", "Error deleting document", e);
                                            }
                                        });
                            }
                        })
                        .setNegativeButton("No", null)
                        .create(); // Use create() to build the AlertDialog
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        // Customize the button text color
                        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE)
                                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white)); // Replace with your color resource
                        ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE)
                                .setTextColor(ContextCompat.getColor(requireContext(), R.color.white)); // Replace with your color resource
                    }
                });

// Show the dialog after setting the OnShowListener
                dialog.show();



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
                                date = (date == null || date.isEmpty()) ? "" : date;

                                if (fieldName != null) {
                                    // Create a Field object and add it to the list
                                    fieldList.add(new Field(fieldName, farmerUID, N_value, K_value, P_value, date));
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
