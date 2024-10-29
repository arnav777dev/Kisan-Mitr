package com.example.soiltest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.soiltest.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class user_info extends AppCompatActivity {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_form);

        // Find the views by ID
        final Button submit = findViewById(R.id.u_data);
        final EditText name = findViewById(R.id.name_edit);
        final EditText email = findViewById(R.id.email_edit);
        final EditText mobile = findViewById(R.id.mobile_text);
        final EditText address = findViewById(R.id.address_edit);

        // Set an OnClickListener for the submit button
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Read the values from the EditText fields
                String userName = name.getText().toString().trim();
                String userEmail = email.getText().toString().trim();
                String userMobile = mobile.getText().toString().trim();
                String userAddress = address.getText().toString().trim();

                boolean valid = true;

                // Input validation for empty fields
                if (userName.isEmpty()) {
                    name.setError("Please fill your name");
                    valid = false;
                }

                if (userEmail.isEmpty()) {
                    email.setError("Please fill your email");
                    valid = false;
                } else if (!isValidEmail(userEmail)) {
                    email.setError("Please enter a valid email");
                    valid = false;
                }

                if (userMobile.isEmpty()) {
                    mobile.setError("Please fill your mobile number");
                    valid = false;
                } else if (!isValidMobile(userMobile)) {
                    mobile.setError("Please enter a valid 10-digit mobile number");
                    valid = false;
                }

                if (userAddress.isEmpty()) {
                    address.setError("Please fill your address");
                    valid = false;
                }

                // If all fields are valid, proceed with user creation
                if (valid) {
                    // Create a new User object if validation is successful
                    User newUser = new User(userName, userEmail, userMobile, userAddress, user.getUid());
                    newUser.registerUser();

                    // Navigate to the dashboard activity
                    Intent intent = new Intent(user_info.this, dashboard.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    // Helper method to validate email format
    private boolean isValidEmail(String email) {
        String gmailRegex = "^[A-Za-z0-9._%+-]+@gmail\\.com$";
        return email.matches(gmailRegex);
    }

    // Helper method to validate mobile number format (checking for 10 digits)
    private boolean isValidMobile(String mobile) {
        return mobile.length() == 10 && mobile.matches("\\d+");  // Ensures it's exactly 10 digits and numeric
    }
}
