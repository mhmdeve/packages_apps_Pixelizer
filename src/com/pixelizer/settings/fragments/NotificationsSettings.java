/*
 * Copyright (C) 2017-2019 The PixelDust Project
     Copyright (C) 2022 PixelPlusUI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pixelizer.settings.fragments;

import com.android.internal.logging.nano.MetricsProto;

import android.os.Bundle;
import com.android.settings.R;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import android.content.ContentResolver;
import androidx.preference.Preference;
import android.provider.Settings;

import com.android.settings.SettingsPreferenceFragment;
import com.pixelizer.settings.Utils;

import com.pixelizer.settings.preferences.CustomSeekBarPreference;
import com.pixelizer.settings.preferences.SystemSettingListPreference;
import com.pixelizer.settings.preferences.SystemSettingMasterSwitchPreference;
import com.pixelizer.settings.preferences.SystemSettingSwitchPreference;

public class NotificationsSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String INCALL_VIB_OPTIONS = "incall_vib_options";
    private static final String PREF_FLASH_ON_CALL = "flashlight_on_call";
    private static final String PREF_FLASH_ON_CALL_DND = "flashlight_on_call_ignore_dnd";
    private static final String PREF_FLASH_ON_CALL_RATE = "flashlight_on_call_rate";

    private CustomSeekBarPreference mFlashOnCallRate;
    private SystemSettingListPreference mFlashOnCall;
    private SystemSettingSwitchPreference mFlashOnCallIgnoreDND;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.pixelizer_settings_notifications);
        PreferenceScreen prefScreen = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

        PreferenceCategory incallVibCategory = (PreferenceCategory) findPreference(INCALL_VIB_OPTIONS);
        if (!Utils.isVoiceCapable(getActivity())) {
            prefScreen.removePreference(incallVibCategory);
        }
        mFlashOnCallRate = (CustomSeekBarPreference)
                findPreference(PREF_FLASH_ON_CALL_RATE);
        int value = Settings.System.getInt(resolver,
                Settings.System.FLASHLIGHT_ON_CALL_RATE, 1);
        mFlashOnCallRate.setValue(value);
        mFlashOnCallRate.setOnPreferenceChangeListener(this);

        mFlashOnCallIgnoreDND = (SystemSettingSwitchPreference)
                findPreference(PREF_FLASH_ON_CALL_DND);
        value = Settings.System.getInt(resolver,
                Settings.System.FLASHLIGHT_ON_CALL, 0);
        mFlashOnCallIgnoreDND.setVisible(value > 1);
        mFlashOnCallRate.setVisible(value != 0);

        mFlashOnCall = (SystemSettingListPreference)
                findPreference(PREF_FLASH_ON_CALL);
        mFlashOnCall.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mFlashOnCall) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putInt(resolver,
                    Settings.System.FLASHLIGHT_ON_CALL, value);
            mFlashOnCallIgnoreDND.setVisible(value > 1);
            mFlashOnCallRate.setVisible(value != 0);
            return true;
        } else if (preference == mFlashOnCallRate) {
            int value = (Integer) newValue;
            Settings.System.putInt(resolver,
                    Settings.System.FLASHLIGHT_ON_CALL_RATE, value);
            return true;
        }
        return false;
    }
    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }
}
