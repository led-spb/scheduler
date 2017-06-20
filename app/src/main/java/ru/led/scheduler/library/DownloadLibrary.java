package ru.led.scheduler.library;

import java.util.Iterator;

import org.json.JSONObject;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import ru.led.scheduler.ServerThread;

public class DownloadLibrary extends BaseLibrary {

	public DownloadLibrary(ServerThread thread) {
		super(thread);
	}

	
	public JSONObject downloadRequest(JSONObject params) throws Exception{
		DownloadManager downMan = (DownloadManager) mContext.getSystemService( Context.DOWNLOAD_SERVICE );
		
		DownloadManager.Request request = new DownloadManager.Request( Uri.parse(params.getString("url")) );
		
		if( params.has("filename") ){
			//File savePath = new File(params.getString("filename"));
			//request.setDestinationUri( Uri.fromFile(savePath) );
			String directory = params.optString("directory", Environment.DIRECTORY_DOWNLOADS);
			
			request.setDestinationInExternalPublicDir( directory, params.getString("filename"));
		}
		if( params.has("title") )
			request.setTitle( params.getString("title"));
		if( params.has("descr") )
			request.setDescription( params.getString("descr"));
		
		request.setNotificationVisibility( params.optInt("notify", 0) );
		
		if( params.has("headers") ){
			JSONObject headers = params.getJSONObject("headers");
			Iterator<String> keys = headers.keys();
			while(keys.hasNext()){
				String key = keys.next(), value = headers.getString(key);
				request.addRequestHeader( key, value);
			}
		}
		request.setAllowedOverRoaming(false);
		
		if( params.optBoolean("scan", true) ){
			request.allowScanningByMediaScanner();
		}
		
		downMan.enqueue(request);
		return OKResponse();
	}

}
