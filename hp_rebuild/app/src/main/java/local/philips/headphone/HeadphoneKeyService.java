/*
 * Copyright 2026 Michał Dulko
 * SPDX-License-Identifier: Apache-2.0
 */
package local.philips.headphone;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** Mirrors the TV's MUSIC volume to the Philips headphone property. Never changes MUSIC. */
public final class HeadphoneKeyService extends AccessibilityService {
    private static final String TAG = "HeadphoneKeyService";
    private static final String VOLUME_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private static final String MUTE_ACTION = "android.media.STREAM_MUTE_CHANGED_ACTION";
    private static final String STREAM_EXTRA = "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private static final long POLL_MS = 1000;
    private static final long API_RETRY_MS = 5000;
    private static final long STATUS_LOG_MS = 10000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private AudioManager audio;
    private PhilipsAudioController controller;
    private boolean running;
    private boolean receiverRegistered;
    private long retryAfter;
    private long nextStatusLog;
    private int attempts;
    private int previousMain = Integer.MIN_VALUE;
    private int previousHeadphone = Integer.MIN_VALUE;
    private boolean previousMute;

    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!running) return;
            synchronize("poll");
            handler.postDelayed(this, POLL_MS);
        }
    };
    private final Runnable eventSync = () -> synchronize("event");
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            int stream = intent.getIntExtra(STREAM_EXTRA, AudioManager.STREAM_MUSIC);
            Log.i(TAG, "Broadcast action=" + intent.getAction() + " stream=" + stream);
            if (stream == AudioManager.STREAM_MUSIC) scheduleSync(0);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "onServiceConnected pid=" + android.os.Process.myPid()
                + " alreadyRunning=" + running);
        if (running) return;
        running = true;
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        getSharedPreferences("service_state", MODE_PRIVATE).edit()
                .putLong("lastConnected", System.currentTimeMillis())
                .putBoolean("running", true).apply();
        IntentFilter filter = new IntentFilter();
        filter.addAction(VOLUME_ACTION);
        filter.addAction(MUTE_ACTION);
        try {
            registerReceiver(receiver, filter);
            receiverRegistered = true;
            Log.i(TAG, "Volume broadcast receiver registered");
        } catch (Exception e) {
            Log.e(TAG, "Register receiver failed; polling still enabled", e);
        }
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    @Override protected boolean onKeyEvent(KeyEvent event) {
        if (event != null && (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN
                || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_MUTE)) {
            Log.i(TAG, "Key code=" + event.getKeyCode() + " action=" + event.getAction());
            if (event.getAction() == KeyEvent.ACTION_UP) scheduleSync(100);
        }
        return false; // Let TV process the volume key normally.
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { Log.w(TAG, "onInterrupt"); }

    private void scheduleSync(long delay) {
        if (!running) return;
        handler.removeCallbacks(eventSync);
        handler.postDelayed(eventSync, delay);
    }

    private void synchronize(String reason) {
        if (!running) return;
        attempts++;
        try {
            if (audio == null) throw new IllegalStateException("AudioManager is null");
            int main = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            boolean muted = audio.isStreamMute(AudioManager.STREAM_MUSIC);
            long now = SystemClock.uptimeMillis();
            boolean heartbeat = now >= nextStatusLog;
            boolean mainChanged = main != previousMain || muted != previousMute;
            if (heartbeat || mainChanged) {
                Log.i(TAG, "Read #" + attempts + " reason=" + reason + " MUSIC=" + main
                        + " mute=" + muted + " controllerReady=" + (controller != null));
                nextStatusLog = now + STATUS_LOG_MS;
                previousMain = main;
                previousMute = muted;
            }
            if (controller == null) {
                if (now < retryAfter) return;
                retryAfter = now + API_RETRY_MS;
                Log.i(TAG, "Creating PhilipsAudioController");
                try {
                    controller = new PhilipsAudioController(this);
                    Log.i(TAG, "PhilipsAudioController READY");
                } catch (Throwable e) {
                    Log.e(TAG, "PhilipsAudioController INIT FAILED", e);
                    return;
                }
            }
            int target = controller.clamp(muted ? 0 : main);
            int actual = controller.getVolume();
            if (heartbeat || mainChanged || actual != previousHeadphone) {
                Log.i(TAG, "Compare headphone=" + actual + " target=" + target);
                previousHeadphone = actual;
            }
            if (actual != target) {
                Log.i(TAG, "WRITE requested: headphone " + actual + " -> " + target);
                controller.setVolume(target);
                int readback = controller.getVolume();
                Log.i(TAG, "WRITE readback=" + readback + " expected=" + target
                        + " result=" + (readback == target ? "OK" : "MISMATCH"));
                previousHeadphone = readback;
            }
        } catch (Throwable e) {
            Log.e(TAG, "SYNC FAILED reason=" + reason, e);
            controller = null;
            retryAfter = SystemClock.uptimeMillis() + API_RETRY_MS;
        }
    }

    private void stop() {
        Log.i(TAG, "Stopping service work");
        running = false;
        getSharedPreferences("service_state", MODE_PRIVATE).edit()
                .putBoolean("running", false).apply();
        handler.removeCallbacksAndMessages(null);
        if (receiverRegistered) {
            try { unregisterReceiver(receiver); }
            catch (Exception e) { Log.w(TAG, "Unregister receiver failed", e); }
            receiverRegistered = false;
        }
        controller = null;
    }

    @Override public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");
        stop();
        return super.onUnbind(intent);
    }
    @Override public void onDestroy() {
        Log.i(TAG, "onDestroy");
        stop();
        super.onDestroy();
    }
}
