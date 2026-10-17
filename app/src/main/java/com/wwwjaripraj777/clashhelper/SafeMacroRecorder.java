package com.wwwjaripraj777.clashhelper;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class SafeMacroRecorder {
    private static final String TAG = "SafeMacroRecorder";
    private final View rootView;
    private final List<SafeMacroDefinition> events = new ArrayList<>();
    private boolean isRecording;

    public SafeMacroRecorder(View rootView) {
        this.rootView = rootView;
    }

    public void startRecording() {
        events.clear();
        isRecording = true;
        rootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isRecording) return false;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    events.add(new SafeMacroDefinition(SafeMacroDefinition.ACTION_TAP,
                            (int) event.getRawX(),
                            (int) event.getRawY(),
                            0L));
                    Log.i(TAG, "Recorded tap at x=" + event.getRawX() + ", y=" + event.getRawY());
                }
                return false;
            }
        });
    }

    public void stopRecording() {
        isRecording = false;
    }

    public List<SafeMacroDefinition> getEvents() {
        return new ArrayList<>(events);
    }

    public void replayOnRoot(final List<SafeMacroDefinition> macro, final Runnable onComplete) {
        final Handler h = new Handler(Looper.getMainLooper());
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (SafeMacroDefinition ev : macro) {
                    if (ev.actionType.equals(SafeMacroDefinition.ACTION_TAP)) {
                        h.post(new Runnable() {
                            @Override
                            public void run() {
                                dispatchTap((int) ev.x, (int) ev.y);
                            }
                        });
                    }
                    if (ev.delayMs > 0) {
                        try {
                            Thread.sleep(ev.delayMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                h.post(new Runnable() {
                    @Override
                    public void run() {
                        if (onComplete != null) onComplete.run();
                    }
                });
            }
        }).start();
    }

    private void dispatchTap(int x, int y) {
        long downTime = System.currentTimeMillis();
        long eventTime = System.currentTimeMillis();
        MotionEvent down = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
        rootView.dispatchTouchEvent(down);
        rootView.dispatchTouchEvent(up);
        down.recycle();
        up.recycle();
    }
}
