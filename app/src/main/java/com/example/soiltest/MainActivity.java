package com.example.soiltest;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.soiltest.Data_Collection.DataActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // Make sure you set the content view to your layout

        // Enable EdgeToEdge
        EdgeToEdge.enable(this);

        // Find the button by ID
        Button buttonToDataActivity = findViewById(R.id.btn1);

        // Set a click listener to navigate to DataActivity
        buttonToDataActivity.setOnClickListener(v -> {
            // Create an Intent to navigate to DataActivity
            Intent intent = new Intent(MainActivity.this, DataActivity.class);
            startActivity(intent);  // Start the DataActivity
        });
    }
}
