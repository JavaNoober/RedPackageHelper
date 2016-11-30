package com.xiaoqi.redpackagehelper;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * <p>Created 16/2/5 下午9:06.</p>
 */
public class BaseSettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName(Config.PREFERENCE_NAME);
    }
}
