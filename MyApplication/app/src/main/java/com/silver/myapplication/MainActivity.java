package com.silver.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Enumeration;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.WebSocket;
import okio.ByteString;


/// App Model
// This is the main activity of the app. It is responsible for creating the WebSocket connection and sending commands to the server.
// The app must maintain two websockets
// 1. One to the Chaos Engine
// 2. One to the Main Server
// Communication is bidirectional in both cases.
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final String WS_URL = "ws://10.0.2.2:6789";
    private static final int CLOSE_CODE = 1000;
    TextView logstv;
    OkHttpClient client;
    OkHttpClient client_main;
    WebSocket ws;
    WebSocket ws_main;
    boolean disconnected = false;

    public void sendBinaryMessage(WebSocket webSocket, ByteString bytes) {
        if (webSocket != null) {
            webSocket.send(bytes);
        }
    }

    public void sendCommand(WebSocket webSocket, String command, int params, byte[] data) {
        logAppend("Sending command: " + command + " with params: " + params);
        if (webSocket != null) {

            String encoded = java.util.Base64.getEncoder().encodeToString(data);
            String commandMessage = String.format("{\"type\": \"%s\", \"round\": \"%d\", \"data\": \"%s\"}", command, params, encoded);
            webSocket.send(commandMessage);
            logAppend("Command sent: " + commandMessage);
        }
        else {
            logAppend("WebSocket is null!");
        }
    }

    public void connectWs() {
        logAppend("Starting the WebSocket connection!");
        client = new OkHttpClient.Builder()
                .protocols(Arrays.asList(Protocol.HTTP_1_1))
                .build();
        Request request = new Request.Builder().url("ws://10.0.2.2:6789").build();
        NodeWSListener listener = new NodeWSListener();
        ws = client.newWebSocket(request, listener);

        sendCommand(ws, "FR", 1, new byte[]{0x01, 0x02, 0x03});
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                        return String.format("IP: %s",inetAddress.getHostAddress().toString());
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, "Exception in Get IP Address: " + ex.toString());
        }
        return null;
    }

    // I know this is not the best way to do this, but it's just a PoC
    // Easier to check relevant logs on display when running standalone mode.
    private void logAppend(String message) {
        runOnUiThread(() -> {
            logstv.append(message + "\n");
            Log.d(TAG, message);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logstv = findViewById(R.id.logscrollview);

        //Make logs tv non editable
        logstv.setFocusable(false);
        logstv.setClickable(false);
        logstv.setFocusableInTouchMode(false);
        logstv.setLongClickable(false);


        // Set the local IP address
        TextView ipField = findViewById(R.id.ipField);
        ipField.setText(getLocalIpAddress());

        // Initialize Buttons
        Button connectBtn = findViewById(R.id.connectBtn);
        connectBtn.setOnClickListener(v -> {
            connectWs();
            disconnected = false;
        });

        Button disconnectBtn = findViewById(R.id.disconnectBtn);
        disconnectBtn.setOnClickListener(v -> {
            if(disconnected == true) {
                logAppend("Worker already disconnected!\n");
                return;
            }
            logAppend("Disconnecting the WebSocket connection!");
            // Trigger shutdown of the dispatcher's executor so the app can exit.
            boolean closed = ws.close(CLOSE_CODE, "Normal Exit");
            if (closed) {
                logAppend("Worker disconnected!\n");
            }
            else {
                logAppend("Worker failed to disconnect!\n");
            }
            client.dispatcher().executorService().shutdown();
            disconnected = true;
        });
    }
}