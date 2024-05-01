package com.google.mlkit.vision.demo.java;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class NodeWSListener extends WebSocketListener {
    private static final int NORMAL_CLOSURE_STATUS = 1000;
    private static final String TAG = "NodeWSListener";
    private final BitmapCallback callback;

    public NodeWSListener(BitmapCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Log.v("Worker", "Connected to server!");
    }

    // Push all messages to an ChaosEngineHandler Handler abstraction one for chaosEngine and
    // the other for the main server
    @Override

    public void onMessage(WebSocket webSocket, String text) {
        Log.v("Worker", "Receiving : " + text);

        try {
            // Decode text as JSON
            JSONObject task = new JSONObject(text);

            if ("error".equals(task.getString("type"))) {
                Log.e("Worker", "Error received from server: " + task.getString("message"));
                return;
            }

            // Check if the message type is 'image'
            if ("image".equals(task.getString("type"))) {
                String base64Image = task.getString("blob");
                // Decode Base64 to get image byte array
                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                // Convert byte array to Bitmap
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                Log.v("Worker", "Image decoded successfully");

                callback.onBitmapReceived(bitmap);

            } else {
                Log.v("Worker", "Unsupported type or operation");
            }
        } catch (JSONException e) {
            Log.e("Worker", "Failed to decode received data", e);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        Log.v("Worker", "Receiving bytes : " + bytes.hex());
        // TODO: Implement the logic to handle the messages
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(NORMAL_CLOSURE_STATUS, null);
        Log.v("Worker", "Closing : " + code + " / " + reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        t.printStackTrace();
    }

}