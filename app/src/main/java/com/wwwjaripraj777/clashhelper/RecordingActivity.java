package com.wwwjaripraj777.clashhelper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class RecordingActivity extends AppCompatActivity {

    private static final int REQUEST_SCREEN_CAPTURE = 3001;
    private Button btnStart;
    private Button btnStop;
    private Button btnScreenshot;

    private int resultCode = 0;
    private Intent resultData = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);

        btnStart = findViewById(R.id.btn_start_record);
        btnStop = findViewById(R.id.btn_stop_record);
        btnScreenshot = findViewById(R.id.btn_screenshot);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScreenCaptureIntent();
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(RecordingActivity.this, RecordingService.class);
                i.setAction(RecordingService.ACTION_STOP);
                startService(i);
            }
        });

        btnScreenshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (resultData == null) {
                    Toast.makeText(RecordingActivity.this, "Start a screen capture session first", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent i = new Intent(RecordingActivity.this, RecordingService.class);
                i.setAction(RecordingService.ACTION_SCREENSHOT);
                i.putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode);
                i.putExtra(RecordingService.EXTRA_RESULT_INTENT, resultData);
                startService(i);
            }
        });
    }

    private void startScreenCaptureIntent() {
        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = mpm.createScreenCaptureIntent();
        startActivityForResult(captureIntent, REQUEST_SCREEN_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCREEN_CAPTURE) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
                return;
            }
            this.resultCode = resultCode;
            this.resultData = data;

            // Start recording service with data
            Intent i = new Intent(this, RecordingService.class);
            i.setAction(RecordingService.ACTION_START);
            i.putExtra(RecordingService.EXTRA_RESULT_CODE, resultCode);
            i.putExtra(RecordingService.EXTRA_RESULT_INTENT, data);
            startService(i);
        }
    }
}
