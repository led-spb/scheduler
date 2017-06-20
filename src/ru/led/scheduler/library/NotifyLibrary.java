package ru.led.scheduler.library;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.led.scheduler.ExecService;
import ru.led.scheduler.R;
import ru.led.scheduler.ServerThread;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class NotifyLibrary extends BaseLibrary {
	
	public NotifyLibrary(ServerThread thread) {
		super(thread);
	}

	public JSONObject toast(JSONObject params) throws Exception{
		final String message = params.getString("message");

		mHandler.post( new Runnable(){
			public void run() {
				Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
			}
		});
		
		return OKResponse();
	}
	
	private PendingIntent jsObjectToPendingIntent(JSONObject obj) throws Exception{
	        String intentType = obj.optString("type", "activity");
	        
		Intent intent = ContextLibrary.createIntent( obj );
		PendingIntent pi = null;
		if( intentType.equals("activity") )
		    pi = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT );
		else if( intentType.equals("service") )
		    pi = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT );
		else if( intentType.equals("broadcast") )
		    pi = PendingIntent.getBroadcast(mContext, 0, intent, 0 );

		return pi;
	}
	
	public JSONObject notify(JSONObject params) throws Exception{
		NotificationManager notifyMan = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		
		int id = params.getInt("id");

		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext)
			.setContentTitle( params.getString("title") )
			.setContentText( params.getString("message") )
			.setSmallIcon( params.optInt("icon", R.drawable.ic_stat_notify) )
			.setWhen( System.currentTimeMillis() )
			.setAutoCancel( params.optBoolean("autocancel",true) )
			.setOngoing( params.optBoolean("ongoing", false) );
		
		if( params.has("intent") ){
			PendingIntent pi = jsObjectToPendingIntent( params.getJSONObject("intent") );
			
			if(pi!=null){
			    builder.setContentIntent( pi );
			}
		}
		
		if( params.has("actions") ){
		    JSONArray acts = params.getJSONArray("actions");
		    for( int idx=0; idx<acts.length(); idx++ ){
                JSONObject action = acts.getJSONObject(idx);
                PendingIntent pi = jsObjectToPendingIntent( action.getJSONObject("intent") );
                String  title = action.getString("title");
                int     icon  = action.optInt("icon", R.drawable.ic_launcher );
                Log.d("notify-action", "Add action"+title);
                builder.addAction(icon, title, pi);
		    }
		}
		
		if( params.has("number") ){
			builder.setNumber( params.getInt("number"));
		}
		
		if( params.has("progress") ){
			JSONObject progress = params.getJSONObject("progress");
			builder.setProgress(progress.optInt("max", 0) , progress.optInt("progress", 0), progress.optBoolean("indeterminate", false) );
		}
		
		if( params.has("priority") ){
		    builder.setPriority(  params.getInt("priority") );
		}
		
		if( params.has("subtext") ){
		    builder.setSubText( params.getString("subtext") );
		}
			
		notifyMan.notify(id, builder.build() );
		return OKResponse();
	}
	
	
	public JSONObject cancel(JSONObject params) throws Exception{
		NotificationManager notifyMan = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
		notifyMan.cancel( params.getInt("id") );
		return OKResponse();
	}
		
	public JSONObject serviceNotify(JSONObject params) throws JSONException {
		mContext.sendBroadcast( new Intent(ExecService.SET_NOTIFY_ACTION).putExtra("message", params.getString("message")) );
		return OKResponse();
	}
}
