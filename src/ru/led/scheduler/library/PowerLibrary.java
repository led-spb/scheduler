package ru.led.scheduler.library;

import org.json.JSONObject;

import android.content.Context;
import android.os.PowerManager;
import ru.led.scheduler.ServerThread;

public class PowerLibrary extends BaseLibrary {
    private PowerManager mPowerMan; 
    
    public PowerLibrary(ServerThread thread) {
	super(thread);
	mPowerMan = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
    }

    public JSONObject isScreenOn(JSONObject params) throws Exception{
	return OKResponse( mPowerMan.isScreenOn() );
    }
    
    public JSONObject reboot(JSONObject params) throws Exception{
	mPowerMan.reboot( params.getString("reason") );
	return OKResponse();
    }
    
}
