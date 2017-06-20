package ru.led.scheduler.library;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import ru.led.scheduler.App;
import ru.led.scheduler.DummyActivity;
import ru.led.scheduler.ServerThread;

public abstract class BaseLibrary {
		//public abstract String getLibraryName();
		protected ServerThread mThread;
		protected Context mContext;
		protected Handler mHandler;
		
		public BaseLibrary(ServerThread thread){
			mThread = thread;
			mContext = mThread.getContext();
			mHandler = mThread.getHandler();
		}
		
		public static JSONObject OKResponse(String msg){
			JSONObject result = new JSONObject();
			try {
				result.put("result", msg);
			} catch (JSONException e) {
			}
			return result;			
		}
		
		protected void startInDummyAactivity(Runnable r){
			startInDummyActivity(DummyActivity.class,r);
		}
		protected void startInDummyActivity(Class activityClass, Runnable r){
			String runnableToken = String.valueOf( r.hashCode() );
			App.Params.put(runnableToken, r);
			
			Intent intent = new Intent(mContext, activityClass )
					.setAction( DummyActivity.ACTION_FAKE_ACTIVITY )
					.putExtra("runnable", runnableToken)
					.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK+Intent.FLAG_ACTIVITY_CLEAR_TASK+Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
				
			mContext.startActivity(intent);	
		}
		
		protected void startInMainThread( Runnable r){
		    mHandler.post(r);
		}
		
		public static JSONObject OKResponse(int res){
			JSONObject result = new JSONObject();
			try {
				result.put("result", res);
			} catch (JSONException e) {}
			return result;			
		}
		
		public static JSONObject OKResponse(boolean res){
			JSONObject result = new JSONObject();
			try {
				result.put("result", res);
			} catch (JSONException e) {}
			return result;			
		}		
		public static JSONObject OKResponse(JSONArray array){
			JSONObject result = new JSONObject();
			try {
				result.put("result", array);
			} catch (JSONException e) {}
			return result;			
		}
		
		public static JSONObject OKResponse(JSONObject obj){
			JSONObject result = new JSONObject();
			try {
				result.put("result", obj);
			} catch (JSONException e) {}
			return result;
		}		
		
		public static JSONObject OKResponse(){
			return OKResponse("success");
		}
		
		
		public static JSONObject ErrorResponse(String msg, Throwable err){
			JSONObject result = new JSONObject();
			try {
				result.put("error", true);
				result.put("msg", msg);
				result.put("description", err.toString() );

                StringWriter trace = new StringWriter();
                err.printStackTrace(new PrintWriter(trace) );
                result.put("trace", trace.toString() );

			} catch (JSONException e) {}
			return result;
		}		
		public static JSONObject ErrorResponse(String msg, String descr){
			return ErrorResponse(msg, new Throwable(descr) );
		}
		
		public JSONObject handleCommand(JSONObject request) throws Exception{
			String command = request.getString("command");
			JSONObject result = new JSONObject();
			
			try{
				Method m = this.getClass().getMethod ( command, JSONObject.class);
				JSONObject params = request.getJSONObject("params");
				
				result = (JSONObject) m.invoke(this, params);
			}catch(InvocationTargetException e){
				return ErrorResponse( "Exception", e.getTargetException() );
			}catch(NoSuchMethodException e){
				return ErrorResponse( "No such command", 
									  "Command \""+command+"\" didn't exists at this library"
				);
			}

			return result;
		}
		
		protected JSONArray cursorToJson(Cursor cur){
			JSONArray result = new JSONArray();
			String[] columns = cur.getColumnNames();
			
			while( cur.moveToNext() ){
				JSONObject row = new JSONObject();
				
				for(int i=0; i<columns.length; i++){
					try {
						String value = cur.getString(i);
						
						try{
							row.put( columns[i], Integer.parseInt(value) );
						}catch(Exception e){
							row.put( columns[i], value );
						}
					} catch(Exception e){
						try {
							row.put( columns[i], null);
						} catch (JSONException e1) {
						}
					}
				}
				result.put(row);
			}
			return result;
		}}
