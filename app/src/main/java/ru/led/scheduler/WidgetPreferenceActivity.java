package ru.led.scheduler;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class WidgetPreferenceActivity extends PreferenceActivity {
	private int mAppWidgetId = 0;
	private String mWidgetName = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		
		if(intent==null){
			finish();
			return;
		}
		
		try {
			mAppWidgetId = intent.getIntExtra("widget_id", 0);
			mWidgetName = intent.getStringExtra("widget_name");
			JSONArray widgetPrefs = new JSONArray( intent.getStringExtra("widget_prefs") );
			

			PreferenceManager prefMan = getPreferenceManager();
			prefMan.setSharedPreferencesName("widget_"+mAppWidgetId);
			
			SharedPreferences prefs = prefMan.getSharedPreferences();
			prefs.edit().putInt("widget_id", mAppWidgetId).putString("widget_name", mWidgetName).commit();
			
			if( widgetPrefs.length()==0 ){
				finish();
				return;
			}
			
			PreferenceScreen screen = prefMan.createPreferenceScreen(this);
			for(int i=0;i<widgetPrefs.length();i++){
				EditTextPreference preference = new EditTextPreference(this);
				
				preference.setKey( widgetPrefs.optString(i) );
				preference.setTitle( widgetPrefs.optString(i) );
				
				screen.addPreference(preference);
			}
			
			setPreferenceScreen(screen);
		} catch (JSONException e) {
			finish();
			return;
		} 

	}
	
	
	@Override
	protected void onStop(){
		super.onStop();
		ExecService.executeWidget(this, mWidgetName, mAppWidgetId);
	}	
	
}
