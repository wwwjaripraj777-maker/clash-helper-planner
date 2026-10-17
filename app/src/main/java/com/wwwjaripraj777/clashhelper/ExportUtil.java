package com.wwwjaripraj777.clashhelper;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportUtil {
    private static final String TAG = "ExportUtil";

    public static File exportMediaMetadataToCsv(Context ctx) {
        File docs = ctx.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (docs == null) docs = ctx.getFilesDir();
        File out = new File(docs, "clash_helper_media_metadata_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv");

        List<File> mediaFiles = new ArrayList<>();
        File movies = ctx.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        File pics = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (movies != null && movies.exists()) mediaFiles.addAll(Arrays.asList(movies.listFiles()));
        if (pics != null && pics.exists()) mediaFiles.addAll(Arrays.asList(pics.listFiles()));

        try {
            FileOutputStream fos = new FileOutputStream(out);
            OutputStreamWriter w = new OutputStreamWriter(fos);
            w.write("filename,type,timestamp,filesize\n");
            for (File f : mediaFiles) {
                if (f == null) continue;
                String type = f.getName().toLowerCase().endsWith(".mp4") ? "video" : "image";
                long ts = f.lastModified();
                long size = f.length();
                w.write(String.format(Locale.US, "%s,%s,%d,%d\n", f.getName(), type, ts, size));
            }
            w.flush();
            w.close();
            fos.close();
            Log.i(TAG, "Exported CSV to: " + out.getAbsolutePath());
            return out;
        } catch (Exception e) {
            Log.e(TAG, "Export failed", e);
            return null;
        }
    }
}
