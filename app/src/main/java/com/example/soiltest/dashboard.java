package com.example.soiltest;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.soiltest.data.model.User;
import com.example.soiltest.sensor_reading.DevicesFragment;
import com.example.soiltest.ui.login.LoginActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class dashboard extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private ListView sideMenuList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dashboard);

        // Initialize Firestore and FirebaseAuth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout);

        // Handle profile picture click to open the side sheet
        ImageView profileImage = findViewById(R.id.profile_image);
        profileImage.setOnClickListener(v -> {
            // Open the drawer when the profile picture is clicked
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Initialize the side menu
        setupSideMenu();
        // Load the initial fragment if this is the first creation
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frag_box, new FarmerFragment()) // Load initial fragment
                    .addToBackStack(null)
                    .commit();
        }


    }


    private void setupSideMenu() {
        // Initialize ListView
        sideMenuList = findViewById(R.id.side_menu_list);

        // Initialize AtomicReferences for user data
        AtomicReference<String> address = new AtomicReference<>("India");
        AtomicReference<String> email = new AtomicReference<>("xyz@example.com");
        AtomicReference<String> mobile = new AtomicReference<>("1234567890");
        AtomicReference<String> name = new AtomicReference<>("Ratan");

        // Get the current user's ID
        String userId = mAuth.getUid(); // Make sure mAuth is initialized

        // List to hold menu items
        List<String> menuItems = new ArrayList<>();

        // Retrieve user data from Firestore
        if (userId != null) {
            db.collection("Users")
                    .document(userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                // Convert Firestore document to User object
                                User user = document.toObject(User.class);

                                // Update AtomicReferences with user data
                                if (user != null) {
                                    name.set(user.getName());
                                    email.set(user.getEmail());
                                    mobile.set(user.getMobile());
                                    address.set(user.getAddress());

                                    // Log the fetched user data for debugging
                                    Log.d(TAG, "User data fetched: " + user.toString());
                                } else {
                                    Log.d(TAG, "User object is null");
                                }
                            } else {
                                Log.d(TAG, "No such document");
                            }
                        } else {
                            Log.e(TAG, "Get failed with ", task.getException());
                        }

                        // Update menuItems after Firestore data is retrieved
                        menuItems.add(name.get());
                        menuItems.add(email.get());
                        menuItems.add(mobile.get());
                        menuItems.add(address.get());
                        menuItems.add("Log out");

                        TextView head = findViewById(R.id.heading);
                        head.setText("Welcome, " + name.get() + "!");

                        // Use custom layout for the adapter
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                dashboard.this,
                                R.layout.list_item, // Use the custom layout you created for list items
                                R.id.list_item_text, // The TextView ID in the custom layout
                                menuItems
                        );

                        sideMenuList.setAdapter(adapter);
                    });
        } else {
            // Handle case where userId is null
            Log.d(TAG, "User ID is null");
        }

        // Handle click events on ListView items
        sideMenuList.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0:
                    // Handle Profile click
//                    Toast.makeText(dashboard.this, "Profile clicked", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    // Handle Settings click
//                    Toast.makeText(dashboard.this, "Settings clicked", Toast.LENGTH_SHORT).show();
                    break;
                case 4:
                    // Handle Log out click
                    mAuth.signOut();
                    Intent intent = new Intent(dashboard.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                    break;
            }

            // Close the drawer after an item is clicked
            drawerLayout.closeDrawer(GravityCompat.START);
        });
    }
    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Check if there is only one fragment in the back stack
        if (fragmentManager.getBackStackEntryCount() == 1) {
            finish();
        }

        // If there are any fragments in the back stack, pop the last one
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            // If no fragments are in the back stack, finish the activity
            super.onBackPressed();
        }
    }



}
