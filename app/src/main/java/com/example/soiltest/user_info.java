package com.example.soiltest.user_info;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.soiltest.R;
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

                // Input validation (optional but recommended)
                User newUser = null;
                if (userName.isEmpty() || userEmail.isEmpty() || userMobile.isEmpty() || userAddress.isEmpty()) {
                    // Display a message if any field is empty
                    Toast.makeText(user_info.this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
                } else {
                    // Create a new User object with the collected data
                    newUser = new User(userName, userEmail, userMobile, userAddress, user.getUid());
                }
                newUser.register_user();



            }
        });
    }
}
