package ru.led.scheduler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.led.scheduler.tools.LockObject;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;


public class ExecService extends Service {
	
	public final static String START_ACTION        = "ru.led.scheduler.START";
	public final static String EXEC_ACTION         = "ru.led.scheduler.EXECUTE";
	public final static String EXEC_WRAPPER_ACTION = "ru.led.scheduler.EXECUTE_WRAPPER";
	public final static String RESTART_ACTION      = "ru.led.scheduler.RESTART";
	public final static String TICK_ACTION         = "ru.led.scheduler.TIME_TICK";
	public final static String SET_NOTIFY_ACTION   = "ru.led.scheduler.SET_NOTIFY_INFO";
	
	private boolean mStarted = false;
	private int NOTIFY_ID = 0x1034;
	private Notification mNotify;
	private long lastNetworkState;
	
	private File scriptDir = new File(App.baseDir);

	private TelephonyManager mTelMan;
	
	@SuppressLint("SimpleDateFormat")
	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if( intent==null || intent.getAction()==null) return;
			String action = intent.getAction();
			Log.i("ExecService.receiver","action="+action );
			
			if( action.equals(SET_NOTIFY_ACTION) ){
				String message = intent.hasExtra("message")?intent.getStringExtra("message"):"";
				ExecService.this.setServiceNotifyInfo(message);
				return;
			}
			
			if( action.equals(TICK_ACTION) ){
				ExecService.this.processTimesTasks();
				return;
			}
			
			if( action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ){
				NetworkInfo ni = (NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
				if( ni==null ) return; 
				
				if( ni.getDetailedState() == NetworkInfo.DetailedState.CONNECTED )
				{ 
					if(	System.currentTimeMillis()-lastNetworkState<3000 ) return;
					lastNetworkState = System.currentTimeMillis();
				}
				action = ni.getTypeName().toUpperCase()+"_"+ni.getDetailedState().name();
				
				if( ni.getType() == ConnectivityManager.TYPE_WIFI && ni.getDetailedState() == DetailedState.CONNECTED){
					WifiManager wifiMan = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
					WifiInfo info = wifiMan.getConnectionInfo();				
					
					executeAction("WIFI_CONNECTED_"+info.getSSID().replaceAll("\\s+", "_").toUpperCase() );
				}
			}
			executeAction(action);
		}
		
