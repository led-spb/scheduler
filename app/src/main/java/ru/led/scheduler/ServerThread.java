package ru.led.scheduler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import ru.led.scheduler.library.BaseLibrary;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class ServerThread extends Thread{
	private Context mContext;
	private boolean mRequestStop = false;
	private boolean mStarted = false;
	private boolean mLocal;
	
	private ServerSocket mServerSock;
	public static int mPort = 3993;
	private Handler mMainThreadHandler;
	
	private Map<String, BaseLibrary> mLibraries;
	
	public Context getContext(){ 
		return mContext;
	}
	public Handler getHandler(){
		return mMainThreadHandler;
	}
	
	ServerThread(Context ctx, Handler UIHandler, boolean local){
		super();

		mLocal = local;
		mContext = ctx;
		mRequestStop = false;
		mStarted = false;
		mMainThreadHandler = UIHandler;
		
		mLibraries = new HashMap<String,BaseLibrary>();
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private BaseLibrary loadLibrary(String lib){
		try {
			// Capitalize library name
			StringBuilder strBuilder = new StringBuilder(lib.toLowerCase());
			strBuilder.setCharAt(0, Character.toUpperCase(strBuilder.charAt(0)) );
			lib = strBuilder.toString();
			
			lib = String.format("ru.led.scheduler.library.%sLibrary", lib);
			Class libraryClass =  Class.forName(lib);
			
			BaseLibrary library = (BaseLibrary) libraryClass.getConstructor( this.getClass() ).newInstance(this);
			
			return library;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	synchronized public BaseLibrary findLibrary(String lib){
		lib = lib.toLowerCase();
		
		if( mLibraries.containsKey(lib) ){
			return mLibraries.get(lib);
		}
		
		BaseLibrary obj = loadLibrary(lib);
		if( obj!=null ){
			mLibraries.put(lib, obj);
		}
		
		return obj;
	}
	
	
	@Override
	public void run(){
		try{

			if( !mLocal ) {
				mServerSock = new ServerSocket(mPort, 50);
			}else {
				mServerSock = new ServerSocket(mPort, 50, InetAddress.getLocalHost());
			}

		}catch(IOException e){
			Log.e(getClass().getName(), "Create server socket", e);		
			return;
		}
		
		mStarted = true;
		while(!mRequestStop){
			try {
				Socket sock = mServerSock.accept();
				Log.d(getClass().getName(), "Connected client at port: "+sock.getPort() );

				new HandleConnection(sock).start();
			}catch (Exception e){
				if(!mRequestStop)
					Log.e(getClass().getName(), "Handle client connection", e);
			}
		}
		mRequestStop = false;
		mStarted = false;
	}
	
	public boolean isStarted(){ return mStarted; }

	public void shutdown(){
	    mRequestStop = true;
	    try{
	    	mServerSock.close();
	    }catch(IOException e) {
	    }
	}
	

	private class HandleConnection extends Thread{
		private Socket mSocket;
		
		public HandleConnection(Socket socket){
			mSocket = socket;
		}
		
		@Override
		public void run(){
			try {
				BufferedReader reader =  new BufferedReader(new InputStreamReader(mSocket.getInputStream()), 2048);
				PrintWriter writer = new PrintWriter(mSocket.getOutputStream(), true);
				
				String data;
				while( (data = reader.readLine())!=null ){
					JSONObject result = handleRequest(data);
					writer.println(result);
					writer.flush();
				}
			} catch (IOException e) {
				Log.e(getClass().getName(), "Handle client connection", e);
			}
			
			try {
				mSocket.close();
			} catch (IOException e){
			}			
		}
		
		
		private JSONObject handleRequest(String data){
			try{
				JSONObject request = new JSONObject(data);
				String library = request.getString("library");
				String command = request.getString("command");

				Log.d(getClass().getName(), "lib:"+library+" command:"+command);
				
				BaseLibrary handler = findLibrary(library);
			
				if( handler==null )
					return BaseLibrary.ErrorResponse("unknown library", "Library \""+library+"\" didn't exists" );

				return handler.handleCommand(request);
			}catch(Exception e){
				return BaseLibrary.ErrorResponse("handle request", e);
			}
		}
	}
}
