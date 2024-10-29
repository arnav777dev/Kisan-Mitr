package com.example.soiltest.sensor_reading;


import static android.content.ContentValues.TAG;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import android.widget.Button;

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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReadingUI extends Fragment {

    private String farmerUID;
    private String fieldUID;
    private String name;
    private String farmerName;


    private enum Connected {False, Pending, True}

    private Connected connected = Connected.False;

    private final BroadcastReceiver broadcastReceiver;
    private int deviceId, portNum, baudRate;
    private UsbSerialPort usbSerialPort;
    private String newline = TextUtil.newline_crlf;
    private int[] npk = new int[3];
    View sendBtn;
    private boolean isBlinking = false;
    private final Handler blinkHandler = new Handler(Looper.getMainLooper());
    FirebaseFirestore db;
    FirebaseAuth user;

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.data_collector, container, false);

        TextView farmer_name = view.findViewById(R.id.farmer_name);
        farmer_name.setText(farmerName);
        TextView field_name = view.findViewById(R.id.field_name);
        field_name.setText(name);
        sendBtn = view.findViewById(R.id.get_data);
        sendBtn.setOnClickListener(v -> collectSample());
        return view;
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

        connect(); // Call your connect method
    }

//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        disconnect(); // Clean up resources
//        if (broadcastReceiver != null) {
//            getActivity().unregisterReceiver(broadcastReceiver);
//        }
//    }

    // Collect sample data
    private void collectSample() {
        Handler handler = new Handler(Looper.getMainLooper());
        ProgressBar progressBar = getActivity().findViewById(R.id.progress_bar);
        progressBar.setMax(30); // Set max to the total iterations
        progressBar.setProgress(0);
        startBlinkingText(); // Start blinking text effect

        for (int i = 0; i < 30; i++) {
            final int index = i; // Need final index for the handler
            handler.postDelayed(() -> {
                try {
                    send(index);
                    progressBar.setProgress(index + 1);
                } catch (IOException e) {
                    Toast.makeText(getActivity(), "Failed", Toast.LENGTH_SHORT).show();
                }
            }, index * 2000); // 500ms delay for each iteration
        }

        // Enable the button and stop blinking text after data collection is finished
        handler.postDelayed(() -> {
            stopBlinkingText(); // Stop blinking text effect
            progressBar.setProgress(30);
        }, 30 * 2000); // The total time of the collection process
    }

    // USB connection management
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

        if (permissionGranted == null) {
            Toast.makeText(getActivity(), "Wait a Second", Toast.LENGTH_SHORT).show();
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

    // Status method for logging and UI feedback
    private void status(String str) {
        Log.d("HomeFragment", "Status method called with: " + str); // Logging status
        if (getView() != null) {
            SpannableStringBuilder spn = new SpannableStringBuilder(str);
            Toast.makeText(getActivity(), spn, Toast.LENGTH_SHORT).show();
        }
    }

    // Sending data
    private void send(int ind) throws IOException {
        if (connected != Connected.True) {
            Toast.makeText(getActivity(), "Not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hex strings to be sent
        String[] hexStrings = {
                "01 03 00 1E 00 01 E4 0C", // N
                "01 03 00 1F 00 01 B5 CC", // P
                "01 03 00 20 00 01 85 C0"  // K
        };

        Handler handler = new Handler(Looper.getMainLooper());
        for (int i = 0; i < hexStrings.length; i++) {
            final String str = hexStrings[i];
            int finalI = i;

            // Schedule each send with a delay
            handler.postDelayed(() -> {
                try {
                    byte[] data = TextUtil.fromHexString(str);
                    writeData(data);
                    byte[] response = new byte[10];
                    int len = usbSerialPort.read(response, 90); // Placeholder for receiving data; replace with actual receiving logic
                    onNewData(response, finalI);
                    Log.d(TAG, String.valueOf(len));

                    // Check if all readings have been collected
                    if (ind == 29) {
                        sendDatatoFirebase(npk);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, 100); // 100ms interval for each string
        }
    }

    // Handle received data
    public void onNewData(byte[] data, int i) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        spn.append(TextUtil.toHexString(data)).append('\n');

        String hexValue = String.valueOf(spn).substring(12, 14);
        int decimalValue = Integer.parseInt(hexValue, 16);
        npk[i] = decimalValue;

        if (i == 2) {
            updateTextView(npk);
        }
    }

    // Update the UI with NPK values
    private void updateTextView(int[] npk) {
        Handler handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(() -> {
            TextView nValueTextView = getActivity().findViewById(R.id.N_value);
            TextView pValueTextView = getActivity().findViewById(R.id.P_value);
            TextView kValueTextView = getActivity().findViewById(R.id.K_value);

            setTextWithFade(nValueTextView, npk[0]);
            setTextWithFade(pValueTextView, npk[1]);
            setTextWithFade(kValueTextView, npk[2]);

            Log.d("HexToDecimal", "Updated NPK values: N=" + npk[0] + ", P=" + npk[1] + ", K=" + npk[2]);

            // Repeat the update if still collecting
            if (!sendBtn.isEnabled()) {
                updateTextView(npk); // Repeat every 2 seconds until collection ends


            }
        }, 2000); // 2000ms delay for every update (2 seconds)
    }

    // Send data to Firebase
    private void sendDatatoFirebase(int[] npk) {
        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance();

        String UID = user.getUid();



        // Get the current date and time as a string for the document ID
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy"); // Set the format pattern
        String currentDate = formatter.format(date);
        // Create a map to store the NPK values
        Map<String, Object> npkData = new HashMap<>();
        npkData.put("n_value", String.valueOf(npk[0]));
        npkData.put("p_value", String.valueOf(npk[1]));
        npkData.put("k_value", String.valueOf(npk[2]));
        npkData.put("name", name);
        npkData.put("date", currentDate);


        db.collection("Users").document(UID)
                .collection("Farmers").document(farmerUID)
                .collection("Fields").document(fieldUID)
                .set(npkData) // Use set() instead of add()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firebase", "NPK values successfully written with ID: " + currentDate);
                })
                .addOnFailureListener(e -> {
                    Log.w("Firebase", "Error writing document", e);
                });
    }



    private void setTextWithFade(TextView textView, int value) {
        textView.setText(String.valueOf(value));

        // Create a fade-in animation
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(textView, "alpha", 0f, 1f);
        fadeIn.setDuration(500); // Duration of the fade effect in milliseconds

        // Start the fade-in animation
        fadeIn.start();
    }

    // Writing data to USB serial
    private void writeData(byte[] data) throws IOException {
        if (connected != Connected.True) {
            throw new IOException("Not connected");
        }
        usbSerialPort.write(data, 1000); // Timeout for writing
        Log.d(TAG, data.toString());
    }

    private void startBlinkingText() {
        isBlinking = true;
        blinkButtonText(); // Start the blinking effect
    }

    private void stopBlinkingText() {
        isBlinking = false;
        blinkHandler.removeCallbacksAndMessages(null); // Stop the blinking handler
        ((Button) sendBtn).setText("Start Collecting"); // Reset the button text
    }

    private void blinkButtonText() {
        blinkHandler.postDelayed(() -> {
            if (!isBlinking) return; // Stop if blinking is disabled

            Button button = (Button) sendBtn;
            if ("Collecting...".equals(button.getText().toString())) {
                button.setText(" "); // Hide text to create a blink effect
            } else {
                button.setText("Collecting...");
            }

            blinkButtonText(); // Schedule the next blink
        }, 500); // 500ms interval for the blink effect
    }

    private OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            disconnect();
            requireActivity().getSupportFragmentManager().popBackStack(); // Navigate back
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


}
