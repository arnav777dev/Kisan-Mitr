package com.example.soiltest.ui.login;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.soiltest.dashboard;
import com.example.soiltest.data.model.User;
import com.example.soiltest.R;
import com.example.soiltest.databinding.ActivityLoginBinding;
import com.example.soiltest.user_info;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.atomic.AtomicReference;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private ActivityLoginBinding binding;
    private FirebaseUser user;
    private boolean isLoginMode = true;  // Flag to toggle between login and sign-up mode
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        // Initialize Firebase Auth
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Replace with your web client ID
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final EditText email_id = binding.username;
        final EditText password = binding.password;
        final Button actionButton = findViewById(R.id.action_button);  // One button for both login and sign-up
        final ImageButton google_sign_in = findViewById(R.id.google_sign_in);
        final ProgressBar loadingProgressBar = binding.loading;

        // Set initial button text for login
        actionButton.setText("Log In");

        // Toggle between login and sign-up mode
        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show the progress bar
                loadingProgressBar.setVisibility(View.VISIBLE);

                // Get email and password input
                String email = email_id.getText().toString().trim();
                String pwd = password.getText().toString().trim();

                // Validate email and password
                if (!isValidEmail(email)) {
                    // Invalid email, show error message
                    email_id.setError("Enter a valid email address");
                    loadingProgressBar.setVisibility(View.GONE);  // Hide the progress bar
                    return;
                }

                if (pwd.length() < 5) {
                    // Invalid password length, show error message
                    password.setError("Password must be at least 5 characters");
                    loadingProgressBar.setVisibility(View.GONE);  // Hide the progress bar
                    return;
                }

                // Proceed based on login or sign-up mode
                if (isLoginMode) {
                    // Log in
                    login(email, pwd);
                } else {
                    // Sign-up
                    signup(email, pwd);
                }
            }
        });

        // Toggle between modes when the text below is clicked
        TextView toggleMode = findViewById(R.id.toggle_mode);
        toggleMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isLoginMode = !isLoginMode;  // Toggle the flag
                if (isLoginMode) {
                    actionButton.setText("Log In");
                    toggleMode.setText("Don't have an account? Sign Up");
                } else {
                    actionButton.setText("Sign Up");
                    toggleMode.setText("Already have an account? Log In");
                }
            }
        });

        // Google sign-in button click
        google_sign_in.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                signInWithGoogle();
            }
        });
    }

    // Helper method to validate email format
    private boolean isValidEmail(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Google Sign-In method
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result from Google Sign-In Intent
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(LoginActivity.this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Authenticate Google Sign-In with Firebase
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "signInWithCredential:success");
                            updateUiWithUser(new LoggedInUserView(user.getEmail()));
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUiWithUser(new LoggedInUserView(currentUser.getEmail()));
        }
    }

    // Firebase login logic
    private void login(String email, String password) {
        binding.loading.setVisibility(View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.loading.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Sign in success
                        user = mAuth.getCurrentUser();
                        Log.d(TAG, "signInWithEmail:success");
                        if (user != null) {
                            updateUiWithUser(new LoggedInUserView(user.getEmail()));
                        }
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        showLoginFailed(R.string.login_failed);
                    }
                });
    }

    // Firebase sign-up logic
    private void signup(String email, String password) {
        binding.loading.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    binding.loading.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        // Sign-up success
                        user = mAuth.getCurrentUser();
                        Log.d(TAG, "createUserWithEmail:success");
                        if (user != null) {
                            updateUiWithUser(new LoggedInUserView(user.getEmail()));
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        showLoginFailed(R.string.signup_failed);
                    }
                });
    }

    // Update UI on successful login/sign-up
    private void updateUiWithUser(LoggedInUserView model) {
        if (isLoginMode) {
            // Check Firestore for user details
            String userId = mAuth.getUid();
            if (userId != null) {
                db.collection("Users").document(userId).get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document != null && document.exists()) {
                                    // User details exist, navigate to dashboard
                                    Intent intent = new Intent(LoginActivity.this, dashboard.class);
                                    startActivity(intent);
                                    finish();  // Close the LoginActivity
                                } else {
                                    // User details do not exist, navigate to user_info
                                    Intent intent = new Intent(LoginActivity.this, user_info.class);
                                    startActivity(intent);
                                    finish();  // Close the LoginActivity
                                }
                            } else {
                                Log.e(TAG, "Get failed with ", task.getException());
                                Toast.makeText(LoginActivity.this, "Error retrieving user data.", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                // Handle case where userId is null
                Log.d(TAG, "User ID is null");
            }
        } else {
            // For sign-up, directly navigate to dashboard
            Intent intent = new Intent(LoginActivity.this, dashboard.class);
            startActivity(intent);
            finish();  // Close the LoginActivity
        }

        // Show welcome message
        String welcome = getString(R.string.welcome) + "Kisan Mitr";
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }



    // Show login/sign-up failure message
    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

}
