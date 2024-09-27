package com.example.soiltest;

import static com.example.soiltest.TextUtil.toHexString;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hoho.android.usbserial.driver.SerialTimeoutException;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;

public class HomeFragment extends Fragment {

    private enum Connected {False, Pending, True}

    private Connected connected = Connected.False;
    private final Handler mainLooper;
    private final BroadcastReceiver broadcastReceiver;
    private int deviceId, portNum, baudRate;
    private UsbSerialPort usbSerialPort;
    private String newline = TextUtil.newline_crlf;
    int[] npk = new int[3];

    public HomeFragment() {
        mainLooper = new Handler(Looper.getMainLooper());
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Constants.INTENT_ACTION_GRANT_USB.equals(intent.getAction())) {
                    Boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
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
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_page, container, false);
        View sendBtn = view.findViewById(R.id.get_data);
        sendBtn.setOnClickListener(v -> {
            collectSample();
        });
        return view;
    }
    private Handler handler = new Handler(Looper.getMainLooper());
    public void collectSample() {
        // Start the loop with a delay
        for (int i = 0; i < 20; i++) {
            final int index = i; // Need final index for the handler
            handler.postDelayed(() -> {
                try {
                    send();
                } catch (IOException e) {
                    Toast.makeText(getActivity(), "Failed", Toast.LENGTH_SHORT).show();
                }
            }, index * 500); // 100ms delay for each iteration
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // Connect to the device
        connect();
    }

    void status(String str) {
        Log.d("HomeFragment", "Status method called with: " + str); // Logging status
        View view = getView();
        if (view != null) {
            TextView fieldNameValue = view.findViewById(R.id.field_name_value);
            SpannableStringBuilder spn = new SpannableStringBuilder(str);
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Update UI on the main thread
            mainLooper.post(() -> {
                fieldNameValue.append(spn);
            });
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
        if (usbConnection == null && permissionGranted == null && !usbManager.hasPermission(driver.getDevice())) {
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_MUTABLE : 0;
            Intent intent = new Intent(Constants.INTENT_ACTION_GRANT_USB);
            intent.setPackage(getActivity().getPackageName());
            PendingIntent usbPermissionIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, flags);
            usbManager.requestPermission(driver.getDevice(), usbPermissionIntent);
            return;
        }
        if (usbConnection == null) {
            status(usbManager.hasPermission(driver.getDevice()) ? "Connection failed: open failed" : "Connection failed: permission denied");
            return;
        }

        try {
            usbSerialPort.open(usbConnection);
            try {
                usbSerialPort.setParameters(baudRate, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                connected = Connected.True; // Set connection state
                status("Connected");
            } catch (UnsupportedOperationException e) {
                status("Setting serial parameters failed: " + e.getMessage());
            }
        } catch (Exception e) {
            status("Error: " + e.getMessage());
            disconnect();
        }
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


    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect(); // Clean up resources
        // Unregister the broadcast receiver if registered
        if (broadcastReceiver != null) {
            getActivity().unregisterReceiver(broadcastReceiver);
        }
    }


    private void send() throws IOException {
        if (connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hex strings to be sent
        String[] hexStrings = {
                "01 03 00 1E 00 01 E4 0C",
                "01 03 00 1F 00 01 B5 CC",
                "01 03 00 20 00 01 85 C0"
        };

        // Handler for scheduling the sends
        Handler handler = new Handler(Looper.getMainLooper());
        for (int i = 0; i < hexStrings.length; i++) {
            final String str = hexStrings[i];

            // Schedule each send with a delay
            int finalI = i;
            handler.postDelayed(() -> {
                String msg;
                byte[] data;
                boolean hexEnabled = true;

                if (hexEnabled) {
                    StringBuilder sb = new StringBuilder();
                    toHexString(sb, TextUtil.fromHexString(str));
                    msg = sb.toString();
                    data = TextUtil.fromHexString(msg);

                } else {
                    msg = str;
                    data = (str + newline).getBytes();
                }

                try {
                    write_data(data);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // Wait for a response after sending each data (assuming you have a way to get response)
                // This should be handled in your data receiving logic, e.g., through a callback
                byte[] response = new byte[10];
                try {
                    int len  = usbSerialPort.read(response, 90); // Placeholder for receiving data; replace with actual receiving logic
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                onNewData(response, finalI);

            }, 100); // 100ms interval for each string
        }
    }

    public void onNewData(byte[] data, int i) {
        SpannableStringBuilder spn = new SpannableStringBuilder();
        spn.append(TextUtil.toHexString(data)).append('\n');

        String hexValue = String.valueOf(spn).substring(12, 14);  // Characters at index 4 and 5 form the 4th byte in hex

        // Convert the hex value to decimal
        int decimalValue = Integer.parseInt(hexValue, 16);
        npk[i] = decimalValue;
        if(i == 2) updateTextView(npk);
    }


    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    private void write_data(byte[] data) throws IOException {
        usbSerialPort.write(data, 100);
    }



    private void updateTextView(int[] npk) {
        // Assuming you have TextViews defined in your layout
        TextView nValueTextView = getActivity().findViewById(R.id.N_value);
        TextView pValueTextView = getActivity().findViewById(R.id.P_value);
        TextView kValueTextView = getActivity().findViewById(R.id.K_value);

        for(int i = 0; i < 3; i++) {
            Log.d("HexToDecimal", "Value of" + i + ": " + npk[i]);
        }

        // Now you can use this decimalValue
        // Update the appropriate TextView based on your logic
        // You may need to determine which value to update based on your logic
        nValueTextView.setText(String.valueOf(npk[0]));
        pValueTextView.setText(String.valueOf(npk[1]));
        kValueTextView.setText(String.valueOf(npk[2]));
        }
    }







