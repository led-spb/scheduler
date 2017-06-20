package ru.led.scheduler.library;

import java.util.Iterator;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.led.scheduler.ServerThread;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;


public class ContextLibrary extends BaseLibrary {
	public ContextLibrary(ServerThread thread) {
		super(thread);
	}
	
	public static Intent createIntent(JSONObject params) throws Exception{
		Intent intent = new Intent();
		if( params.has("action") ) intent.setAction(params.getString("action"));
		if( params.has("data") ){
			if( params.has("type") ) 
				intent.setDataAndType( Uri.parse(params.getString("data")), params.getString("type") );
			else
				intent.setData( Uri.parse(params.getString("data")) );
		}
		if( params.has("flags") ) intent.setFlags( params.getInt("flags") );
		if( params.has("category") ){
		    Object category = params.get("category");
		    if( category instanceof String )
		       intent.addCategory( (String)category );
		    if( category instanceof JSONArray ){
			JSONArray a = (JSONArray)category;
			for( int i=0; i<a.length() ;i++)
			    intent.addCategory( a.getString(i) );
		    }
		}
		
		if( params.has("extra") ){
			JSONObject extra_data = (JSONObject) params.get("extra");
			Log.d("extra_data",extra_data.toString());
			@SuppressWarnings("unchecked")
			Iterator<String> keys = extra_data.keys();
			while( keys.hasNext() ){
				String key = keys.next();
				Object obj = extra_data.get(key);
			
				if( obj instanceof Float)   intent.putExtra(key, (Float)obj );
				else if( obj instanceof Integer) intent.putExtra(key, extra_data.getInt(key) );
				else if( obj instanceof Long)    intent.putExtra(key, extra_data.getLong(key) );
				else intent.putExtra(key, extra_data.getString(key) );
			}
		}
		
		if( params.has("package") && params.has("class") )
			intent.setComponent( new ComponentName(params.getString("package"), params.getString("class") ) );
		else if( params.has("package") )
		    	intent.setPackage( params.getString("package") );
		
		Log.d("intent",intent.toString() );
		return intent;
	}
	
	public JSONObject startActivity(JSONObject params) throws Exception{
		Intent intent = createIntent(params);
		mContext.startActivity(intent);
		
		return OKResponse();
	}
	
	
	public JSONObject startApplication(JSONObject params) throws Exception{
	    	PackageManager pm = mContext.getPackageManager();
	    	Intent intent= pm.getLaunchIntentForPackage( params.getString("package") );
	    	if( params.has("flags") ) intent.setFlags( params.getInt("flags") );
	    	
		mContext.startActivity(intent);
		
		return OKResponse();
	}
	
	public JSONObject sendBroadcast(JSONObject params) throws Exception{
		Intent intent = createIntent(params);
		mContext.sendBroadcast(intent);
		
		return OKResponse();
	}

	public JSONObject startService(JSONObject params) throws Exception{
		Intent intent = createIntent(params);
		mContext.startService(intent);
		
		return OKResponse();
	}
	
	public JSONObject resolverQuery(JSONObject params) throws Exception{
		ContentResolver resolver = mContext.getContentResolver();
		
		Uri uri = Uri.parse(params.getString("url"));
		String where = params.optString("where", null);
		String orderby = params.optString("orderby", null);
		
		JSONArray args = params.optJSONArray("args");
		String[] arguments=null;
		if(args!=null && args.length()>0 ){
			arguments=new String[args.length()];
			for(int i=0;i<args.length();i++)
				arguments[i] = args.getString(i);
		}
		
		Cursor cur = resolver.query(uri, null, where, arguments, orderby);
		return OKResponse( cursorToJson(cur) );
	}
	
	public JSONObject resolverInsert(JSONObject params) throws Exception{
		Uri uri = Uri.parse( params.getString("url") );
		params.remove("url");
		
		ContentValues values = new ContentValues();
		@SuppressWarnings("unchecked")
		Iterator<String> keys = params.keys();
		while( keys.hasNext() ){
			String key=keys.next();
			values.put( key, params.optString(key) );
		}

		Uri event = mContext.getContentResolver().insert( uri, values );
		return OKResponse( event.toString() );	
	}

/*	
	public JSONObject startProcess(JSONObject params) throws Exception{
	    return OKResponse();
	}*/
}
