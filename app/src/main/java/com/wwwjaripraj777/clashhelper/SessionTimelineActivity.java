package com.wwwjaripraj777.clashhelper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SessionTimelineActivity extends AppCompatActivity {

    private ListView lv;
    private Button btnExport;
    private Button btnAnalyze;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        lv = findViewById(R.id.lv_sessions);
        btnExport = findViewById(R.id.btn_export_csv);
        btnAnalyze = findViewById(R.id.btn_analyze);

        loadFiles();

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File csv = ExportUtil.exportMediaMetadataToCsv(SessionTimelineActivity.this);
                if (csv != null) {
                    Toast.makeText(SessionTimelineActivity.this, "Exported: " + csv.getAbsolutePath(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SessionTimelineActivity.this, "Export failed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAnalyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SuspiciousAnalyzer.Report r = SuspiciousAnalyzer.analyzeScreenshots(SessionTimelineActivity.this);
                Toast.makeText(SessionTimelineActivity.this, r.details, Toast.LENGTH_LONG).show();
            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String path = (String) parent.getItemAtPosition(position);
                File f = new File(path);
                if (!f.exists()) {
                    Toast.makeText(SessionTimelineActivity.this, "File not found", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent it = new Intent(Intent.ACTION_VIEW);
                it.setDataAndType(Uri.fromFile(f), f.getName().toLowerCase().endsWith(".mp4") ? "video/*" : "image/*");
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(it);
                } catch (Exception e) {
                    Toast.makeText(SessionTimelineActivity.this, "No app to open this file", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadFiles() {
        List<String> items = new ArrayList<>();
        File movies = getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES);
        File pics = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        if (movies != null && movies.exists()) {
            File[] m = movies.listFiles();
            if (m != null) for (File f : m) items.add(f.getAbsolutePath());
        }
        if (pics != null && pics.exists()) {
            File[] p = pics.listFiles();
            if (p != null) for (File f : p) items.add(f.getAbsolutePath());
        }
        ArrayAdapter<String> adp = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        lv.setAdapter(adp);
    }
}
