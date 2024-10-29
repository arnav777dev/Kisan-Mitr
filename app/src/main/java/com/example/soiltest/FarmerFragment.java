package com.example.soiltest;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
public class FarmerFragment extends Fragment {

    private ListView listView;
    private FarmerAdapter farmerAdapter;
    private List<Farmer> farmerList;
    private List<Farmer> filteredList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private EditText getFarmer;
    private ProgressBar progressBar; // Add ProgressBar variable

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.farmer_box, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize ListView, EditText, and ProgressBar
        listView = view.findViewById(R.id.list_farmer);
        getFarmer = view.findViewById(R.id.get_farmer);
        progressBar = view.findViewById(R.id.progressBar); // Initialize ProgressBar

        farmerList = new ArrayList<>();
        filteredList = new ArrayList<>();
        farmerAdapter = new FarmerAdapter(getActivity(), filteredList);
        listView.setAdapter(farmerAdapter);

        // Fetch and populate farmers
        fetchFarmersFromFirestore();

        // Set up search functionality
        getFarmer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence query, int i, int i1, int i2) {
                filterFarmers(query.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        // Set item click listener for the ListView
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Farmer clickedFarmer = filteredList.get(position);
                String farmerUID = clickedFarmer.getName() + "_" + clickedFarmer.getMobile().substring(0, 4);

                Bundle bundle = new Bundle();
                bundle.putString("farmerUID", farmerUID);
                bundle.putString("farmerName", clickedFarmer.getName());

                FieldFragment fieldFragment = new FieldFragment();
                fieldFragment.setArguments(bundle);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frag_box, fieldFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        Button addFarmer = view.findViewById(R.id.add_farmer);
        addFarmer.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frag_box, new addFarmer())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }

    private void fetchFarmersFromFirestore() {
        String userId = mAuth.getUid();
        if (userId != null) {
            progressBar.setVisibility(View.VISIBLE); // Show ProgressBar
            db.collection("Users").document(userId).collection("Farmers")
                    .get()
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE); // Hide ProgressBar after fetching data
                        if (task.isSuccessful()) {
                            farmerList.clear();
                            for (DocumentSnapshot document : task.getResult()) {
                                String farmerName = document.getString("name");
                                String village = document.getString("village");
                                String mobile = document.getString("mobile");
                                if (farmerName != null && village != null && mobile != null) {
                                    farmerList.add(new Farmer(farmerName, village, mobile));
                                }
                            }
                            filteredList.addAll(farmerList);
                            farmerAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(getActivity(), "Error getting farmers: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(getActivity(), "User not authenticated.", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterFarmers(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(farmerList);
        } else {
            for (Farmer farmer : farmerList) {
                if (farmer.getName().toLowerCase().contains(query.toLowerCase()) ||
                        farmer.getMobile().contains(query) ||
                        farmer.getVillage().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(farmer);
                }
            }
        }
        farmerAdapter.notifyDataSetChanged();
    }
}
