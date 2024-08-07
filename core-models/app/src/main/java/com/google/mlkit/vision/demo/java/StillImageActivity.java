/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.java;

import static java.lang.Math.max;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.annotation.KeepName;
import com.google.mlkit.vision.demo.BitmapUtils;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.R;
import com.google.mlkit.vision.demo.VisionImageProcessor;
import com.google.mlkit.vision.demo.java.labeldetector.LabelDetectorProcessor;
import com.google.mlkit.vision.demo.preference.SettingsActivity;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.WebSocket;
import okio.ByteString;

/** Activity demonstrating different image detector features with a still image from camera. */
@KeepName
public final class StillImageActivity extends AppCompatActivity implements BitmapCallback, LabelCallback {

  private static final String TAG = "ImageRecognition";

  private static final String OBJECT_DETECTION = "Object Detection";
  private static final String IMAGE_LABELING = "Image Labeling";

  private static final String SIZE_SCREEN = "w:screen"; // Match screen width
  private static final String SIZE_1024_768 = "w:1024"; // ~1024*768 in a normal ratio
  private static final String SIZE_640_480 = "w:640"; // ~640*480 in a normal ratio
  private static final String SIZE_ORIGINAL = "w:original"; // Original image size

  private static final String KEY_IMAGE_URI = "com.google.mlkit.vision.demo.KEY_IMAGE_URI";
  private static final String KEY_SELECTED_SIZE = "com.google.mlkit.vision.demo.KEY_SELECTED_SIZE";

  private static final int REQUEST_IMAGE_CAPTURE = 1001;
  private static final int REQUEST_CHOOSE_IMAGE = 1002;

  private ImageView preview;
  private GraphicOverlay graphicOverlay;
  private String selectedMode = OBJECT_DETECTION;
  private String selectedSize = SIZE_SCREEN;

  boolean isLandScape;

  private Uri imageUri;
  private int imageMaxWidth;
  private int imageMaxHeight;
  private VisionImageProcessor imageProcessor;

  private int round = 0;
  // Custom
  private static final String WS_URL = "ws://10.0.2.2:6789";
  private static final int CLOSE_CODE = 1000;
//  TextView logstv;
  OkHttpClient client;
  OkHttpClient client_main;
  WebSocket ws;
  WebSocket ws_main;
  boolean disconnected = false;

  boolean ChaosMode = true;
  public void sendBinaryMessage(WebSocket webSocket, ByteString bytes) {
    if (webSocket != null) {
      webSocket.send(bytes);
    }
  }

  // I know this is not the best way to do this, but it's just a PoC
  // Easier to check relevant logs on display when running standalone mode.
  private void logAppend(String message) {
    runOnUiThread(() -> {
//      logstv.append(message + "\n");
      Log.d(TAG, message);
    });
  }

