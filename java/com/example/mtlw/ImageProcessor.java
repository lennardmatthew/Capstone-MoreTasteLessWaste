package com.example.mtlw;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;

public class ImageProcessor {
    private static final String TAG = "ImageProcessor";

    public static Bitmap processImage(InputStream inputStream) {
        try {
            // Read the image from input stream
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode image from input stream");
                return null;
            }

            // Get image orientation
            int orientation = getImageOrientation(inputStream);
            
            // Rotate image if needed
            if (orientation != 0) {
                originalBitmap = rotateImage(originalBitmap, orientation);
            }

            // Resize image to a reasonable size (e.g., 1024x1024 max)
            return resizeImage(originalBitmap, 1024, 1024);
        } catch (Exception e) {
            Log.e(TAG, "Error processing image: " + e.getMessage());
            return null;
        }
    }

    private static int getImageOrientation(InputStream inputStream) {
        try {
            ExifInterface exif = new ExifInterface(inputStream);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading image orientation: " + e.getMessage());
            return 0;
        }
    }

    private static Bitmap rotateImage(Bitmap bitmap, int degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static Bitmap resizeImage(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        float ratio = Math.min(
            (float) maxWidth / width,
            (float) maxHeight / height
        );
        
        if (ratio >= 1) {
            return bitmap;
        }
        
        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
} 