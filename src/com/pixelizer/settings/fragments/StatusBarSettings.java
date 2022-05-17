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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragment;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;

import com.pixelizer.settings.preferences.SystemSettingMasterSwitchPreference;
import com.pixelizer.settings.preferences.SystemSettingListPreference;
import com.pixelizer.settings.preferences.SystemSettingSwitchPreference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SearchIndexable
public class StatusBarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Indexable {

    private static final String NETWORK_TRAFFIC_STATE = "network_traffic_state";
    private static final String STATUS_BAR_QUICK_QS_PULLDOWN = "qs_quick_pulldown";

    private static final int PULLDOWN_DIR_NONE = 0;
    private static final int PULLDOWN_DIR_RIGHT = 1;
    private static final int PULLDOWN_DIR_LEFT = 2;
    
    private SystemSettingMasterSwitchPreference mNetTrafficState;
    private SystemSettingListPreference mClockPosition;
    private SystemSettingListPreference mQuickPulldown;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.pixelizer_settings_statusbar);
        PreferenceScreen prefSet = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();

        mNetTrafficState = (SystemSettingMasterSwitchPreference)
                findPreference(NETWORK_TRAFFIC_STATE);
        mNetTrafficState.setOnPreferenceChangeListener(this);
        boolean enabled = Settings.System.getInt(resolver,
                NETWORK_TRAFFIC_STATE, 0) == 1;
        mNetTrafficState.setChecked(enabled);
        updateNetTrafficSummary(enabled);

        // Quick Pulldown
        mQuickPulldown = findPreference(STATUS_BAR_QUICK_QS_PULLDOWN);
        mQuickPulldown.setOnPreferenceChangeListener(this);
        updateQuickPulldownSummary(mQuickPulldown.getIntValue(0));

    }

    @Override
    public void onResume() {
        super.onResume();
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
        
            mQuickPulldown.setEntries(R.array.status_bar_quick_qs_pulldown_entries_rtl);
            mQuickPulldown.setEntryValues(R.array.status_bar_quick_qs_pulldown_values);
        }
        updateNetTrafficSummary();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final ContentResolver resolver = getActivity().getContentResolver();
 
        if (preference == mNetTrafficState) {
            boolean enabled = (boolean) objValue;
            Settings.System.putInt(resolver, NETWORK_TRAFFIC_STATE, enabled ? 1 : 0);
            updateNetTrafficSummary(enabled);
            return true;
        } else if (preference == mQuickPulldown) {
            int quickPulldownValue = Integer.valueOf((String) objValue);
            Settings.System.putIntForUser(resolver, Settings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                    quickPulldownValue, UserHandle.USER_CURRENT);
            updateQuickPulldownSummary(quickPulldownValue);
            return true;
	}
        return false;
    }

    private void updateNetTrafficSummary() {
        final boolean enabled = Settings.System.getInt(
                getActivity().getContentResolver(),
                NETWORK_TRAFFIC_STATE, 0) == 1;
        updateNetTrafficSummary(enabled);
    }

    private void updateNetTrafficSummary(boolean enabled) {
        if (mNetTrafficState == null) return;
        String summary = getActivity().getString(R.string.switch_off_text);
        if (enabled) {
            final boolean onStatus = Settings.System.getInt(
                    getActivity().getContentResolver(),
                    Settings.System.NETWORK_TRAFFIC_VIEW_LOCATION, 0) == 0;
            summary = getActivity().getString(R.string.network_traffic_state_summary);
            summary += " " + (onStatus
                    ? getActivity().getString(R.string.traffic_statusbar)
                    : getActivity().getString(R.string.traffic_expanded_statusbar));
        }
        mNetTrafficState.setSummary(summary);
    }


    private void updateQuickPulldownSummary(int value) {
        String summary="";
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL){
            if (value == PULLDOWN_DIR_LEFT) {
                value = PULLDOWN_DIR_RIGHT;
            }else if (value == PULLDOWN_DIR_RIGHT) {
                value = PULLDOWN_DIR_LEFT;
            }
        }
        switch (value) {
            case PULLDOWN_DIR_NONE:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_off);
                break;
            case PULLDOWN_DIR_LEFT:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_summary_left_edge);
                break;
            case PULLDOWN_DIR_RIGHT:
                summary = getResources().getString(
                    R.string.status_bar_quick_qs_pulldown_summary_right_edge);
                break;
        }
        mQuickPulldown.setSummary(summary);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.pixelizer_settings_statusbar;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    return keys;
                }
            };
}
