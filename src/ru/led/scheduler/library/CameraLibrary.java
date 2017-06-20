package ru.led.scheduler.library;

import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.json.JSONException;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import ru.led.scheduler.App;
import ru.led.scheduler.CameraActivity;
import ru.led.scheduler.DummyActivity;
import ru.led.scheduler.R;
import ru.led.scheduler.ServerThread;
import ru.led.scheduler.tools.LockObject;

public class CameraLibrary extends BaseLibrary {
	private Camera mCamera;
	private Activity previewActivity;
	
	
	public CameraLibrary(ServerThread thread) {
		super(thread);
		Log.i(this.getClass().getName(), "construcor");
	}
	
	public JSONObject count(JSONObject params) throws Exception{
		return OKResponse( Camera.getNumberOfCameras() );
	}
	
	public JSONObject open(JSONObject params) throws Exception{
		if(mCamera==null){
		  if( params!=null && params.has("id") )
			mCamera = Camera.open(params.getInt("id"));
		  else
			mCamera = Camera.open();
		}
		return OKResponse();
	}
	
	public JSONObject startPreview(JSONObject params) throws Exception{
		checkCamOpen();
		if( previewActivity!=null ) throw new RuntimeException("Preview is already active...");
		
		final LockObject mLock = new LockObject();
		
		// Make dummy activity for preview
		Runnable r = new Runnable(){
			public void run() {
				DummyActivity fakeActivity=(DummyActivity) App.Params.get("dummy_activity");
				mLock.put("activity", fakeActivity);
				
				try{
					Log.d(this.getClass().getName(),"Runned camera fake activity");
					fakeActivity.setContentView( R.layout.camera_activity );
				    
					
					final SurfaceView preview = (SurfaceView) fakeActivity.findViewById( R.id.cameraSurfaceView);

					preview.getHolder().addCallback(
							new Callback(){
								public void surfaceChanged(SurfaceHolder surface,	int arg1, int arg2, int arg3) {
								}

								public void surfaceCreated(SurfaceHolder surface) {
									Log.d(this.getClass().getName(), "SurfaceHolder created");
									synchronized(mLock){
									  mLock.put("holder", preview.getHolder() );
									  mLock.notify();
									}
								}

								public void surfaceDestroyed(SurfaceHolder surface) {
								}
							}
					); 
				}catch(Exception e){
					Log.e(this.getClass().getName(), "takeShot", e);
					
					synchronized(mLock){
						mLock.notify();
					}
				}
			}
		};
		

		startInDummyActivity(CameraActivity.class, r );
		
		synchronized (mLock) {
			mLock.wait();
		}
		previewActivity = (Activity)mLock.get("activity");
		
		
		SurfaceHolder mHolder = (SurfaceHolder) mLock.get("holder");

		mCamera.setPreviewDisplay(mHolder);
		mCamera.startPreview();		
		
		return OKResponse();
	}
	
	public JSONObject stopPreview(JSONObject params) throws Exception{
		checkCamOpen();
		if( previewActivity==null ) throw new RuntimeException("Preview is not active...");
		previewActivity.finish();
		previewActivity=null;
		
		return OKResponse();
	}
	
	public JSONObject takeShot(final JSONObject params) throws Exception{
		checkCamOpen();
		if( previewActivity==null && params.optBoolean("force",false)==false ){
			throw new RuntimeException("Preview window is not started...");
		}
		final JSONObject result = new JSONObject();
	
		final CountDownLatch latch = new CountDownLatch(1);
	    mCamera.takePicture(null, null,
            new PictureCallback(){
                @SuppressLint("SdCardPath")
                public void onPictureTaken(byte[] data, Camera camera) {
                    try {
                          if( params.has("filename") ){
                              FileOutputStream output = new FileOutputStream( params.optString("filename","/sdcard/shot.jpg") );
                              output.write(data);
                              output.close();

                              result.put("result", "success");
                          }
                          else{
                              result.put("result", Base64.encodeToString(data, Base64.NO_WRAP) );
                          }
                    } catch (Exception e) {
                          Log.e(this.getClass().getName(), "Failed to save picture.", e);
                          return ;
                    } finally {
                          latch.countDown();
                    }
                  }
                }
        );
	    latch.await();
	    
		return result;
	}
	
	public JSONObject release(JSONObject params) throws Exception{
		if(previewActivity!=null) stopPreview(null);
		if(mCamera!=null){
			mCamera.release();
			mCamera = null;
		}
		return OKResponse();
	}

	public JSONObject getParams(JSONObject params) throws Exception{
		checkCamOpen();
		
		Parameters camParams = mCamera.getParameters();
		JSONObject result=new JSONObject();
		
		List<String> data=  Arrays.asList( camParams.flatten().split(";") );
		Iterator<String> it = data.iterator();
		while(it.hasNext()){
			String p = it.next();
			String[] d = p.split("=");
			
			try{
				result.put(d[0], Integer.parseInt(d[1]) );
			}catch(NumberFormatException e){
				result.put(d[0], d[1] );
			}
		}
		
		return OKResponse(result);
	}
	
	public JSONObject setParams(JSONObject params) throws Exception{
		checkCamOpen();
		
		Parameters camParams = mCamera.getParameters();

		@SuppressWarnings("unchecked")
		Iterator<String> keys = params.keys();
		while(keys.hasNext()){
			String key = keys.next();
			try{
				camParams.set( key, params.getInt(key) );
			}catch(JSONException e){
				camParams.set(key, params.getString(key) );
			}
		}
		mCamera.setParameters(camParams);
	
		return OKResponse();
	}
	
	public JSONObject setParam(JSONObject params) throws Exception{
		checkCamOpen();
		
		Parameters camParams = mCamera.getParameters();

		String key = params.getString("param");
		try{
			camParams.set( key, params.getInt("value") );
		}catch(JSONException e){
			camParams.set(key, params.getString("value") );
		}
	
		mCamera.setParameters(camParams);
	
		return OKResponse();
	}	
	
	
	private void checkCamOpen() throws RuntimeException{
		if(mCamera==null) 
			throw new RuntimeException("Camera is not opened...");
	}
}
