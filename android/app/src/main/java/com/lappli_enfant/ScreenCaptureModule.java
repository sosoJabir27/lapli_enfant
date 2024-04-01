package com.lappli_enfant;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.os.Bundle;
import android.widget.Toast;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ScreenCaptureModule extends ReactContextBaseJavaModule implements Application.ActivityLifecycleCallbacks {

    private static final int REQUEST_SCREEN_CAPTURE_PERMISSION = 1;
    private static final int REQUEST_MEDIA_PROJECTION = 2;
    private static final String TAG = "ScreenCaptureModule";

    private Promise promise;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private boolean mediaProjectionInitialized = false;

    public ScreenCaptureModule(ReactApplicationContext reactContext) {
        super(reactContext);
        mediaProjectionManager = (MediaProjectionManager) reactContext
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Application application = (Application) reactContext.getApplicationContext();
        application.registerActivityLifecycleCallbacks(this); // Enregistrement des rappels de cycle de vie de
                                                              // l'activité
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        // Logique à exécuter lorsque une activité est créée
    }

    @Override
    public void onActivityStarted(Activity activity) {
        // Logique à exécuter lorsque une activité démarre
    }

    @Override
    public void onActivityResumed(Activity activity) {
        // Logique à exécuter lorsque une activité est reprise
    }

    @Override
    public void onActivityPaused(Activity activity) {
        // Logique à exécuter lorsque une activité est mise en pause
    }

    @Override
    public void onActivityStopped(Activity activity) {
        // Logique à exécuter lorsque une activité est arrêtée
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        // Logique à exécuter lorsque l'état de l'activité doit être sauvegardé
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        // Logique à exécuter lorsque une activité est détruite
    }

    // private void registerActivityLifecycleCallbacks() {
    // Application application = (Application)
    // getReactApplicationContext().getApplicationContext();
    // application.registerActivityLifecycleCallbacks(this);
    // }

    @Override
    public String getName() {
        return "ScreenCaptureModule";
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults,
            final Promise promise) {
        if (requestCode == REQUEST_SCREEN_CAPTURE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCapture(promise);
            } else {
                Toast.makeText(getCurrentActivity(), "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                promise.reject("PERMISSION_DENIED", "Permission denied for screen capture");
            }
        }
    }

    @ReactMethod
    public void startCapture(final Promise promise) {
        requestScreenCapturePermission(promise);
        requestMediaProjection(promise);

        if (checkWriteExternalStoragePermission() && checkReadExternalStoragePermission()) {
            requestMediaProjection(promise);
        } else {
            requestScreenCapturePermission(promise);

        }
    }

    private void requestScreenCapturePermission(final Promise promise) {
        ActivityCompat.requestPermissions(getCurrentActivity(),
                new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE },
                REQUEST_SCREEN_CAPTURE_PERMISSION);
        this.promise = promise;
    }

    private void requestMediaProjection(final Promise promise) {
        if (mediaProjectionManager != null) {

            Intent intent = mediaProjectionManager.createScreenCaptureIntent();

            promise.resolve("hhhhhhhhhhhhhhhh "  + intent);

            getCurrentActivity().startActivityForResult(intent, REQUEST_MEDIA_PROJECTION);
            onActivityResult(getCurrentActivity(), REQUEST_SCREEN_CAPTURE_PERMISSION, REQUEST_MEDIA_PROJECTION, intent);

            promise.resolve("startActivityForResult() called for obtaining MediaProjection   " + intent.getDataString()
                    + "  hhhhhhhhhh  " + intent.getAction());
        } else {
            promise.reject("MEDIA_PROJECTION_ERROR", "Failed to initialize MediaProjectionManager");
        }
    }

    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        promise.resolve("inside activity");
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
            if (mediaProjection != null) {
                mediaProjectionInitialized = true;
                startScreenCapture();
            } else {
                promise.reject("MEDIA_PROJECTION_ERROR", "Failed to get MediaProjection object");
            }
        } else {
            promise.reject("MEDIA_PROJECTION_ERROR", "MediaProjection initialization failed");
        }
    }

    @ReactMethod
    public void stopCapture(final Promise promise) {
        promise.resolve(mediaProjection);
        // Vérifier si MediaProjection est initialisé avant d'arrêter la capture d'écran
        if (mediaProjectionInitialized && mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
            promise.resolve("Screen capture stopped");
        } else {
            promise.reject("MEDIA_PROJECTION_ERROR", "MediaProjection is not initialized");
        }
    }

    private boolean checkWriteExternalStoragePermission() {
        return ContextCompat.checkSelfPermission(getReactApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkReadExternalStoragePermission() {
        return ContextCompat.checkSelfPermission(getReactApplicationContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void startScreenCapture() {
        // Get screen dimensions
        int screenWidth = getCurrentActivity().getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getCurrentActivity().getWindowManager().getDefaultDisplay().getHeight();
        int screenDensity = getCurrentActivity().getResources().getDisplayMetrics().densityDpi;

        // Create ImageReader
        imageReader = ImageReader.newInstance(screenWidth, screenHeight, 0x1, 2);

        // Create VirtualDisplay
        mediaProjection.createVirtualDisplay("ScreenCapture", screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, null);

        // Capture screen after a short delay to allow VirtualDisplay to be created
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss",
                            java.util.Locale.getDefault());
                    String date = dateFormat.format(new Date());
                    String fileName = "screenshot-" + date + ".png";

                    Bitmap bitmap = takeScreenshot();
                    saveBitmap(bitmap, fileName);

                } catch (Exception e) {
                    Log.e(TAG, "Error capturing screen: " + e.getMessage());
                }
            }
        }, 1000);
    }

    private Bitmap takeScreenshot() {
        Bitmap screenshotBitmap = null;
        if (imageReader != null) {
            // Get the latest frame from ImageReader
            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                // Convert Image to Bitmap
                int width = image.getWidth();
                int height = image.getHeight();
                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * width;
                screenshotBitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height,
                        Bitmap.Config.ARGB_8888);
                screenshotBitmap.copyPixelsFromBuffer(buffer);
                image.close();
            }
        }
        return screenshotBitmap;
    }

    private void saveBitmap(Bitmap bitmap, String fileName) {
        FileOutputStream fos = null;

        try {
            File directory = new File(Environment.getExternalStorageDirectory() + File.separator + "Screenshots");

            if (!directory.exists()) {
                directory.mkdirs();
            }

            File file = new File(directory, fileName);
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            promise.resolve("Screenshot saved: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error saving bitmap: " + e.getMessage());
            promise.reject("SAVE_ERROR", "Error saving bitmap");
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing FileOutputStream: " + e.getMessage());
                }
            }
        }
    }
}
