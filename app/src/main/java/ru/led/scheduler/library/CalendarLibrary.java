package ru.led.scheduler.library;

import java.util.Iterator;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import ru.led.scheduler.ServerThread;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;


public class CalendarLibrary extends BaseLibrary {
	public CalendarLibrary(ServerThread thread) {
		super(thread);
	}
/*
	@Override
	public String getLibraryName() {
		return "calendar";
	}
*/	
	
	public JSONObject getEvents(JSONObject params){
		String where = params.optString("where",null);
		String order = params.optString("order",null);
		String []args = null;
		try{
			JSONArray p = params.getJSONArray("params");
			args = new String[p.length()];
			for(int i=0;i<p.length();i++){
				args[i] = p.getString(i);
			}
		}catch(Exception e){
			args=null;
		}
		
		Cursor cur = mContext.getContentResolver().query(Uri.parse("content://com.android.calendar/events"),
						null, where, args, order );
		
		return OKResponse( cursorToJson(cur) );
	}
	
	public JSONObject getCalendarList(JSONObject params){
		Cursor cur = mContext.getContentResolver().query(Uri.parse("content://com.android.calendar/calendars"),
				null, null, null, null );
		return OKResponse( cursorToJson(cur) );
	}
	
	public JSONObject putEvent(JSONObject params){
		ContentValues values = new ContentValues();
		@SuppressWarnings("unchecked")
		Iterator<String> keys = params.keys();
		while( keys.hasNext() ){
			String key=keys.next();
			
			values.put( key, params.optString(key) );
		}
	    values.put( "eventTimezone", TimeZone.getDefault().getID() );

		Uri event = mContext.getContentResolver().insert(
				      Uri.parse("content://com.android.calendar/events"),
				      values
		);
		
		
		JSONObject result = new JSONObject();
		try {
			result.put("result",  event.getLastPathSegment() );
		} catch (JSONException e) {
		}

		return result;
	}

}
