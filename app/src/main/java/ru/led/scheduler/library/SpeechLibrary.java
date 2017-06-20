package ru.led.scheduler.library;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import ru.led.scheduler.ServerThread;
import ru.led.scheduler.tools.LockObject;


public class SpeechLibrary extends BaseLibrary {
    private SpeechRecognizer mRecognizer;
    private TextToSpeech     mTextToSpeech;

    public SpeechLibrary(ServerThread thread) {
	super(thread);
    }

    
    @SuppressWarnings("unchecked")
    public JSONObject startRecognition(JSONObject params) throws Exception{
	final LockObject mWaitLock = new LockObject();
	
	// start listening
	startInMainThread(
	    	new Runnable(){
	    	    public void run(){
			mRecognizer = SpeechRecognizer.createSpeechRecognizer(mContext);
			mRecognizer.setRecognitionListener( new RecognitionListener(){
			    public void onBeginningOfSpeech() {
				synchronized(mWaitLock){
				   mWaitLock.put("status", "started"); 
				}
			    }

			    public void onError(int error) {
				synchronized(mWaitLock){
				    mWaitLock.put("status", "error");
				    mWaitLock.put("error", error);
				    mWaitLock.notify();
				}
			    }
			    public void onResults(Bundle results) {
				synchronized(mWaitLock){
				    mWaitLock.put("status", "done");
				    mWaitLock.put("results",  results.getStringArrayList( SpeechRecognizer.RESULTS_RECOGNITION ) );
				    mWaitLock.notify();
				}
			    }

			    public void onEndOfSpeech(){}
			    public void onEvent(int eventType, Bundle params){}
			    public void onPartialResults(Bundle partialResults){}
			    public void onReadyForSpeech(Bundle params){}
			    public void onRmsChanged(float rmsdB){}	
			    public void onBufferReceived(byte[] buffer) {}


			});
			
	    		Intent recIntent = new Intent( RecognizerIntent.ACTION_RECOGNIZE_SPEECH )
	    			.putExtra( RecognizerIntent.EXTRA_LANGUAGE, "ru-RU" )
	    			.putExtra( RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM );
	    		mRecognizer.startListening(recIntent);	    		
	    	    }
	    	}
	);
	
	
	try{
	  synchronized(mWaitLock){
	      mWaitLock.wait( 15*1000 );
	  }
	}catch(InterruptedException e){
	    mWaitLock.put("status","timeout");
	}

	// stop listening
	startInMainThread(
		new Runnable(){
		    public void run(){
			mRecognizer.stopListening();
		    }
		}
	);
	
	JSONObject result = new JSONObject();
	result.put("status", mWaitLock.get("status") );
	if( mWaitLock.containsKey("results") ){
	    result.put("results", new JSONArray( (ArrayList<String>)mWaitLock.get("results") ) );
	}
	
	return OKResponse(result);	
    }
    
    
    public JSONObject initSpeak(JSONObject params) throws Exception{
	final LockObject mLock = new LockObject();
	
	startInMainThread(
		new Runnable(){
		    public void run(){
			mTextToSpeech = new TextToSpeech( mContext , new TextToSpeech.OnInitListener() {
			    public void onInit(int status) {
				mTextToSpeech.setLanguage( new Locale("ru", "RU") );
				
				synchronized(mLock){
				    mLock.put("status", status);
				    mLock.notify();
				}
			    }
			});
		    }
		}
	);
	
	
	synchronized(mLock){
	    mLock.wait(5000);
	}
	
	return OKResponse();
    }

    
    public JSONObject speak(JSONObject params) throws Exception{
	String text = params.getString("text");
	if( params.has("filename") ){
	    mTextToSpeech.synthesizeToFile( text, null, params.getString("filename"));
	}else{
	    mTextToSpeech.speak( text, TextToSpeech.QUEUE_ADD, null );
	}
	return OKResponse();
    }
}
