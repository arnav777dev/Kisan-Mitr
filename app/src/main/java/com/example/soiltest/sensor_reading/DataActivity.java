package com.example.soiltest.sensor_reading;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import com.example.soiltest.R;


public class DataActivity extends AppCompatActivity implements FragmentManager.OnBackStackChangedListener{

    private String farmerUID;
    private String fieldUID;
    private String name;
    private String farmerName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);
        Log.d("DataActivity", "onCreate called");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Retrieve intent extras
        if (getIntent() != null) {
            farmerUID = getIntent().getStringExtra("farmerUID");
            fieldUID = getIntent().getStringExtra("fieldUID");
            name = getIntent().getStringExtra("name");
            farmerName = getIntent().getStringExtra("farmerName");
        }

        // Prepare fragment arguments
        Bundle bundle = new Bundle();
        bundle.putString("farmerUID", farmerUID);
        bundle.putString("fieldUID", fieldUID);
        bundle.putString("name", name);
        bundle.putString("farmerName", farmerName);
        if (savedInstanceState == null) {
            DevicesFragment devicesFragment = new DevicesFragment();
            devicesFragment.setArguments(bundle);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment, devicesFragment, "devices")
                    .commit();
        }
        // Register back stack listener to control the toolbar back button
        getSupportFragmentManager().addOnBackStackChangedListener(this);
    }

    public void onBackStackChanged() {
        // Update the toolbar's back button based on the back stack count
        getSupportActionBar().setDisplayHomeAsUpEnabled(getSupportFragmentManager().getBackStackEntryCount() > 0);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Handle back button in toolbar
        return true;
    }

    @Override
    public void onBackPressed() {
        // Check if there are any fragments in the back stack
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack(); // Pop the top fragment

        } else {
            super.onBackPressed(); // Exit activity if no fragments are left
            finish();
        }
    }
}
