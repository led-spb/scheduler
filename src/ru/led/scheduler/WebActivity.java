package ru.led.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;

@SuppressLint("SetJavaScriptEnabled")
@SuppressWarnings("deprecation")
public class WebActivity extends Activity {
	private Socket mSocket;
	
	public String runQuery(String query){
		try {
			if(mSocket==null)
				mSocket = new Socket(InetAddress.getLocalHost(), ServerThread.mPort);
		
			BufferedReader reader =  new BufferedReader(new InputStreamReader(mSocket.getInputStream()), 2048);
			PrintWriter writer = new PrintWriter(mSocket.getOutputStream(), true);

			writer.println(query);
			String result = reader.readLine();
			return result;
		}catch(Exception e){
			Log.e("WebActivity", "error", e);
		}
		return "";
	}
	
	public void setForceScreenOn(){
	    getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
	}
	public void clearForceScreenOn(){
	    getWindow().clearFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
	}
	
	@Override
	protected void onStop(){
	    //WebView wv = (WebView) findViewById(R.id.webView);
	    //wv.loadUrl("javascript:onwebclose()");
	    
	    super.onStop();
	    if(mSocket!=null){
    	    	try {
    	    	    mSocket.close();
    	    	} catch (IOException e) {}
    	    	mSocket=null;
	    }
	}
	
	@Override
	protected void onPause(){
	    super.onPause();
	    WebView wv = (WebView) findViewById(R.id.webView);
	    wv.getSettings().setJavaScriptEnabled(false);
	}

	@Override
	protected void onResume(){
	    super.onResume();
	    WebView wv = (WebView) findViewById(R.id.webView);
	    wv.getSettings().setJavaScriptEnabled(true);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
				
		//this.setTheme( android.R.style. );
		setContentView(R.layout.web_activity);
		
		WebView wv = (WebView) findViewById(R.id.webView);
		wv.setWebChromeClient(new WebChromeClient(){
		    public boolean onConsoleMessage (ConsoleMessage cm){
			Log.d("WebActivity", 
				   cm.message() + " -- From line "
	                         + cm.lineNumber() + " of "
	                         + cm.sourceId() 
	                );
			return true;
		    }
		});
		
		final String injectJS = "Android=function(lib,command,params){return JSON.parse(android.runQuery(JSON.stringify({library:lib,command:command,params:params||{}})));};";
		
		wv.setWebViewClient( new WebViewClient(){
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url){
				return false;
			}
			
			@Override 
			public void onPageFinished(WebView view, String url){
			    	view.loadUrl("javascript:"+injectJS);
			}
		}
		);
		WebSettings webSettings = wv.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setPluginsEnabled(true);
		webSettings.setLoadWithOverviewMode(true);
		// webSettings.setUseWideViewPort(true); 
		webSettings.setUseWideViewPort(false);
		webSettings.setAllowFileAccess(true);
		/*
		webSettings.setAllowFileAccessFromFileURLs(true);
		webSettings.setAllowUniversalAccessFromFileURLs(true);*/
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		
	    
		Intent intent = getIntent();
		wv.addJavascriptInterface(this, "android" );
		//wv.setWebContentsDebuggingEnabled(true);
		
		//wv.setBackgroundColor(Color.BLACK);
		
		if( intent.getData()!=null ){
			Log.d("url",intent.getData().toString());
			wv.loadUrl( intent.getData().toString() );
		}else{
			if( intent.hasExtra("content") ){
				wv.loadData( intent.getStringExtra("content"), "text/html", "utf-8");
			}
		}
	}
}
