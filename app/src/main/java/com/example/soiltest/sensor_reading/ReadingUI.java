package com.example.soiltest.sensor_reading;


import android.animation.ObjectAnimator;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.soiltest.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReadingUI extends Fragment {

    private String farmerUID;
    private String fieldUID;
    private String name;
    private String farmerName;

    private enum Connected {False, Pending, True}
    private Connected connected = Connected.False;
    private BroadcastReceiver broadcastReceiver;
    private int deviceId, portNum, baudRate;
    private UsbSerialPort usbSerialPort;
    View sendBtn;

    private TextView topLeftTextView, topRightTextView, centerTextView, bottomLeftTextView, bottomRightTextView, farmerNameTextView, fieldTextView;
    ProgressBar progressBar;


    private int[][] npk = new int[5][3];  // Each subarray holds N, P, K values for the five regions
    private int[] npkAverages = new int[3];  // Store averaged N, P, K values
    //     0  1  2
    //     n  p  k


    // 0 - top left
    // 1 - top right
    // 2 - bottom left
    // 3 - bottom right
    // 4 - center


    public ReadingUI() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Constants.INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    connect(granted);
                }
            }
        };
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve arguments safely
        if (getArguments() != null) {
            deviceId = getArguments().getInt("device");
            portNum = getArguments().getInt("port");
            baudRate = getArguments().getInt("baud");
            farmerUID = getArguments().getString("farmerUID");
            fieldUID = getArguments().getString("fieldUID");
            name = getArguments().getString("name");
            farmerName = getArguments().getString("farmerName");

        }
    }


    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(Constants.INTENT_ACTION_GRANT_USB);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above, specify RECEIVER_NOT_EXPORTED
            getActivity().registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            // For versions below Android 13, no need for the flag
            getActivity().registerReceiver(broadcastReceiver, filter);
        }

        connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect(); // Clean up resources
        // Unregister receiver only if it is registered
        if (broadcastReceiver != null) {
            try {
                getActivity().unregisterReceiver(broadcastReceiver);
                broadcastReceiver = null; // Clear reference after unregistering
            } catch (IllegalArgumentException e) {
                // Receiver was not registered, so no need to unregister
                e.printStackTrace();
            }
        }
    }

    private void connect() {
        connect(null);
    }

    private void connect(Boolean permissionGranted) {
        UsbDevice device = null;
        UsbManager usbManager = (UsbManager) getActivity().getSystemService(Context.USB_SERVICE);
        for (UsbDevice v : usbManager.getDeviceList().values()) {
            if (v.getDeviceId() == deviceId) {
                device = v;
                break; // Found the device, break the loop
            }
        }

        if (device == null) {
            status("Connection failed: device not found");
            return;
        }

        UsbSerialDriver driver = UsbSerialProber.getDefaultProber().probeDevice(device);
        if (driver == null) {
            driver = CustomProber.getCustomProber().probeDevice(device);
        }
        if (driver == null) {
            status("Connection failed: no driver for device");
            return;
        }
        if (driver.getPorts().size() <= portNum) {
            status("Connection failed: not enough ports at device");
            return;
        }

        usbSerialPort = driver.getPorts().get(portNum);
        UsbDeviceConnection usbConnection = usbManager.openDevice(driver.getDevice());

        if (usbConnection == null) {
            if (permissionGranted == null && !usbManager.hasPermission(driver.getDevice())) {
                requestUsbPermission(usbManager, driver.getDevice());
            } else {
                status(usbManager.hasPermission(driver.getDevice()) ? "Connection failed: open failed" : "Connection failed: permission denied");
            }
            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            usbSerialPort.setParameters(baudRate, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            connected = Connected.True; // Set connection state
            status("Connected");
        } catch (Exception e) {
            status("Error: " + e.getMessage());
            disconnect();
        }
    }

    private void requestUsbPermission(UsbManager usbManager, UsbDevice device) {
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_MUTABLE : 0;
        Intent intent = new Intent(Constants.INTENT_ACTION_GRANT_USB);
        intent.setPackage(getActivity().getPackageName());
        PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, flags);
        usbManager.requestPermission(device, usbPermissionIntent);
    }

    private void disconnect() {
        connected = Connected.False;
        if (usbSerialPort != null) {
            try {
                usbSerialPort.close(); // Ensure you close the port if open
            } catch (Exception e) {
                Log.e("HomeFragment", "Error closing USB serial port: " + e.getMessage());
            }
        }
        usbSerialPort = null; // Nullify reference after closing
    }

    private void status(String str) {
        if (getView() != null) {
            SpannableStringBuilder spn = new SpannableStringBuilder(str);
            Toast.makeText(getActivity(), spn, Toast.LENGTH_SHORT).show();
        }
    }

    // Writing data to USB serial
    private void writeData(byte[] data) throws IOException {
        if (connected != Connected.True) {
            throw new IOException("Not connected");
        }
        usbSerialPort.write(data, 1000);
    }

    private OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            disconnect();
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        try {
            getActivity().unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("HomeFragment", "Receiver not registered", e);
        }
        disconnect(); // Ensure you properly clean up the connection
    }

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.field_reading, container, false);



        // Initialize TextViews
        topLeftTextView = view.findViewById(R.id.top_left);
        topRightTextView = view.findViewById(R.id.top_right);
        centerTextView = view.findViewById(R.id.center);
        bottomLeftTextView = view.findViewById(R.id.bottom_left);
        bottomRightTextView = view.findViewById(R.id.bottom_right);
        farmerNameTextView = view.findViewById(R.id.farmer_name);
        fieldTextView = view.findViewById(R.id.field_name);

        farmerNameTextView.setText(farmerName);
        fieldTextView.setText(name);

        // Setup onClickListeners to collect samples for each region
        topLeftTextView.setOnClickListener(view1 -> collectSample(0));
        topRightTextView.setOnClickListener(view1 -> collectSample(1));
        centerTextView.setOnClickListener(view1 -> collectSample(4));
        bottomLeftTextView.setOnClickListener(view1 -> collectSample(2));
        bottomRightTextView.setOnClickListener(view1 -> collectSample(3));

        sendBtn = view.findViewById(R.id.get_data);
        sendBtn.setOnClickListener(v -> sendDatatoFirebase(averageNPKValues()));

        return view;
    }

    // Collect samples for a specific region
    private void collectSample(int regionIndex) {
        Handler handler = new Handler(Looper.getMainLooper());
        progressBar = getActivity().findViewById(R.id.progress_bar);
        progressBar.setMax(30);
        progressBar.setProgress(0);

        // Start blinking animation for the selected region's TextView
        startBlinkAnimation(regionIndex);

        for (int i = 0; i < 30; i++) {
            final int index = i;
            handler.postDelayed(() -> {
                try {
                    send(regionIndex);
                    progressBar.setProgress(index + 1);
                } catch (IOException e) {
                    Toast.makeText(getActivity(), "Failed to collect data", Toast.LENGTH_SHORT).show();
                }
            }, index * 500); // 2-second interval for each iteration
        }

        // Finalize data collection after 30 iterations
        handler.postDelayed(() -> {
            progressBar.setProgress(30);
            stopBlinkAnimation(regionIndex);
            setFinalBackground(regionIndex);
        }, 30 * 500);
    }

    // Start blinking animation for a region's TextView
    private void startBlinkAnimation(int regionIndex) {
        TextView regionTextView = getRegionTextView(regionIndex);
        if (regionTextView != null) {
            ObjectAnimator blinkAnimator = ObjectAnimator.ofFloat(regionTextView, "alpha", 1f, 0f);
            blinkAnimator.setDuration(500); // Blink interval
            blinkAnimator.setInterpolator(new LinearInterpolator());
            blinkAnimator.setRepeatMode(ObjectAnimator.REVERSE);
            blinkAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            blinkAnimator.start();

            regionTextView.setTag(blinkAnimator); // Save animator in tag for later use
        }
    }

    // Stop the blinking animation and reset the background
    private void stopBlinkAnimation(int regionIndex) {
        TextView regionTextView = getRegionTextView(regionIndex);
        if (regionTextView != null) {
            ObjectAnimator blinkAnimator = (ObjectAnimator) regionTextView.getTag();
            if (blinkAnimator != null) {
                blinkAnimator.cancel();
                regionTextView.setAlpha(1f); // Reset alpha
            }
        }
    }

    // Set the final background drawable after data collection is complete
    private void setFinalBackground(int regionIndex) {
        TextView regionTextView = getRegionTextView(regionIndex);
        if (regionTextView != null) {
            regionTextView.setBackgroundResource(R.drawable.pro_rectangle_field);
            regionTextView.setTextColor(getResources().getColor(R.color.white));
            progressBar.setProgress(0);
        }
    }
    // Helper method to get the TextView based on region index
    private TextView getRegionTextView(int regionIndex) {
        switch (regionIndex) {
            case 0: return topLeftTextView;
            case 1: return topRightTextView;
            case 2: return bottomLeftTextView;
            case 3: return bottomRightTextView;
            case 4: return centerTextView;
            default: return null;
        }
    }


    // Send request for each nutrient
    private void send(int regionIndex) throws IOException {
        if (connected != Connected.True) {
            Toast.makeText(getActivity(), "Not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] hexStrings = {
                "01 03 00 1E 00 01 E4 0C", // N
                "01 03 00 1F 00 01 B5 CC", // P
                "01 03 00 20 00 01 85 C0"  // K
        };

        Handler handler = new Handler(Looper.getMainLooper());
        for (int i = 0; i < hexStrings.length; i++) {
            final String str = hexStrings[i];
            int finalI = i;
            handler.postDelayed(() -> {
                try {
                    byte[] data = TextUtil.fromHexString(str);
                    writeData(data);
                    byte[] response = new byte[10];
                    int len = usbSerialPort.read(response, 90); // Adjust based on actual response logic
                    onNewData(response, regionIndex, finalI); // Pass region and nutrient index
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }, 100); // 100ms interval
        }
    }

    // Process new data and update UI in real-time
    public void onNewData(byte[] data, int regionIndex, int nutrientIndex) {
        String hexValue = TextUtil.toHexString(data).substring(12, 14);
        int decimalValue = Integer.parseInt(hexValue, 16);
        npk[regionIndex][nutrientIndex] = decimalValue;

        // Update each region's TextView with new values in real-time
        updateRegionTextView(regionIndex);
    }

    // Update UI for each region's NPK values
    private void updateRegionTextView(int regionIndex) {
        String npkText = "N: " + npk[regionIndex][0] + "\n P: " + npk[regionIndex][1] + "\n K: " + npk[regionIndex][2];

        switch (regionIndex) {
            case 0:
                topLeftTextView.setText(npkText);
                break;
            case 1:
                topRightTextView.setText(npkText);
                break;
            case 2:
                bottomLeftTextView.setText(npkText);
                break;
            case 3:
                bottomRightTextView.setText(npkText);
                break;
            case 4:
                centerTextView.setText(npkText);
                break;
        }
    }

    // Calculate average NPK values
    private int[] averageNPKValues() {
        int[] avgNpk = new int[3]; // N, P, K
        for (int i = 0; i < 5; i++) { // Sum for each region
            for (int j = 0; j < 3; j++) {
                avgNpk[j] += npk[i][j];
//                Log.d("ceck avg", npk[i][0] + " " + npk[i][1] + " " + npk[i][2]);
            }
        }
        for (int i = 0; i < 3; i++) { // Calculate averages
            avgNpk[i] /= 5;
        }
        Log.d("avg", Arrays.toString(avgNpk));
        return avgNpk;
    }

    // Send data to Firebase after averaging
    private void sendDatatoFirebase(int[] avgNpk) {

        TextView avg_readings = getActivity().findViewById(R.id.avg_readings);

        StringBuilder final_readings = new StringBuilder();
        final_readings.append("Nutrient            | Actual Value | Ideal Range\n")
                .append(String.format("Nitrogen (N):       %-5s mg/kg    (100–200)\n", avgNpk[0]))
                .append(String.format("Phosphorus (P): %-5s mg/kg    (25–50)\n", avgNpk[1]))
                .append(String.format("Potassium (K):    %-5s mg/kg    (100–150)\n", avgNpk[2]));

        avg_readings.setText(final_readings.toString());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth user = FirebaseAuth.getInstance();

        String UID = user.getUid();
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        String currentDate = formatter.format(date);

        Map<String, String> npkData = new HashMap<>();
        npkData.put("n_value", String.valueOf(avgNpk[0]));
        npkData.put("p_value", String.valueOf(avgNpk[1]));
        npkData.put("k_value", String.valueOf(avgNpk[2]));


        npkData.put("name", name);
        npkData.put("date", currentDate);

//        int byteSize = 0;
//        for (Map.Entry<String, String> entry : npkData.entrySet()) {
//            byteSize += entry.getKey().getBytes().length;   // Size of key in bytes
//            byteSize += entry.getValue().getBytes().length; // Size of value in bytes
//        }
//
//        Log.d("size in bytes", String.valueOf(byteSize));
        db.collection("Users").document(UID).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        int byteSize = 0;
                        for (Map.Entry<String, Object> entry : documentSnapshot.getData().entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();

                            // Calculate key size
                            byteSize += key.getBytes().length;

                            // Calculate value size based on its type
                            if (value instanceof String) {
                                byteSize += ((String) value).getBytes().length;
                            } else if (value instanceof Long || value instanceof Double) {
                                byteSize += 8;  // Long and Double generally take 8 bytes
                            } else if (value instanceof Boolean) {
                                byteSize += 1;  // Boolean takes 1 byte
                            } else if (value instanceof Map || value instanceof List) {
                                // You could recursively calculate size for nested maps/lists if needed
                            }
                            // Add more types as needed
                        }
                        Log.d("Document size in bytes", String.valueOf(byteSize));
                    }
                })
                .addOnFailureListener(e -> Log.e("Error fetching document", e.toString()));




        db.collection("Users").document(UID)
                .collection("Farmers").document(farmerUID)
                .collection("Fields").document(fieldUID)
                .set(npkData)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "NPK values successfully uploaded"))
                .addOnFailureListener(e -> Log.w("Firebase", "Error writing document", e));
    }


}
