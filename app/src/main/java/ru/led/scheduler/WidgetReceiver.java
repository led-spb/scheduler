package ru.led.scheduler;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class WidgetReceiver extends AppWidgetProvider {
	public final static String WIDGET_REFRESH_ACTION = "ru.led.scheduler.WIDGET_REFRESH";
	
	
	public WidgetReceiver(){
		super();
	}
	
	@Override
	public void onReceive(Context context, Intent intent){
		if(intent!=null && intent.getAction()!=null && intent.getAction().equals(WIDGET_REFRESH_ACTION) ){
			AppWidgetManager man = AppWidgetManager.getInstance(context);
			int[] widgets = man.getAppWidgetIds( new ComponentName(context, this.getClass() ) );
			onUpdate(context,man,widgets);
		}else
			super.onReceive(context, intent);
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.i("AppWidgetProvider", "onUpdate");
        final int N = appWidgetIds.length;
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int i=0; i<N; i++) {
        	int widgetId = appWidgetIds[i];
        	SharedPreferences prefs = context.getSharedPreferences("widget_"+widgetId, 0);
        	String widget = prefs.getString("widget_name", "");

        	ExecService.executeWidget(context, widget, widgetId);
        }
    }
	
	
	@Override
	public void onDeleted (Context context, int[] appWidgetIds){
		super.onDeleted(context, appWidgetIds);
		
		for(int i=0;i<appWidgetIds.length;i++)
		  context.getSharedPreferences("widget_"+appWidgetIds[i], 0).edit().clear().commit();
	}
}
