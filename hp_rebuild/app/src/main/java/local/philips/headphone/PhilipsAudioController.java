/*
 * Copyright 2026 Michał Dulko
 * SPDX-License-Identifier: Apache-2.0
 */
package local.philips.headphone;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Adapter for Philips' internal headphone-volume property; uses no modified system files. */
final class PhilipsAudioController {
    private static final String TAG = "PhilipsAudioController";
    private static final int HEADPHONE_VOLUME = 1;
    private static final int TV_SPEAKERS_ONOFF = 32; // Verified by Philips Speaker Diagnostics 1.0: ON=1 OFF=0
    // Philips' settings screen appears to cap headphone volume at 60, even though
    // getPropertyMaxValue(1) reports 100. Do not command above the observed UI limit.
    private static final int SAFE_UI_MAXIMUM = 60;

    private final Object manager;
    private final Method getProperty;
    private final Method setProperty;
    private final int minimum;
    private final int maximum;

    PhilipsAudioController(Context context) throws Exception {
        Context philipsContext = context.createPackageContext("org.droidtv.settings",
                Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        Class<?> type = Class.forName("org.droidtv.tv.audio.TvAudioManagerImpl", true,
                philipsContext.getClassLoader());
        Constructor<?> ctor = type.getConstructor();
        manager = ctor.newInstance();
        getProperty = type.getMethod("getProperty", int.class);
        setProperty = type.getMethod("setProperty", int.class, int.class);
        minimum = number(type.getMethod("getPropertyMinValue", int.class).invoke(manager, HEADPHONE_VOLUME));
        int vendorMaximum = number(type.getMethod("getPropertyMaxValue", int.class).invoke(manager, HEADPHONE_VOLUME));
        maximum = Math.min(vendorMaximum, SAFE_UI_MAXIMUM);
        if (minimum < 0 || maximum < minimum) {
            throw new IllegalStateException("Unexpected headphone range: " + minimum + ".." + vendorMaximum);
        }
        Log.i(TAG, "Ready: property=" + HEADPHONE_VOLUME + " vendorRange="
                + minimum + ".." + vendorMaximum + " effectiveRange=" + minimum + ".." + maximum);
    }

    /** True if the vendor reports that built-in TV speakers are enabled. */
    boolean areTvSpeakersEnabled() throws Exception {
        int raw = number(invoke(getProperty, TV_SPEAKERS_ONOFF));
        if (raw != 0 && raw != 1) {
            throw new IllegalStateException("Unexpected TV speaker property value: " + raw);
        }
        return raw == 1;
    }

    /** Explicit, user-triggered action only; never used by the background volume synchronizer. */
    void setTvSpeakersEnabled(boolean enabled) throws Exception {
        Log.i(TAG, "TV_SPEAKERS request property=32 target=" + (enabled ? 1 : 0));
        invoke(setProperty, TV_SPEAKERS_ONOFF, enabled ? 1 : 0);
    }

    int getVolume() throws Exception {
        return number(invoke(getProperty, HEADPHONE_VOLUME));
    }

    int clamp(int value) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    void setVolume(int value) throws Exception {
        int safe = clamp(value);
        invoke(setProperty, HEADPHONE_VOLUME, safe);
    }

    private Object invoke(Method method, Object... args) throws Exception {
        try {
            return method.invoke(manager, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw e;
        }
    }

    private static int number(Object value) {
        return ((Number) value).intValue();
    }
}
