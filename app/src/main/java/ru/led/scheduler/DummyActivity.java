package ru.led.scheduler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class DummyActivity extends Activity {
	public static String ACTION_SHORTCUT_RUN  = "ru.led.scheduler.SHORTCUT";
	public static String ACTION_FAKE_ACTIVITY = "ru.led.scheduler.FAKE";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    Intent intent = getIntent();
	    /*
		if( intent!=null && intent.hasExtra("theme") ){
			int theme = intent.getIntExtra("theme", android.R.style.Theme_NoDisplay);
			Log.d("DummyActivity","Set theme: "+theme);
			setTheme( theme );
		}*/
		
	    super.onCreate(savedInstanceState);
	    if( intent!=null && intent.getAction().equals(ACTION_SHORTCUT_RUN) && intent.hasExtra("script") ){
	    	String script = intent.getStringExtra("script");

	    	Map<String, String> extra = null;
   	
	    	if( true /*intent.hasExtra("params")*/ ){
		    	// Bundle params = intent.getBundleExtra("params");
	    		Bundle params=intent.getExtras();
		    	extra = new HashMap<String, String>();
		    	
		    	for(Iterator<String> it=params.keySet().iterator(); it.hasNext();){
		    		String key = it.next(); 
		    		String value = params.getString(key);

		    		if( key.startsWith("param_") )
		    		    key = key.substring( "param_".length() );
		    		
		    		extra.put(key, value);
		    	}
	    	}
	    	
	    	if( script!=null ){
	    		ExecService.executeTask(this, script, extra);
	    	}
	    }
	    
	    if(intent!=null && intent.getAction().equals(ACTION_FAKE_ACTIVITY) ){
	    	String runnableToken = intent.getExtras().getString("runnable");
	    	Runnable r = (Runnable) App.Params.get(runnableToken);
	    	
	    	App.Params.put("dummy_activity", this);
	    	App.Params.remove(runnableToken);
	    	r.run();
	    	
	    	return;
	    }
	    
	    finish();
	}
	
	public static Intent getShortcutIntent(Context ctx, String script, Map<String,String> params){
		Intent intent = new Intent(ctx.getApplicationContext(), DummyActivity.class)
				.putExtra("script", script)
				.setAction(ACTION_SHORTCUT_RUN);
		if( params!=null ){
			//Bundle extra = new Bundle();
			for(Iterator<String> it=params.keySet().iterator(); it.hasNext(); ){
				String key=it.next();
				intent.putExtra("param_"+key, params.get(key));
				//extra.putString(key, params.get(key));
			}
			//intent.putExtra("params", extra);
		}
		return intent;
	}

}
