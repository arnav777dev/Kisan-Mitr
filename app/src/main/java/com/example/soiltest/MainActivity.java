package com.example.soiltest;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.soiltest.sensor_reading.DataActivity;
import com.example.soiltest.ui.login.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // Set the content view to your splash screen layout

        // Enable EdgeToEdge (if needed for your design)
        EdgeToEdge.enable(this);

        // Check if the user is already logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentUser != null) {
                    // User is already logged in, go to the home screen or main part of the app
                    Intent intent = new Intent(MainActivity.this, dashboard.class);
                    startActivity(intent);
                } else {
                    // User is not logged in, go to the LoginActivity
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
                finish();  // Close the splash screen activity
            }
        }, 1000);  // 2000 milliseconds delay
    }


}
