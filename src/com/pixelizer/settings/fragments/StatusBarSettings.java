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
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.View;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.pixelizer.settings.preferences.SystemSettingListPreference;
import com.pixelizer.settings.preferences.SystemSettingMasterSwitchPreference;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class StatusBarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, Indexable {

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
    private static final String NETWORK_TRAFFIC_STATE = "network_traffic_state";
    private static final String STATUS_BAR_QUICK_QS_PULLDOWN = "qs_quick_pulldown";
    private static final int PULLDOWN_DIR_NONE = 0;
    private static final int PULLDOWN_DIR_RIGHT = 1;
    private static final int PULLDOWN_DIR_LEFT = 2;
    private static final String PREF_STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String PREF_STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final int BATTERY_STYLE_PORTRAIT = 0;
    private static final int BATTERY_STYLE_TEXT = 4;
    private static final int BATTERY_STYLE_HIDDEN = 5;
    private static final int BATTERY_PERCENT_HIDDEN = 0;
    private static final int BATTERY_PERCENT_SHOW = 2;
    private ListPreference mBatteryPercent;
    private ListPreference mBatteryStyle;
    private int mBatteryPercentValue;
    private int mBatteryPercentValuePrev;
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

        int batterystyle = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);

        mBatteryStyle = (ListPreference) findPreference(PREF_STATUS_BAR_BATTERY_STYLE);
        mBatteryStyle.setValue(String.valueOf(batterystyle));
        mBatteryStyle.setSummary(mBatteryStyle.getEntry());
        mBatteryStyle.setOnPreferenceChangeListener(this);

        mBatteryPercentValue = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0, UserHandle.USER_CURRENT);
        mBatteryPercentValuePrev = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT + "_prev", -1, UserHandle.USER_CURRENT);

        mBatteryPercent = (ListPreference) findPreference(PREF_STATUS_BAR_SHOW_BATTERY_PERCENT);
        mBatteryPercent.setValue(String.valueOf(mBatteryPercentValue));
        mBatteryPercent.setSummary(mBatteryPercent.getEntry());
        mBatteryPercent.setOnPreferenceChangeListener(this);
        updateBatteryOptions(batterystyle, mBatteryPercentValue);
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
        } else if (preference == mBatteryStyle) {
            int batterystyle = Integer.parseInt((String) objValue);
            updateBatteryOptions(batterystyle, mBatteryPercentValue);
            int index = mBatteryStyle.findIndexOfValue((String) objValue);
            mBatteryStyle.setSummary(mBatteryStyle.getEntries()[index]);
            return true;
        } else if (preference == mBatteryPercent) {
            mBatteryPercentValue = Integer.parseInt((String) objValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, mBatteryPercentValue,
                    UserHandle.USER_CURRENT);
            int index = mBatteryPercent.findIndexOfValue((String) objValue);
            mBatteryPercent.setSummary(mBatteryPercent.getEntries()[index]);
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

    private void updateBatteryOptions(int batterystyle, int batterypercent) {
        ContentResolver resolver = getActivity().getContentResolver();
        switch (batterystyle) {
            case BATTERY_STYLE_TEXT:
                handleTextPercentage(BATTERY_PERCENT_SHOW);
                break;
            case BATTERY_STYLE_HIDDEN:
                handleTextPercentage(BATTERY_PERCENT_HIDDEN);
                break;
            default:
                mBatteryPercent.setEnabled(true);
                if (mBatteryPercentValuePrev != -1) {
                    Settings.System.putIntForUser(resolver,
                            Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
                            mBatteryPercentValuePrev, UserHandle.USER_CURRENT);
                    Settings.System.putIntForUser(resolver,
                            Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT + "_prev",
                            -1, UserHandle.USER_CURRENT);
                    mBatteryPercentValue = mBatteryPercentValuePrev;
                    mBatteryPercentValuePrev = -1;
                    int index = mBatteryPercent.findIndexOfValue(String.valueOf(mBatteryPercentValue));
                    mBatteryPercent.setSummary(mBatteryPercent.getEntries()[index]);
                }

                Settings.System.putIntForUser(resolver,
                        Settings.System.STATUS_BAR_BATTERY_STYLE, batterystyle,
                        UserHandle.USER_CURRENT);
                break;
        }
    }

    private void updateQuickPulldownSummary(int value) {
        String summary = "";
        if (getResources().getConfiguration().getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            if (value == PULLDOWN_DIR_LEFT) {
                value = PULLDOWN_DIR_RIGHT;
            } else if (value == PULLDOWN_DIR_RIGHT) {
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

    private void handleTextPercentage(int batterypercent) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (mBatteryPercentValuePrev == -1) {
            mBatteryPercentValuePrev = mBatteryPercentValue;
            Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT + "_prev",
                    mBatteryPercentValue, UserHandle.USER_CURRENT);
        }
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
                batterypercent, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_TEXT,
                UserHandle.USER_CURRENT);
        int index = mBatteryPercent.findIndexOfValue(String.valueOf(batterypercent));
        mBatteryPercent.setSummary(mBatteryPercent.getEntries()[index]);
        mBatteryPercent.setEnabled(false);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }
}
