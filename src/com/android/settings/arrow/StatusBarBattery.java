/*
 * Copyright (C) 2017-2019 The Dirty Unicorns Project
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
package com.android.settings.arrow;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.PreferenceCategory;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.arrow.support.preferences.SystemSettingListPreference;
import com.arrow.support.preferences.SystemSettingSwitchPreference;

import java.util.ArrayList;
import java.util.List;

public class StatusBarBattery extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";
    private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
    private static final String STATUS_BAR_BATTERY_TEXT_CHARGING = "status_bar_battery_text_charging";

    private SwitchPreference mBatteryTextCharging;

    private SystemSettingListPreference mBatteryPercent;
    private SystemSettingListPreference mBatteryStyle;

    private static final int BATTERY_STYLE_PORTRAIT = 0;
    private static final int BATTERY_STYLE_TEXT = 4;
    private static final int BATTERY_STYLE_HIDDEN = 5;
    private static final int BATTERY_PERCENT_HIDDEN = 0;
    private static final int BATTERY_STYLE_IOS16 = 18;

    private static final int BATTERY_PERCENT_INSIDE = 1;
    private static final int BATTERY_PERCENT_RIGHT = 2;
    private static final int BATTERY_PERCENT_LEFT = 3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.statusbar_battery);

        int batterystyle = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);
        int batterypercent = Settings.System.getIntForUser(getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0, UserHandle.USER_CURRENT);

        mBatteryStyle = (SystemSettingListPreference) findPreference(STATUS_BAR_BATTERY_STYLE);
        mBatteryStyle.setOnPreferenceChangeListener(this);

        mBatteryPercent = (SystemSettingListPreference) findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);
        mBatteryPercent.setOnPreferenceChangeListener(this);

        handleBatteryPercent(batterystyle, batterypercent);

        mBatteryTextCharging = (SwitchPreference) findPreference(STATUS_BAR_BATTERY_TEXT_CHARGING);
        mBatteryTextCharging.setEnabled(batterystyle != BATTERY_STYLE_TEXT &&
                (batterypercent == BATTERY_PERCENT_INSIDE || batterypercent == BATTERY_PERCENT_HIDDEN));
    }

    private void handleBatteryPercent(int batterystyle, int batterypercent) {
        if (batterystyle < BATTERY_STYLE_TEXT) {
            mBatteryPercent.setEntries(R.array.status_bar_battery_percent_entries);
            mBatteryPercent.setEntryValues(R.array.status_bar_battery_percent_values);;
        }
        else {
            mBatteryPercent.setEntries(R.array.status_bar_battery_percent_no_text_inside_entries);
            mBatteryPercent.setEntryValues(R.array.status_bar_battery_percent_no_text_inside_values);
            if (batterystyle == BATTERY_STYLE_IOS16) {
                if (batterypercent != BATTERY_PERCENT_INSIDE) {
                    batterypercent = BATTERY_PERCENT_INSIDE;
                    mBatteryPercent.setValueIndex(BATTERY_PERCENT_INSIDE);
                }
            } else if (batterypercent == BATTERY_PERCENT_INSIDE) {
                batterypercent = BATTERY_PERCENT_HIDDEN;
                mBatteryPercent.setValueIndex(BATTERY_PERCENT_HIDDEN);
            }
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT,
                    batterypercent, UserHandle.USER_CURRENT);
        }

        mBatteryPercent.setEnabled(
                batterystyle != BATTERY_STYLE_TEXT &&
                batterystyle != BATTERY_STYLE_HIDDEN &&
                batterystyle != BATTERY_STYLE_IOS16);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mBatteryStyle) {
            int batterystyle = Integer.parseInt((String) newValue);
            int batterypercent = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0, UserHandle.USER_CURRENT);
            mBatteryTextCharging.setEnabled(batterystyle != BATTERY_STYLE_TEXT &&
                    (batterypercent == BATTERY_PERCENT_INSIDE || batterypercent == BATTERY_PERCENT_HIDDEN));
            return true;
        } else if (preference == mBatteryPercent) {
            int value = Integer.parseInt((String) newValue);
            int batterystyle = Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);
            mBatteryTextCharging.setEnabled(batterystyle == BATTERY_STYLE_HIDDEN ||
                    (batterystyle != BATTERY_STYLE_TEXT && value != 2));
            return true;
        }
        return false;
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_STYLE, BATTERY_STYLE_PORTRAIT, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.STATUS_BAR_BATTERY_TEXT_CHARGING, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_BATTERY_STYLE, -1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_SHOW_BATTERY_PERCENT, 2, UserHandle.USER_CURRENT);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ARROW;
    }
}
