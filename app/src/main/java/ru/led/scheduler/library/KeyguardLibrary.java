package ru.led.scheduler.library;

import org.json.JSONObject;

import android.app.KeyguardManager;
import android.content.Context;
import ru.led.scheduler.ServerThread;

public class KeyguardLibrary extends BaseLibrary {
    private KeyguardManager mKeyguardMan;
    
    public KeyguardLibrary(ServerThread thread) {
	super(thread);
	mKeyguardMan = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
    }

    public JSONObject getStatus(JSONObject params) throws Exception{
	JSONObject result = new JSONObject();
	
	result.put("locked", mKeyguardMan.isKeyguardLocked() );
	result.put("secure", mKeyguardMan.isKeyguardSecure() );
	result.put("restricted", mKeyguardMan.inKeyguardRestrictedInputMode() );
	
	return OKResponse(result);
    }
}
