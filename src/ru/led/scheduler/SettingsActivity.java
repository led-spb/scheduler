package ru.led.scheduler;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import ru.led.scheduler.R;

/**
 * Created by Alexey.Ponimash on 21.04.2015.
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstance){
        super.onCreate(savedInstance);
        addPreferencesFromResource(R.xml.settiings_prefs);
    }
}
