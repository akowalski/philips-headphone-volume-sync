/*
 * Copyright 2026 Michał Dulko
 * SPDX-License-Identifier: Apache-2.0
 */
package local.philips.headphone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Visible manual diagnosis that runs independently of AccessibilityService. */
public final class MainActivity extends Activity {
    private static final String TAG = "HeadphoneDiagUI";
    private TextView report;
    private TextView speakerStatus;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(30, 22, 30, 22);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView title = new TextView(this);
        title.setText(getString(R.string.diag_title));
        title.setTextSize(24);
        content.addView(title);
        TextView hint = new TextView(this);
        hint.setText(getString(R.string.diag_hint));
        hint.setTextSize(16);
        content.addView(hint);
        Button read = new Button(this);
        styleButton(read, R.string.button_read);
        read.setOnClickListener(v -> inspect(false));
        content.addView(read);
        Button sync = new Button(this);
        styleButton(sync, R.string.button_sync);
        sync.setOnClickListener(v -> inspect(true));
        content.addView(sync);
        TextView speakerHeading = new TextView(this);
        speakerHeading.setText(R.string.speakers_title);
        speakerHeading.setTextSize(21);
        speakerHeading.setPadding(0, 22, 0, 6);
        content.addView(speakerHeading);
        TextView speakerHint = new TextView(this);
        speakerHint.setText(R.string.speakers_hint);
        speakerHint.setTextSize(15);
        content.addView(speakerHint);
        speakerStatus = new TextView(this);
        speakerStatus.setTextSize(17);
        speakerStatus.setPadding(0, 12, 0, 6);
        content.addView(speakerStatus);
        Button speakerRead = new Button(this);
        styleButton(speakerRead, R.string.speakers_read);
        speakerRead.setOnClickListener(v -> refreshSpeakerStatus());
        content.addView(speakerRead);
        Button speakerOn = new Button(this);
        styleButton(speakerOn, R.string.speakers_on);
        speakerOn.setOnClickListener(v -> confirmSpeakerChange(true));
        content.addView(speakerOn);
        Button speakerOff = new Button(this);
        styleButton(speakerOff, R.string.speakers_off);
        speakerOff.setOnClickListener(v -> confirmSpeakerChange(false));
        content.addView(speakerOff);
        TextView enableHint = new TextView(this);
        enableHint.setText(R.string.enable_accessibility_hint);
        enableHint.setTextSize(16);
        enableHint.setPadding(0, 18, 0, 8);
        content.addView(enableHint);
        Button settings = new Button(this);
        styleButton(settings, R.string.button_accessibility);
        settings.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
            catch (Exception e) { report.setText(getString(R.string.settings_error) + ": " + e); }
        });
        content.addView(settings);
        report = new TextView(this);
        report.setTextSize(17);
        report.setTextIsSelectable(true);
        content.addView(report);
        ScrollView scroller = new ScrollView(this);
        scroller.addView(content);
        setContentView(scroller);
        inspect(false);
        refreshSpeakerStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        // Handles speaker changes made manually in Philips Settings while this screen was away.
        if (speakerStatus != null) refreshSpeakerStatus();
    }

    private void refreshSpeakerStatus() {
        try {
            PhilipsAudioController controller = new PhilipsAudioController(this);
            boolean on = controller.areTvSpeakersEnabled();
            speakerStatus.setText(getString(R.string.speakers_current,
                    getString(on ? R.string.speakers_state_on : R.string.speakers_state_off)));
            Log.i(TAG, "TV_SPEAKERS read property=32 value=" + (on ? 1 : 0));
        } catch (Throwable e) {
            speakerStatus.setText(getString(R.string.speakers_read_error) + ": " + e);
            Log.e(TAG, "TV_SPEAKERS read failed", e);
        }
    }

    private void confirmSpeakerChange(boolean enabled) {
        // A wrong vendor property write could alter routing; never change anything silently.
        new AlertDialog.Builder(this)
                .setTitle(enabled ? R.string.speakers_confirm_on_title : R.string.speakers_confirm_off_title)
                .setMessage(R.string.speakers_confirm_message)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> { })
                .setPositiveButton(R.string.speakers_confirm_apply,
                        (dialog, which) -> applySpeakerChange(enabled))
                .show();
    }

    private void applySpeakerChange(boolean enabled) {
        try {
            PhilipsAudioController controller = new PhilipsAudioController(this);
            boolean before = controller.areTvSpeakersEnabled();
            Log.i(TAG, "TV_SPEAKERS before=" + (before ? 1 : 0) + " requested=" + (enabled ? 1 : 0));
            if (before != enabled) controller.setTvSpeakersEnabled(enabled);
            boolean after = controller.areTvSpeakersEnabled();
            Log.i(TAG, "TV_SPEAKERS after=" + (after ? 1 : 0) + " verified=" + (after == enabled));
            speakerStatus.setText(getString(after == enabled ? R.string.speakers_result_ok : R.string.speakers_result_mismatch)
                    + "  " + getString(R.string.speakers_current,
                    getString(after ? R.string.speakers_state_on : R.string.speakers_state_off)));
        } catch (Throwable e) {
            speakerStatus.setText(getString(R.string.speakers_write_error) + ": " + e);
            Log.e(TAG, "TV_SPEAKERS write failed", e);
        }
    }

    private void styleButton(Button button, int textResource) {
        button.setText(textResource);
        // Keep the same light-blue D-pad focus highlight on Philips Android TV.
        button.setBackgroundTintList(null);
        button.setBackgroundResource(R.drawable.tv_button_bg);
        button.setTextColor(getColorStateList(R.color.tv_button_text));
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setMinHeight(54);
        button.setFocusable(true);
    }

    private void inspect(boolean doWrite) {
        StringBuilder sb = new StringBuilder();
        try {
            String enabledList = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            int master = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0);
            String ownService = new ComponentName(this, HeadphoneKeyService.class).flattenToString();
            boolean listed = enabledList != null && enabledList.contains(ownService);
            sb.append(getString(R.string.accessibility_global)).append(": ").append(master).append('\n');
            sb.append(getString(R.string.service_listed)).append(": ").append(listed).append('\n');
            sb.append(getString(R.string.last_connected)).append(": ")
                    .append(getSharedPreferences("service_state", MODE_PRIVATE)
                            .getLong("lastConnected", 0)).append("\n\n");
            AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
            int main = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            boolean mute = audio.isStreamMute(AudioManager.STREAM_MUSIC);
            sb.append("MUSIC: ").append(main).append(" / ")
                    .append(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
                    .append("  MUTE: ").append(mute).append('\n');
            Log.i(TAG, "UI main=" + main + " muted=" + mute + " manualWrite=" + doWrite);
            PhilipsAudioController controller = new PhilipsAudioController(this);
            int oldVolume = controller.getVolume();
            sb.append(getString(R.string.headphone_before)).append(": ").append(oldVolume).append('\n');
            int target = controller.clamp(mute ? 0 : main);
            sb.append(getString(R.string.expected_volume)).append(": ").append(target).append('\n');
            if (doWrite) {
                controller.setVolume(target);
                int after = controller.getVolume();
                sb.append(getString(R.string.after_write)).append(": ").append(after).append('\n');
                sb.append(getString(R.string.result)).append(": ")
                        .append(after == target ? "OK" : getString(R.string.not_matching)).append('\n');
                Log.i(TAG, "UI manual sync old=" + oldVolume + " target=" + target
                        + " readback=" + after);
            } else {
                sb.append(getString(R.string.read_only)).append('\n');
            }
        } catch (Throwable e) {
            sb.append(getString(R.string.error)).append(": ").append(e).append('\n');
            Throwable cause = e.getCause();
            if (cause != null) sb.append(getString(R.string.cause)).append(": ").append(cause).append('\n');
            Log.e(TAG, "UI diagnostic failed", e);
        }
        report.setText(sb.toString());
    }
}
