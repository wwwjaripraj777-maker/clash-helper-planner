package com.wwwjaripraj777.clashhelper;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class SafeMacroActivity extends AppCompatActivity {
    private SafeMacroRecorder recorder;
    private TextView textView;
    private Button btnStart;
    private Button btnStop;
    private Button btnReplay;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_macro);

        View root = findViewById(android.R.id.content);
        recorder = new SafeMacroRecorder(root);

        textView = findViewById(R.id.tv_macro_status);
        btnStart = findViewById(R.id.btn_start_macro);
        btnStop = findViewById(R.id.btn_stop_macro);
        btnReplay = findViewById(R.id.btn_replay_macro);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.startRecording();
                textView.setText("Recording taps inside this app only...");
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recorder.stopRecording();
                List<SafeMacroDefinition> events = recorder.getEvents();
                textView.setText("Recorded " + events.size() + " tap(s). This is limited to the current app only.");
            }
        });

        btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<SafeMacroDefinition> events = recorder.getEvents();
                if (events.isEmpty()) {
                    textView.setText("No macro recorded.");
                    return;
                }
                textView.setText("Replaying recorded taps in this app...");
                recorder.replayOnRoot(events, new Runnable() {
                    @Override
                    public void run() {
                        textView.setText("Replay complete.");
                    }
                });
            }
        });
    }
}
