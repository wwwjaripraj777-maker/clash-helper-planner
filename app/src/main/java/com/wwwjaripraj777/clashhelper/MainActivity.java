package com.wwwjaripraj777.clashhelper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY = 1001;
    private Button btnStartFloating;
    private Button btnRecording;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartFloating = findViewById(R.id.btn_start_floating);
        btnStartFloating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestOverlayPermissionIfNeeded();
            }
        });

        btnRecording = findViewById(R.id.btn_recording);
        btnRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, RecordingActivity.class);
                startActivity(i);
            }
        });

        // Initialize ViewModel or DB here if needed
        UpgradeViewModel vm = new ViewModelProvider(this).get(UpgradeViewModel.class);
    }

    private void requestOverlayPermissionIfNeeded() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY);
        } else {
            startFloatingService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                startFloatingService();
            }
        }
    }

    private void startFloatingService() {
        Intent svc = new Intent(this, FloatingButtonService.class);
        startService(svc);
    }
}
