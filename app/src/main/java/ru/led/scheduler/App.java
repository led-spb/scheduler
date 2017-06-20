package ru.led.scheduler;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

public class App extends Application {
	@SuppressLint("SdCardPath")
	public static String baseDir = "/sdcard/scripts/";
	public static String configFile = baseDir+"scheduler.conf";
	
	private ServerThread mServer;
	private Handler      mHandler;
	private Schedule     mSchedule = new Schedule(configFile);
	
	@Override
	public void onCreate(){
		super.onCreate();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		mHandler = new Handler();
		mServer  = new ServerThread(this, mHandler, prefs.getBoolean("local",true) );

		startServerThread();

        if( prefs.getBoolean("autostart",false) ){
            this.startService( ExecService.getRestartIntent(this) );
        }
	}
	
	public ServerThread getServerThread(){
	    return mServer;
	}
	
	public static Map<String,Object> Params = new HashMap<String, Object>();

	public void refreshConfig(){
		mSchedule = new Schedule(configFile);
	}
	
	public Schedule getSchedule(){
		return mSchedule;
	}
	
	public void startServerThread(){
		if( mServer.isStarted() ) return;
		try{
			mServer.start();
		}catch(Exception e){
			Log.e( getClass().getName(), "startServer", e);
		}
	}

    private boolean mServiceStarted=false;
    public boolean getServiceStarted(){
        return mServiceStarted;
    }
    public void setServiceStarted(boolean state){
        mServiceStarted=state;
    }
}
