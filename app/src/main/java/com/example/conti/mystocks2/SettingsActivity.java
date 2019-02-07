package com.example.conti.mystocks2;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

/**
 * Settings
 */
public class SettingsActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_settings);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment
            implements Preference.OnPreferenceChangeListener
//            implements SharedPreferences.OnSharedPreferenceChangeListener
    {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            Preference prefStocklist = findPreference(getString(R.string.pref_stocklist_key));
            prefStocklist.setOnPreferenceChangeListener(this);

            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String s = sharedPrefs.getString(prefStocklist.getKey(), "");
            onPreferenceChange(prefStocklist, s);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value)
        {
            if (preference.getKey().equals( getString(R.string.pref_stocklist_key) )) {
                preference.setSummary( value.toString() );
            }
            return true;
        }

//        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
//        {
//            if (key.equals(KEY_PREF_SYNC_CONN)) {
//                Preference connectionPref = findPreference(key);
//                // Set summary to be the user-description for the selected value
//                connectionPref.setSummary(sharedPreferences.getString(key, ""));
//            }
//        }
    }
}
