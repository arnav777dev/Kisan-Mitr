package com.example.soiltest.data.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class User {
    private String name;
    private String email;
    private String mobile;
    private String address;
    private String uid;

    private static User instance; // Singleton instance
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_NAME = "name";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_MOBILE = "mobile";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_UID = "uid";

    public User() {
        // Required empty constructor
    }
    // Private constructor to prevent instantiation from other classes
    public User(String name, String email, String mobile, String address, String uid) {
        this.name = name;
        this.email = email;
        this.mobile = mobile;
        this.address = address;
        this.uid = uid;
    }

    // Public method to get the singleton instance
    public static User getInstance(String name, String email, String mobile, String address, String uid) {
        if (instance == null) {
            instance = new User(name, email, mobile, address, uid);
        }
        return instance;
    }

    // Method to register the user in Firestore
    public void registerUser() {
        HashMap<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("mobile", mobile);
        userData.put("address", address);
        userData.put("uid", uid);

        db.collection("Users")
                .document(uid)  // Use 'uid' as the document ID
                .set(userData) // Use set() to explicitly define the document ID
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "User registered with ID: " + uid);
                })
                .addOnFailureListener(e -> {
                    Log.w("Firebase", "Error adding user", e);
                });

    }
    // Method to clear user data from SharedPreferences
    public static void clearPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear(); // This will remove all stored data
        editor.apply();

        // Optionally, set the instance to null if you want to reset the singleton
        instance = null;
    }



    // Optional: Add getter methods for the fields if needed
    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getMobile() {
        return mobile;
    }

    public String getAddress() {
        return address;
    }

    public String getUid() {
        return uid;
    }
}
