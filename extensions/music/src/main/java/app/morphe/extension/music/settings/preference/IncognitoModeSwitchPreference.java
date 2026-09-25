/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.music.settings.preference;

import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

import app.morphe.extension.music.patches.privacy.IncognitoModePatch;

/**
 * SwitchPreference that, in addition to persisting its own value, immediately drives the real
 * Google account "Pause watch history" / "Pause search history" switches to match, via
 * {@link IncognitoModePatch}.
 */
@SuppressWarnings({"unused", "deprecation"})
public class IncognitoModeSwitchPreference extends SwitchPreference {

    public IncognitoModeSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        installChangeListener();
    }

    public IncognitoModeSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        installChangeListener();
    }

    public IncognitoModeSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        installChangeListener();
    }

    public IncognitoModeSwitchPreference(Context context) {
        super(context);
        installChangeListener();
    }

    private void installChangeListener() {
        setOnPreferenceChangeListener((preference, newValue) -> {
            IncognitoModePatch.toggle(preference.getContext(), Boolean.TRUE.equals(newValue));
            return true;
        });
    }
}
