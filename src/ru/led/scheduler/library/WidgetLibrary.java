package ru.led.scheduler.library;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.led.scheduler.ServerThread;
import ru.led.scheduler.WidgetReceiver;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetLibrary extends BaseLibrary {
    	private PackageManager pm;
    
	public WidgetLibrary(ServerThread thread) {
		super(thread);
		pm = mContext.getPackageManager();
	}
	
	public JSONObject setView(JSONObject params) throws Exception{
		int appWidgetId = params.getInt("widget");
		String layout_name = params.getString("layout");
		String package_name = params.optString("package", mContext.getPackageName() );
		
		Resources res = pm.getResourcesForApplication( package_name );  
		int layout_id = res.getIdentifier(layout_name, "layout", package_name);
		
		Log.d("setView", 
				String.format("id:%d package:%s layout:%s id:%d", appWidgetId, package_name,layout_name, layout_id) 
		);
		
		RemoteViews view = new RemoteViews(package_name, layout_id);
		
		if( params.has("data") ){
			JSONObject data = params.optJSONObject("data");

			for(Iterator<String> it=data.keys();it.hasNext();){
				String view_name = it.next();
				int viewId = res.getIdentifier(view_name, "id", package_name);
				
				
				JSONObject param_values = data.getJSONObject(view_name);
				
				for(Iterator<String> i=param_values.keys();i.hasNext();){
					String value_type = i.next();
					JSONObject value = param_values.getJSONObject(value_type);
					
					if(value_type.equals("text"))
						setTextParams(view, viewId, value);

					if(value_type.equals("image"))
						setImageParams(view, viewId, value);
					
					if(value_type.equals("intent"))
						setIntentParams(view, viewId, value);
				}
			}
		}
		
		AppWidgetManager widgetMan = AppWidgetManager.getInstance(mContext);
		widgetMan.updateAppWidget(appWidgetId, view);
		
		return OKResponse();
	}
	
	private void setTextParams(RemoteViews v, int viewId, JSONObject values ) throws Exception{
		for(Iterator<String> it=values.keys();it.hasNext();){
			String what = it.next();
			if( what.equals("value") )
				v.setTextViewText(viewId, values.getString(what) );
			if( what.equals("color") )
				v.setTextColor(viewId, values.getInt(what) );
		}
	}
	private void setImageParams(RemoteViews v, int viewId, JSONObject values ) throws Exception{
		for(Iterator<String> it=values.keys();it.hasNext();){
			String what = it.next();
			if( what.equals("url") )
				v.setImageViewUri(viewId, Uri.parse(values.getString(what)) );
		}		
	}
	private void setIntentParams(RemoteViews v, int viewId, JSONObject values ) throws Exception{
		Intent intent = ContextLibrary.createIntent( values );
		PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, 0);
		
		v.setOnClickPendingIntent( viewId, pendingIntent );		
	}
	
	
	public JSONObject getWidgetParams(JSONObject params) throws Exception{
		int appWidgetId = params.getInt("widget");
		SharedPreferences prefs = mContext.getSharedPreferences("widget_"+appWidgetId, 0);
		return new JSONObject( prefs.getAll() );
	}
	
	
	public JSONObject getWidgetList(JSONObject params) throws Exception{
		AppWidgetManager widgetMan = AppWidgetManager.getInstance(mContext);
		
		String package_str = params.optString("package", mContext.getPackageName() );
		
		JSONArray result = new JSONArray();
		Iterator<AppWidgetProviderInfo> it = widgetMan.getInstalledProviders().iterator();
		
		while( it.hasNext() ){
			AppWidgetProviderInfo info = it.next();
			
			if( package_str==null || package_str.equals(info.provider.getPackageName()) ){
				int[] widgets = widgetMan.getAppWidgetIds(info.provider);
				
				for(int i=0;i<widgets.length;i++){
					result.put( 
						new JSONObject()
							.put("provider", info.provider.getClassName())
							.put("id", widgets[i] )
					);
				}
			}
		}
		return OKResponse(result);
	}
	
	
	public JSONObject refreshWidgets(JSONObject params) throws Exception{
		mContext.sendBroadcast( new Intent(WidgetReceiver.WIDGET_REFRESH_ACTION) );
		return OKResponse();
	}
}
