package com.google.mlkit.vision.demo.java;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Base64;

public class ImageDecoder {

    public static void decodeBase64ToImage(String base64ImageString, String outputPath) throws IOException {
        // Decode Base64 to byte array
        byte[] imageBytes = Base64.getDecoder().decode(base64ImageString);

        ByteBuffer byteBuffer = ByteBuffer.wrap(imageBytes);
        // Convert byte array to BufferedImage
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes)) {

        } catch (IOException e) {
            System.out.println("Error decoding or saving the image: " + e.getMessage());
            throw e; // Re-throwing to allow caller to handle
        }
    }


    public static void main(String[] args) {
        try {
            String base64ImageString = "your_base64_encoded_string_here";
            decodeBase64ToImage(base64ImageString, "output_image.png");
        } catch (IOException e) {
            System.out.println("Error processing the image: " + e.getMessage());
        }
    }
}
