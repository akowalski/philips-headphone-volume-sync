/*
 * Copyright 2026 Michał Dulko
 * SPDX-License-Identifier: Apache-2.0
 */
package local.philips.headphone;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/** Configuration activity launched from the Accessibility service's Settings action. */
public final class SpeakerSettingsActivity extends Activity {
    private static final String TAG = "SpeakerSettings";
    private Switch speakerSwitch;
    private TextView status;
    private boolean updatingUi;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle(R.string.speaker_settings_title);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 28, 32, 28);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText(R.string.speaker_settings_title);
        title.setTextSize(24);
        layout.addView(title);

        TextView help = new TextView(this);
        help.setText(R.string.speaker_settings_description);
        help.setTextSize(16);
        help.setPadding(0, 14, 0, 22);
        layout.addView(help);

        speakerSwitch = new Switch(this);
        speakerSwitch.setText(R.string.speaker_settings_switch);
        speakerSwitch.setTextSize(19);
        speakerSwitch.setMinHeight(62);
        speakerSwitch.setFocusable(true);
        speakerSwitch.setOnCheckedChangeListener((buttonView, requested) -> {
            if (!updatingUi) confirmSpeakerChange(requested);
        });
        layout.addView(speakerSwitch);

        status = new TextView(this);
        status.setTextSize(17);
        status.setPadding(0, 18, 0, 16);
        layout.addView(status);

        Button refresh = new Button(this);
        refresh.setText(R.string.speaker_settings_refresh);
        refresh.setBackgroundTintList(null);
        refresh.setBackgroundResource(R.drawable.tv_button_bg);
        refresh.setTextColor(getColorStateList(R.color.tv_button_text));
        refresh.setTextSize(16);
        refresh.setAllCaps(false);
        refresh.setFocusable(true);
        refresh.setMinHeight(54);
        refresh.setOnClickListener(v -> refreshState());
        layout.addView(refresh);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);
        setContentView(scrollView);
        refreshState();
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) refreshState();
    }

    private void showSwitchState(boolean enabled) {
        updatingUi = true;
        try {
            speakerSwitch.setChecked(enabled);
        } finally {
            updatingUi = false;
        }
    }

    private void refreshState() {
        try {
            boolean enabled = new PhilipsAudioController(this).areTvSpeakersEnabled();
            showSwitchState(enabled);
            speakerSwitch.setEnabled(true);
            status.setText(getString(R.string.speaker_settings_current,
                    getString(enabled ? R.string.speakers_state_on : R.string.speakers_state_off)));
            Log.i(TAG, "TV_SPEAKERS read property=32 value=" + (enabled ? 1 : 0));
        } catch (Throwable e) {
            speakerSwitch.setEnabled(false);
            status.setText(getString(R.string.speakers_read_error) + ": " + e);
            Log.e(TAG, "TV_SPEAKERS read failed", e);
        }
    }

    private void confirmSpeakerChange(boolean enabled) {
        // Switch immediately reflects user input; revert display until the change is confirmed.
        showSwitchState(!enabled);
        new AlertDialog.Builder(this)
                .setTitle(enabled ? R.string.speakers_confirm_on_title : R.string.speakers_confirm_off_title)
                .setMessage(R.string.speaker_settings_confirm_message)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> refreshState())
                .setOnCancelListener(dialog -> refreshState())
                .setPositiveButton(R.string.speakers_confirm_apply, (dialog, which) -> apply(enabled))
                .show();
    }

    private void apply(boolean enabled) {
        try {
            PhilipsAudioController controller = new PhilipsAudioController(this);
            boolean before = controller.areTvSpeakersEnabled();
            Log.i(TAG, "TV_SPEAKERS before=" + (before ? 1 : 0) + " requested=" + (enabled ? 1 : 0));
            if (before != enabled) controller.setTvSpeakersEnabled(enabled);
            boolean after = controller.areTvSpeakersEnabled();
            Log.i(TAG, "TV_SPEAKERS after=" + (after ? 1 : 0) + " verified=" + (after == enabled));
            showSwitchState(after);
            status.setText(getString(after == enabled ? R.string.speakers_result_ok : R.string.speakers_result_mismatch)
                    + " " + getString(R.string.speaker_settings_current,
                    getString(after ? R.string.speakers_state_on : R.string.speakers_state_off)));
        } catch (Throwable e) {
            status.setText(getString(R.string.speakers_write_error) + ": " + e);
            Log.e(TAG, "TV_SPEAKERS write failed", e);
            refreshState();
        }
    }
}
