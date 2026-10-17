package com.wwwjaripraj777.clashhelper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingService extends Service {
    public static final String TAG = "RecordingService";

    public static final String ACTION_START = "com.wwwjaripraj777.clashhelper.action.START";
    public static final String ACTION_STOP = "com.wwwjaripraj777.clashhelper.action.STOP";
    public static final String ACTION_SCREENSHOT = "com.wwwjaripraj777.clashhelper.action.SCREENSHOT";

    public static final String EXTRA_RESULT_CODE = "extra_result_code";
    public static final String EXTRA_RESULT_INTENT = "extra_result_intent";

    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private android.hardware.display.VirtualDisplay virtualDisplay;

    private int width;
    private int height;
    private int densityDpi;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
            Intent data = intent.getParcelableExtra(EXTRA_RESULT_INTENT);
            startRecording(resultCode, data);
        } else if (ACTION_STOP.equals(action)) {
            stopRecording();
        } else if (ACTION_SCREENSHOT.equals(action)) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1);
            Intent data = intent.getParcelableExtra(EXTRA_RESULT_INTENT);
            takeScreenshot(resultCode, data);
        }
        return START_STICKY;
    }

    private void startRecording(int resultCode, Intent data) {
        if (mediaProjection != null) {
            Log.i(TAG, "Already recording");
            return;
        }

        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpm.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection is null");
            return;
        }

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        densityDpi = metrics.densityDpi;
        width = metrics.widthPixels;
        height = metrics.heightPixels;

        try {
            setUpMediaRecorder();
            virtualDisplay = mediaProjection.createVirtualDisplay("RecordingDisplay",
                    width, height, densityDpi,
                    android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mediaRecorder.getSurface(), null, null);
            mediaRecorder.start();

            startForeground(101, buildNotification("Recording..."));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setUpMediaRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        File movies = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (movies == null) movies = getFilesDir();
        String fileName = "clash_record_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp4";
        File out = new File(movies, fileName);
        mediaRecorder.setOutputFile(out.getAbsolutePath());

        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.prepare();
    }

    private void stopRecording() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }

        stopForeground(true);
        stopSelf();
    }

    private void takeScreenshot(int resultCode, Intent data) {
        // For screenshot, if mediaProjection already available, reuse it; otherwise create a temporary one
        final MediaProjection tempProjection;
        boolean temporary = false;
        if (mediaProjection != null) {
            tempProjection = mediaProjection;
        } else {
            MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            tempProjection = mpm.getMediaProjection(resultCode, data);
            temporary = true;
            if (tempProjection == null) {
                Log.e(TAG, "Unable to obtain MediaProjection for screenshot");
                return;
            }
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(metrics);
            densityDpi = metrics.densityDpi;
            width = metrics.widthPixels;
            height = metrics.heightPixels;
        }

        ImageReader imageReader = ImageReader.newInstance(width, height, ImageFormat.RGB_565, 2);
        android.hardware.display.VirtualDisplay v = tempProjection.createVirtualDisplay("screencap",
                width, height, densityDpi,
                android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        Image image = null;
        try {
            // Wait briefly for image to be available
            long start = System.currentTimeMillis();
            while ((image = imageReader.acquireLatestImage()) == null && System.currentTimeMillis() - start < 2000) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (image == null) {
                Log.e(TAG, "No image captured");
                return;
            }

            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.RGB_565);
            bitmap.copyPixelsFromBuffer(buffer);

            // Crop to the actual width
            Bitmap cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height);

            File pics = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (pics == null) pics = getFilesDir();
            String fileName = "clash_ss_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".png";
            File out = new File(pics, fileName);
            FileOutputStream fos = new FileOutputStream(out);
            cropped.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            Log.i(TAG, "Screenshot saved: " + out.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (image != null) image.close();
            imageReader.close();
            v.release();
            if (temporary && tempProjection != null) tempProjection.stop();
        }
    }

    private Notification buildNotification(String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "rec_chan")
                .setContentTitle("Clash Helper Recorder")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel("rec_chan", "Recorder", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
    }
}
