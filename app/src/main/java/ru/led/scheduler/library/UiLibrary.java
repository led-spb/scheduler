package ru.led.scheduler.library;

import org.json.JSONObject;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import ru.led.scheduler.App;
import ru.led.scheduler.ServerThread;
import ru.led.scheduler.DummyActivity;
import ru.led.scheduler.WebActivity;
import ru.led.scheduler.tools.LockObject;

public class UiLibrary extends BaseLibrary {

	public UiLibrary(ServerThread thread) {
		super(thread);
	}
	
	
	public JSONObject prompt(JSONObject params) throws Exception{
		final String title = params.optString("title","prompt");
		final String message = params.getString("message");
		final String value = params.optString("value","");
		
		final LockObject mLock = new LockObject();
		
		final Runnable r = new Runnable(){
			public void run() {
				final DummyActivity mActivity = (DummyActivity) App.Params.get("dummy_activity");
				final EditText edit = new EditText(mContext);
				edit.setText(value);
				
				OnClickListener onClick = new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						synchronized(mLock){
							dialog.dismiss();
							mActivity.finish();
							
							mLock.put("value", edit.getText().toString() );
							mLock.notify();
						}
					}
				};
				
				AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
				   .setTitle(title).setMessage(message).setView(edit)
				   .setPositiveButton(android.R.string.ok, onClick)
				   .setNegativeButton(android.R.string.cancel, onClick)
				   .setCancelable(false)
				;
				
				builder.show();
			}
		};
		
		startInDummyActivity(DummyActivity.class, r);
		
		synchronized (mLock) {
			mLock.wait();
		}
		
		return OKResponse( (String)mLock.get("value") );
	}

	
	public JSONObject web(final JSONObject params) throws Exception{
		/*
		startInDummyActivity(DummyActivity.class, new Runnable(){
			public void run() {
			}} 
		);*/
		
		Intent intent = new Intent(mContext, WebActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		if( params.has("url") ){
			intent.setData(Uri.parse(params.getString("url")));
		}else{
			intent.putExtra("content", params.optString("content","") );
		}
		
		mContext.startActivity(intent);
		
		return OKResponse();
	}
	
}
