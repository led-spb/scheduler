package ru.led.scheduler.library;

import java.io.FileInputStream;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;
import ru.led.scheduler.ServerThread;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.provider.Settings.System;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.format.Formatter;

import android.app.UiModeManager;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;


public class SettingsLibrary extends BaseLibrary {
	
	public SettingsLibrary(ServerThread thread) {
		super(thread);
	}

	public JSONObject setAirplaneMode(JSONObject params) throws Exception{
		boolean state = params.getBoolean("state");
		
		System.putInt(mContext.getContentResolver(), System.AIRPLANE_MODE_ON, state?1:0 );
		mContext.sendBroadcast( 
			new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).putExtra("state", state)
		);
		return OKResponse();
	}
	
	public JSONObject setWallpaper(JSONObject params) throws Exception{
		String filename = params.getString("file");
		WallpaperManager wallMan = WallpaperManager.getInstance(mContext);
		
		
		wallMan.setStream( new FileInputStream(filename) );
		return OKResponse();
	}
	
	public JSONObject getChargingState(JSONObject params) throws Exception{
		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED );
		Intent intent = mContext.registerReceiver(null, filter);
		
		JSONObject result = new JSONObject();
		result.put("plugged", intent.getIntExtra( BatteryManager.EXTRA_PLUGGED, -1) );
		result.put("level",   intent.getIntExtra( BatteryManager.EXTRA_LEVEL, -1) );
		result.put("scale",   intent.getIntExtra( BatteryManager.EXTRA_SCALE, -1) );
		switch( intent.getIntExtra( BatteryManager.EXTRA_PLUGGED, -1) ){
			case BatteryManager.BATTERY_PLUGGED_AC:
				result.put("state", "ac");
				break;
			case BatteryManager.BATTERY_PLUGGED_USB:
				result.put("state", "usb");
				break;
			default:
				result.put("state", "unplugged");
		}
		
		return result;
	}
	
	public JSONObject setWifiState(JSONObject params) throws Exception{
	  WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
	  wifiManager.setWifiEnabled( params.getBoolean("state") );
	  return OKResponse();
	}
	
	public JSONObject setWifiSleepPolicy(JSONObject params) throws Exception{
	  int mode = 0;
	  String str = params.getString("mode");
	  
	  if( str.equals("never") )        mode = System.WIFI_SLEEP_POLICY_NEVER;
	  if( str.equals("never_plugg") )  mode = System.WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED;
	  if( str.equals("default") )	   mode = System.WIFI_SLEEP_POLICY_DEFAULT;
	  
	  System.putInt( mContext.getContentResolver(), System.WIFI_SLEEP_POLICY, mode );
	  return OKResponse();
	}
		
	
	public JSONObject getAudioSettings(JSONObject params) throws Exception{
	  JSONObject result = new JSONObject();
	  String stream = params.getString("stream");
	  
	  int streamType = 0;
	  
	  if( stream.equals("dtfm") )   streamType= AudioManager.STREAM_DTMF;
	  if( stream.equals("music") )  streamType= AudioManager.STREAM_MUSIC;
	  if( stream.equals("notify") ) streamType= AudioManager.STREAM_NOTIFICATION;
	  if( stream.equals("ring") )   streamType= AudioManager.STREAM_RING;
	  if( stream.equals("system") ) streamType= AudioManager.STREAM_SYSTEM;
	  if( stream.equals("voice") )  streamType= AudioManager.STREAM_VOICE_CALL;
	  
	  AudioManager audioManager = (AudioManager) mContext.getSystemService( Context.AUDIO_SERVICE );
	  
	  
	  result.put("volume", audioManager.getStreamVolume(streamType) );
	  result.put("max", audioManager.getStreamMaxVolume(streamType) );
	  
	  return result;
	}

	public JSONObject setAudioSettings(JSONObject params) throws Exception{
	  String stream = params.getString("stream");
	  int volume = params.getInt("volume");
	  int flags = 1;
	  if( params.has("flags") )
		  flags = params.getInt("flags");
	  
	  int streamType = 0;
	  
	  if( stream.equals("dtfm") )   streamType= AudioManager.STREAM_DTMF;
	  if( stream.equals("music") )  streamType= AudioManager.STREAM_MUSIC;
	  if( stream.equals("notify") ) streamType= AudioManager.STREAM_NOTIFICATION;
	  if( stream.equals("ring") )   streamType= AudioManager.STREAM_RING;
	  if( stream.equals("system") ) streamType= AudioManager.STREAM_SYSTEM;
	  if( stream.equals("voice") )  streamType= AudioManager.STREAM_VOICE_CALL;
	  
	  AudioManager audioManager = (AudioManager) mContext.getSystemService( Context.AUDIO_SERVICE );
	  
	  audioManager.setStreamVolume(streamType, volume, flags);
	  
	  return OKResponse();
	}
	
	public JSONObject setRingerMode(JSONObject params) throws Exception{
	  String mode = params.getString("mode");
	  int modeType = 0;

	  if( mode.equals("normal") )  modeType = AudioManager.RINGER_MODE_NORMAL;
	  if( mode.equals("silent") )  modeType = AudioManager.RINGER_MODE_SILENT;
	  if( mode.equals("vibrate") ) modeType = AudioManager.RINGER_MODE_VIBRATE;
	  
	  AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
	  audioManager.setRingerMode(modeType);
	  
	  return OKResponse();
	}

	public JSONObject setAccelerometerRotation(JSONObject params) throws Exception{
	  boolean mode = params.getBoolean("state");
	  System.putInt( mContext.getContentResolver(), System.ACCELEROMETER_ROTATION, mode?1:0 );
	  return OKResponse();
	}


	public JSONObject getCellLocation(JSONObject params) throws Exception{
		TelephonyManager telMan = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
		
		GsmCellLocation cell = (GsmCellLocation) telMan.getCellLocation();
		JSONArray arr = new JSONArray();
		
		//telMan.getNetworkOperator()
		Configuration conf = mContext.getResources().getConfiguration();
	
		if(cell!=null){
			JSONObject result = new JSONObject();

			result.put("cid", cell.getCid() );
			result.put("lac", cell.getLac() );
			result.put("psc", cell.getPsc() );
			result.put("mcc", conf.mcc );
			result.put("mnc", conf.mnc );
			
			arr.put(result);
		}
		
		return OKResponse(arr);
	}
	
	public JSONObject setDisplayTimeout(JSONObject params) throws Exception{
		int timeout = params.getInt("timeout");
		System.putInt( mContext.getContentResolver(), System.SCREEN_OFF_TIMEOUT, timeout );
		return OKResponse();
	}
	
	public JSONObject setGPSStatus(JSONObject params) throws Exception{
		return ErrorResponse("not supported", "Settings GPS state not supported since Android 2.3.7");
	}

	
	public JSONObject setBrightnessLevel(JSONObject params) throws Exception{
	  int level = params.getInt("level");
	  System.putInt( mContext.getContentResolver(), System.SCREEN_BRIGHTNESS, level );
	  return OKResponse();
	}
	
	public JSONObject setBrightnessMode(JSONObject params) throws Exception{
	  System.putInt( mContext.getContentResolver(), System.SCREEN_BRIGHTNESS_MODE, params.getInt("mode") );
	  return OKResponse();
	}

	public JSONObject getBrightnessLevel(JSONObject params) throws Exception{
	  return OKResponse( System.getInt( mContext.getContentResolver(), System.SCREEN_BRIGHTNESS ) );
	}
	
	public JSONObject setCarMode(JSONObject params) throws Exception{
	  UiModeManager uiManager = (UiModeManager) mContext.getSystemService( Context.UI_MODE_SERVICE );
	  
	  if( params.getBoolean("state") )  uiManager.enableCarMode(0);
	  else  uiManager.disableCarMode(0);
	  return OKResponse();
	}
	
	public JSONObject getPreferences(JSONObject params) throws Exception{
	  /*UiModeManager uiManager = (UiModeManager) mContext.getSystemService( Context.UI_MODE_SERVICE );
		  
		  if( params.getBoolean("state") )  uiManager.enableCarMode(0);
		  else  uiManager.disableCarMode(0);*/
		SharedPreferences pref = mContext.getSharedPreferences( params.getString("name"), Context.MODE_PRIVATE );
		JSONObject result = new JSONObject(pref.getAll());
		
		return OKResponse(result);
	}
	
	
	public JSONObject setPreferences(JSONObject params) throws Exception{
		SharedPreferences pref = mContext.getSharedPreferences( params.getString("name"), Context.MODE_PRIVATE );
		
		JSONObject values = params.getJSONObject("values");
		
		Editor editor = pref.edit();
		for(Iterator<String> it = values.keys(); it.hasNext(); ){
			String key = it.next();
			editor.putString(key, values.getString(key) );
		}
		editor.commit();
		
		return OKResponse();
	}
			
	
	public JSONObject getWifiInfo(JSONObject params) throws Exception{
		WifiManager wifiMan = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wifiMan.getConnectionInfo();
		
		JSONObject result = new JSONObject();
		result.put("ssid", info.getSSID() );
		result.put("bssid", info.getBSSID() );
		result.put("hidden", info.getHiddenSSID() );
		result.put("speed", info.getLinkSpeed()+WifiInfo.LINK_SPEED_UNITS );
		result.put("ipaddress", Formatter.formatIpAddress(info.getIpAddress()) );
		result.put("rssi", info.getRssi() );
		result.put("mac", info.getMacAddress() );

		return OKResponse( result );
	}
}
