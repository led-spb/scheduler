package ru.led.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

public class IntentReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if( intent==null) return;
		String action = intent.getAction();
		if( action==null ) return;
		
		if( action.equals( Intent.ACTION_MEDIA_MOUNTED )){
			// auto start when reboot & sdcard ready
            if(  PreferenceManager.getDefaultSharedPreferences(context).getBoolean("auto_start",false) )
			    context.startService( ExecService.getRestartIntent(context) );
		}
	}

}