  public void sendCommand(WebSocket webSocket, String command, int round, byte[] data) {
    logAppend("Sending command: " + command + " with round: " + round);
    if (webSocket != null) {

      // Encode data to base64
      String encoded = java.util.Base64.getEncoder().encodeToString(data);
      String commandMessage = String.format("{\"type\": \"%s\", \"round\": %d, \"data\": \"%s\"}", command, round, encoded);
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
            .connectTimeout(10, TimeUnit.SECONDS)
            .protocols(Arrays.asList(Protocol.HTTP_1_1))
            .build();
    Request request = new Request.Builder().url("ws://10.0.2.2:6789").build();
//    Request request = new Request.Builder().url("ws://localhost:6789").build();
    NodeWSListener listener = new NodeWSListener(this);
    ws = client.newWebSocket(request, listener);

//    sendCommand(ws, "REG", 0, "dev_name".getBytes());
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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_still_image);
    StrictMode.allowThreadDiskReads();
    findViewById(R.id.select_image_button)
        .setOnClickListener(
            view -> {
              // Menu for selecting either: a) take new photo b) select from existing
              PopupMenu popup = new PopupMenu(StillImageActivity.this, view);
              popup.setOnMenuItemClickListener(
                  menuItem -> {
                    int itemId = menuItem.getItemId();
                    if (itemId == R.id.select_images_from_local) {
                      startChooseImageIntentForResult();
                      return true;
                    } else if (itemId == R.id.take_photo_using_camera) {
                      startCameraIntentForResult();
                      return true;
                    }
                    return false;
                  });
              MenuInflater inflater = popup.getMenuInflater();
              inflater.inflate(R.menu.camera_button_menu, popup.getMenu());
              popup.show();
            });
    preview = findViewById(R.id.preview);
    graphicOverlay = findViewById(R.id.graphic_overlay);

    populateFeatureSelector();
    populateSizeSelector();

    isLandScape =
        (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

    if (savedInstanceState != null) {
      imageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI);
      selectedSize = savedInstanceState.getString(KEY_SELECTED_SIZE);
    }

    View rootView = findViewById(R.id.root);
    rootView
        .getViewTreeObserver()
        .addOnGlobalLayoutListener(
            new OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                imageMaxWidth = rootView.getWidth();
                imageMaxHeight = rootView.getHeight() - findViewById(R.id.control).getHeight();
                if (SIZE_SCREEN.equals(selectedSize)) {
                  tryReloadAndDetectInImage();
                }
              }
            });

    Switch connectsw = findViewById(R.id.connect_switch);
    connectsw.setOnCheckedChangeListener((buttonView, isChecked) -> {
      if (isChecked) {
        connectWs();
        disconnected = false;
      } else {
        if(disconnected == true) {
          logAppend("Worker already disconnected!\n");
          return;
        }
        boolean closed = ws.close(CLOSE_CODE, "User requested disconnect");
        if (closed) {
          logAppend("Worker disconnected!\n");
        }
        else {
          logAppend("Worker failed to disconnect!\n");
        }
        client.dispatcher().executorService().shutdown();
        disconnected = true;
        this.round = 0;
      }
    });

    ImageView settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(
        v -> {
          Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
          intent.putExtra(
              SettingsActivity.EXTRA_LAUNCH_SOURCE, SettingsActivity.LaunchSource.STILL_IMAGE);
          startActivity(intent);
        });
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    createImageProcessor();
    tryReloadAndDetectInImage();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
  }

  private void populateFeatureSelector() {
    Spinner featureSpinner = findViewById(R.id.feature_selector);
    List<String> options = new ArrayList<>();
    options.add(IMAGE_LABELING);

    // Creating adapter for featureSpinner
    ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
    // Drop down layout style - list view with radio button
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // attaching data adapter to spinner
    featureSpinner.setAdapter(dataAdapter);
    featureSpinner.setOnItemSelectedListener(
        new OnItemSelectedListener() {

          @Override
          public void onItemSelected(
              AdapterView<?> parentView, View selectedItemView, int pos, long id) {
            selectedMode = parentView.getItemAtPosition(pos).toString();
            createImageProcessor();
            tryReloadAndDetectInImage();
          }

          @Override
          public void onNothingSelected(AdapterView<?> arg0) {}
        });
  }

  private void populateSizeSelector() {
    Spinner sizeSpinner = findViewById(R.id.size_selector);
    List<String> options = new ArrayList<>();
    options.add(SIZE_SCREEN);
    options.add(SIZE_1024_768);
    options.add(SIZE_640_480);
    options.add(SIZE_ORIGINAL);

    // Creating adapter for featureSpinner
    ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
    // Drop down layout style - list view with radio button
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // attaching data adapter to spinner
    sizeSpinner.setAdapter(dataAdapter);
    sizeSpinner.setOnItemSelectedListener(
        new OnItemSelectedListener() {

          @Override
          public void onItemSelected(
              AdapterView<?> parentView, View selectedItemView, int pos, long id) {
            selectedSize = parentView.getItemAtPosition(pos).toString();
            tryReloadAndDetectInImage();
          }

          @Override
          public void onNothingSelected(AdapterView<?> arg0) {}
        });
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(KEY_IMAGE_URI, imageUri);
    outState.putString(KEY_SELECTED_SIZE, selectedSize);
  }

  private void startCameraIntentForResult() {
    // Clean up last time's image
    imageUri = null;
    preview.setImageBitmap(null);

    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      ContentValues values = new ContentValues();
      values.put(MediaStore.Images.Media.TITLE, "New Picture");
      values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
      imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
      takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }
  }

  private void startChooseImageIntentForResult() {
    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      tryReloadAndDetectInImage();
    } else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == RESULT_OK) {
      // In this case, imageUri is returned by the chooser, save it.
      imageUri = data.getData();
      tryReloadAndDetectInImage();
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void tryReloadAndDetectInImage() {
    Log.d(TAG, "Try reload and detect image");
    try {
      if (imageUri == null) {
        return;
      }

      if (SIZE_SCREEN.equals(selectedSize) && imageMaxWidth == 0) {
        // UI layout has not finished yet, will reload once it's ready.
        return;
      }

      Bitmap imageBitmap = BitmapUtils.getBitmapFromContentUri(getContentResolver(), imageUri);
      if (imageBitmap == null) {
        return;
      }

      // Clear the overlay first
      graphicOverlay.clear();

      Bitmap resizedBitmap;
      if (selectedSize.equals(SIZE_ORIGINAL)) {
        resizedBitmap = imageBitmap;
      } else {
        // Get the dimensions of the image view
        Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

        // Determine how much to scale down the image
        float scaleFactor =
            max(
                (float) imageBitmap.getWidth() / (float) targetedSize.first,
                (float) imageBitmap.getHeight() / (float) targetedSize.second);

        resizedBitmap =
            Bitmap.createScaledBitmap(
                imageBitmap,
                (int) (imageBitmap.getWidth() / scaleFactor),
                (int) (imageBitmap.getHeight() / scaleFactor),
                true);
      }

      preview.setImageBitmap(resizedBitmap);

      if (imageProcessor != null) {
        graphicOverlay.setImageSourceInfo(
            resizedBitmap.getWidth(), resizedBitmap.getHeight(), /* isFlipped= */ false);
        imageProcessor.processBitmap(resizedBitmap, graphicOverlay);
      } else {
        Log.e(TAG, "Null imageProcessor, please check adb logs for imageProcessor creation error");
      }
    } catch (IOException e) {
      Log.e(TAG, "Error retrieving saved image");
      imageUri = null;
    }
  }

  private void tryReloadAndDetectInImage(Bitmap bitmap) {
    Log.d(TAG, "Labelling Image");

    if (SIZE_SCREEN.equals(selectedSize) && imageMaxWidth == 0) {
      // UI layout has not finished yet, will reload once it's ready.
      return;
    }

    Bitmap imageBitmap = bitmap;
    if (imageBitmap == null) {
      return;
    }

    // Clear the overlay first
    graphicOverlay.clear();

    Bitmap resizedBitmap;
    if (selectedSize.equals(SIZE_ORIGINAL)) {
      resizedBitmap = imageBitmap;
    } else {
      // Get the dimensions of the image view
      Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

      // Determine how much to scale down the image
      float scaleFactor =
              max(
                      (float) imageBitmap.getWidth() / (float) targetedSize.first,
                      (float) imageBitmap.getHeight() / (float) targetedSize.second);

      resizedBitmap =
              Bitmap.createScaledBitmap(
                      imageBitmap,
                      (int) (imageBitmap.getWidth() / scaleFactor),
                      (int) (imageBitmap.getHeight() / scaleFactor),
                      true);
    }

    preview.setImageBitmap(resizedBitmap);

    if (imageProcessor != null) {
      graphicOverlay.setImageSourceInfo(
              resizedBitmap.getWidth(), resizedBitmap.getHeight(), /* isFlipped= */ false);
      imageProcessor.processBitmap(resizedBitmap, graphicOverlay);
    } else {
      Log.e(TAG, "Null imageProcessor, please check adb logs for imageProcessor creation error");
    }
    Log.v(TAG, "Image labelled");
  }

  private Pair<Integer, Integer> getTargetedWidthHeight() {
    int targetWidth;
    int targetHeight;

    switch (selectedSize) {
      case SIZE_SCREEN:
        targetWidth = imageMaxWidth;
        targetHeight = imageMaxHeight;
        break;
      case SIZE_640_480:
        targetWidth = isLandScape ? 640 : 480;
        targetHeight = isLandScape ? 480 : 640;
        break;
      case SIZE_1024_768:
        targetWidth = isLandScape ? 1024 : 768;
        targetHeight = isLandScape ? 768 : 1024;
        break;
      default:
        throw new IllegalStateException("Unknown size");
    }

    return new Pair<>(targetWidth, targetHeight);
  }

  private void createImageProcessor() {
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
    try {
      switch (selectedMode) {
        case IMAGE_LABELING:
          imageProcessor = new LabelDetectorProcessor(this, ImageLabelerOptions.DEFAULT_OPTIONS, this);
          break;
        default:
          Log.e(TAG, "Unknown selectedMode: " + selectedMode);
      }
    } catch (Exception e) {
      Log.e(TAG, "Can not create image processor: " + selectedMode, e);
      Toast.makeText(
              getApplicationContext(),
              "Can not create image processor: " + e.getMessage(),
              Toast.LENGTH_LONG)
          .show();
    }
  }

  @Override
  public void onBitmapReceived(Bitmap bitmap) {
    Log.v(TAG, "Bitmap received");
    runOnUiThread(() -> {
      tryReloadAndDetectInImage(bitmap);
    });
  }


  // Chaos here.
  @Override
  public void onLabelCallback(List<ImageLabel> labels) {
    Log.v(TAG, "Labels received");
    // Convert the labels to a Json string
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    for (ImageLabel label : labels) {
      sb.append("{\"label\": \"");
      sb.append(label.getText());
      sb.append("\", \"confidence\": ");
      sb.append(label.getConfidence());
      sb.append("},");
    }

    // Remove the last comma
    if (labels.size() > 0) {
      sb.deleteCharAt(sb.length() - 1);
    }
    sb.append("]");
    String json = sb.toString();
    Log.v(TAG, json);

    if (this.ChaosMode) {
      Random rand = new Random();
      // Generate a random sleep duration under 100ms.
      long sleepdur = rand.nextInt(100);
      try {
        if (rand.nextInt(10) < 8) {
          Thread.sleep(sleepdur);
        }
      } catch (InterruptedException e) {
        Log.d(TAG, "onLabelCallback: Unable to sleep lmao");
        throw new RuntimeException(e);
      }
    }

    // Send the labels to the worker
    sendCommand(ws, "LB", this.round, json.getBytes());
    this.round++;
  }
}
