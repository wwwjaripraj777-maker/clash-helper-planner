package com.wwwjaripraj777.clashhelper;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SuspiciousAnalyzer {
    private static final String TAG = "SuspiciousAnalyzer";

    public static class Report {
        public boolean suspicious;
        public String details;
    }

    public static Report analyzeScreenshots(Context ctx) {
        Report r = new Report();
        try {
            File pics = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
            if (pics == null || !pics.exists()) {
                r.suspicious = false;
                r.details = "No screenshots found.";
                return r;
            }
            File[] files = pics.listFiles();
            if (files == null || files.length < 6) {
                r.suspicious = false;
                r.details = "Not enough screenshots for analysis (need >=6).";
                return r;
            }
            List<File> list = new ArrayList<>(Arrays.asList(files));
            Collections.sort(list, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return Long.compare(o1.lastModified(), o2.lastModified());
                }
            });
            List<Long> diffs = new ArrayList<>();
            for (int i = 1; i < list.size(); i++) {
                diffs.add(list.get(i).lastModified() - list.get(i - 1).lastModified());
            }
            double mean = 0;
            for (Long d : diffs) mean += d;
            mean = mean / diffs.size();
            double var = 0;
            for (Long d : diffs) var += (d - mean) * (d - mean);
            var = var / diffs.size();
            double std = Math.sqrt(var);

            // If standard deviation is very low compared to mean, flag suspicious (very regular screenshots)
            boolean suspicious = std < mean * 0.05; // arbitrary threshold: std < 5% of mean
            r.suspicious = suspicious;
            StringBuilder sb = new StringBuilder();
            sb.append("Screenshots analyzed: ").append(list.size()).append("\n");
            sb.append(String.format("Mean interval (ms): %.1f\n", mean));
            sb.append(String.format("Std dev (ms): %.1f\n", std));
            if (suspicious) sb.append("Pattern detected: intervals are unusually regular (possible automation).\n");
            else sb.append("No strong regularity detected.\n");
            r.details = sb.toString();
            return r;
        } catch (Exception e) {
            Log.e(TAG, "Analysis error", e);
            r.suspicious = false;
            r.details = "Analysis failed: " + e.getMessage();
            return r;
        }
    }
}