		private void executeAction(String action){
			Log.i("ExecService.receiver","proc_action="+action );
			
			Schedule mSchedule = ((App)getApplication()).getSchedule();

            for(Iterator<Task> it=mSchedule.getTasks().iterator(); it.hasNext(); ){
                Task task = it.next();
                if( !task.getEnabled() ) continue;

                for( Iterator<TaskSchedule> its=task.getSchedulesIterator(); its.hasNext(); ){
                    TaskSchedule ts = its.next();
                    if( ts.getType()==TaskSchedule.SCHEDULE_TYPE_EVENT && ts.getSchedule().equals(action) ){
                        Log.i("execute","Execute script "+task.getName() +" on action "+action );
                        executeTask(task, null);
                        break;
                    }
                }
            }
		}
		
		
	};
	
	
	private PhoneStateListener mPhoneListener = new PhoneStateListener(){
		private CellLocation oldLocation= null;
		@Override
		public void onCellLocationChanged (CellLocation location){
			///*
			if( (oldLocation==null && location!=null) || (oldLocation!=null && location==null) ||
				(oldLocation!=null && location!=null && !oldLocation.equals(location))
			){//*/{
				ExecService.this.sendBroadcast( new Intent("CELL_CHANGED") );
				oldLocation = location;
			}
		}
		@Override
		public void onCallStateChanged(int state, String incomingNumber){
		}
	};
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	

	private void registerService(){
		if( mStarted ) return;
		
		mTelMan = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		mTelMan.listen(mPhoneListener, PhoneStateListener.LISTEN_CELL_LOCATION+PhoneStateListener.LISTEN_CALL_STATE);
		
		// Register receiver for time tick events and others
		IntentFilter filter = new IntentFilter();
		filter.addAction(TICK_ACTION);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		filter.addAction(SET_NOTIFY_ACTION);
		
		registerReceiver(mReceiver, filter );
		
		setServiceNotifyInfo("");
		startForeground(NOTIFY_ID, mNotify);

		mStarted = true;
	}
	
	private void unregisterService(){
		if(!mStarted) return;
		
		unregisterReceiver(mReceiver);
		mTelMan.listen(mPhoneListener, PhoneStateListener.LISTEN_NONE);
		
		stopForeground(true);
	}

	
	public void setServiceNotifyInfo(String message){
		//if( mNotify==null ){
			mNotify = new Notification( R.drawable.ic_stat_notify, 
					getString(R.string.app_name), 
					System.currentTimeMillis() 
			);
		//}
		mNotify.setLatestEventInfo(this, getString(R.string.app_name), message, 
				PendingIntent.getActivity(this, 0, new Intent().setClass(this, MainActivity.class), 0 )
		);
			
		NotificationManager notifyMan = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notifyMan.notify(NOTIFY_ID, mNotify);
	}
	
	@Override
	public void onDestroy(){
        ((App)getApplication()).setServiceStarted(false);
		unregisterService();
		super.onDestroy();
	}
	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if( intent!=null && intent.getAction()!=null )
		{
			String action = intent.getAction();
			
			// Запуск планировщика
			if( action.equals(START_ACTION) || action.equals(RESTART_ACTION) ){
				registerService();

				processActionTasks();
				processTimesTasks();
                ((App)getApplication()).setServiceStarted(true);

				return START_STICKY;
			}

			// Обработчики команды планировщика
			if( action.equals(EXEC_ACTION) ){
				String taskName = null;
				Map<String,String> env = new HashMap<String, String>();
				Schedule mSchedule = ((App)getApplication()).getSchedule();


				if( intent.hasExtra("script") ){
					taskName = intent.getExtras().getString("script");
				}
				if( intent.hasExtra("widget") ){
					taskName = intent.getExtras().getString("widget");
					env.put("APP_WIDGET_ID",  intent.getIntExtra("widget_id",0)+"" );
				}
				
				if( intent.hasExtra("params") ){
					Bundle params = intent.getExtras().getBundle("params");
					
					for(Iterator<String> it = params.keySet().iterator(); it.hasNext(); ){
						String param_name = it.next();
						String param_value = params.getString(param_name);
						
						env.put("PARAM_"+param_name.toUpperCase(), param_value);
					}
				}

                Task task = mSchedule.getTaskByName( taskName );
                if(task==null) return START_NOT_STICKY;
                else           executeTask(task, env);

				if(!mStarted) stopSelf(startId);
				return START_NOT_STICKY;
			}
			

			if( action.equals(EXEC_WRAPPER_ACTION) ){
                Task task = new Task().setCommand( intent.getStringExtra("command") );
                executeTask(task, null);
			}
		}
		
		if(!mStarted) stopSelf(startId);
		return START_NOT_STICKY;
	}

	private ArrayList<String> splitToArgs(String command){
		ArrayList<String> args = new ArrayList<String>();
		
		Pattern split = Pattern.compile("\\S+|(\".*[^\\\\]\")");
		
		Matcher match = split.matcher(command);
		while( match.find() ){
			String argument = match.group();
			args.add( argument );
		}
		return args;
	}


	private void executeTask(final Task task, final Map<String, String> env){
        final String command = task.getCommand();
        final ArrayList<String> args = splitToArgs(command.trim());

        Thread execThread = new Thread(new Runnable(){
            public void run() {
                try{
                        ProcessBuilder builder = new ProcessBuilder(args).redirectErrorStream(true).directory(scriptDir);

                        Map<String, String> process_env = builder.environment();
                        // Global environment values

                        process_env.putAll(((App) getApplication()).getSchedule().getGlobalEnv());

                        // Script env values
                        if (env != null) {
                            process_env.putAll(env);
                        }

                        Process proc = builder.start();

                        boolean saveLog = task.getLogging();
                        BufferedWriter log = null;
                        if (saveLog) {
                            log = new BufferedWriter(new FileWriter(getLogFile(task.getName()), true));
                            log.write(dateFormat.format(new Date()) + ": running command \"" + command + "\"\n");
                        }

                        BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                        char[] buffer = new char[8192];
                        int rc;
                        while (true) {
                            rc = input.read(buffer);
                            if (rc == -1) break;
                            if (saveLog) {
                                log.write(buffer, 0, rc);
                            }
                        }

                        proc.waitFor();

                        if (saveLog) {
                            log.write("\n");
                            log.flush();
                            log.close();
                        }

                        if (task.getNotify()) {
                            App app = (App) getApplication();
                            app.getServerThread().getHandler().post(new Runnable() {
                                                                        public void run() {
                                                                            Toast toast = Toast.makeText(getApplicationContext(), "Task " + task.getName() + " finished", Toast.LENGTH_LONG);
                                                                            toast.show();
                                                                        }
                                                                    }
                            );
                        }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
        execThread.start();
	}
	
	private void processTimesTasks(){
		Calendar minStart = null;
		Schedule mSchedule = ((App)getApplication()).getSchedule();

        for(Iterator<Task> it=mSchedule.getTasks().iterator(); it.hasNext(); ){
            Task task = it.next();
            if( !task.getEnabled() ) continue;
            boolean need_execute = false;

            for(Iterator<TaskSchedule> its = task.getSchedulesIterator(); its.hasNext(); ){
                TaskSchedule ts = its.next();
                if(ts.getType()!=TaskSchedule.SCHEDULE_TYPE_TIME) continue;

                if( ts.isNow() ) need_execute = true;
                Calendar nextTime = ts.getNextTime();
                if( minStart==null || minStart.after(nextTime) ) minStart = nextTime;
            }

            if( need_execute )
                executeTask(task, null);
        }

		if( minStart!=null ){
			PendingIntent intent = PendingIntent.getBroadcast(
					this, 0, 
					new Intent( TICK_ACTION ), 0
			);
			AlarmManager alarmMan = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
			alarmMan.set(AlarmManager.RTC_WAKEUP, minStart.getTimeInMillis(), intent);
		}
	}
	
	public void processActionTasks(){
		Schedule mSchedule = ((App)getApplication()).getSchedule();
		
		IntentFilter filter = new IntentFilter(TICK_ACTION);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		filter.addAction(SET_NOTIFY_ACTION);
		unregisterReceiver(mReceiver);

        for(Iterator<Task> it=mSchedule.getTasks().iterator(); it.hasNext(); ){
            Task task = it.next();
            if( !task.getEnabled() ) continue;
            for(Iterator<TaskSchedule> its = task.getSchedulesIterator(); its.hasNext(); ){
                TaskSchedule ts = its.next();
                if(ts.getType()!=TaskSchedule.SCHEDULE_TYPE_EVENT) continue;

                Log.i("action","Task "+task.getName()+" on "+ts.getSchedule());

                filter.addAction( ts.getSchedule() );
            }
        }

		registerReceiver(mReceiver, filter);
	}

	
	public static void executeTask(Context ctx, String name){
		executeTask(ctx, name, null);
	}
	
	public static void executeTask(Context ctx, String name, Map<String, String> params){
		Intent intent = new Intent(EXEC_ACTION)
				.setClass( ctx, ExecService.class)
				.putExtra("script", name);
		if( params!=null ){
			Bundle data = new Bundle();
			for(Iterator<Entry<String,String>> it = params.entrySet().iterator(); it.hasNext(); ){
				Entry<String,String> entry = it.next();
				data.putString( entry.getKey(), entry.getValue() );
			}
			intent.putExtra("params", data);
		}
		
		ctx.startService(intent);
	}
	
	public static void executeWidget(Context ctx, String name, int widgetId){
		Intent intent = new Intent(EXEC_ACTION)
				.setClass( ctx, ExecService.class)
				.putExtra("widget", name)
				.putExtra("widget_id", widgetId);
		
		ctx.startService(intent);
	}
	
	public static Intent getServiceIntent(Context ctx){
		return new Intent(ctx, ExecService.class);
	}
	public static Intent getStartIntent(Context ctx){
		return  getServiceIntent(ctx).setAction( START_ACTION );
	}
	public static Intent getRestartIntent(Context ctx){
		return getServiceIntent(ctx).setAction( RESTART_ACTION );
	}
	
	public static File getLogFile(String name){
		return new File(App.baseDir+"/logs", name+".log");
	}
}
