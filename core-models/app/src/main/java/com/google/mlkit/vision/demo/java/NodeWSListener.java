package com.google.mlkit.vision.demo.java;

import android.util.Log;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class NodeWSListener extends WebSocketListener {
    private static final int NORMAL_CLOSURE_STATUS = 1000;
    private static final String TAG = "NodeWSListener";

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Log.v("Worker", "Connected to the server!");
    }

    // Push all messages to an ChaosEngineHandler Handler abstraction one for chaosEngine and the other for the main server
    @Override
    public void onMessage(WebSocket webSocket, String text) {
        Log.v("Worker", "Receiving : " + text);
        // TODO: Implement the logic to handle the messages
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