package ru.led.scheduler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.util.Log;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.AbsListView;
import android.widget.TabHost;

public class MainActivity extends Activity implements AdapterView.OnItemClickListener{
	private enum ActivityMode {MODE_DEFAULT, MODE_SHORTCUT, MODE_WIDGET};
	
	public  Schedule mSchedule;
	private ListAdapter mTasksAdapter, mWidgetsAdapter;
	private ActivityMode mActivityMode = ActivityMode.MODE_DEFAULT;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
	    if( getIntent().getAction()!=null && getIntent().getAction().equals( Intent.ACTION_CREATE_SHORTCUT ) )
	    	mActivityMode = ActivityMode.MODE_SHORTCUT;
	    
	    if( getIntent().getAction()!=null && getIntent().getAction().equals( AppWidgetManager.ACTION_APPWIDGET_CONFIGURE ) )
	    	mActivityMode = ActivityMode.MODE_WIDGET;
	    
	    Log.i("MainActivity.onCreate","mode="+mActivityMode);
	    
	    mSchedule = ((App)getApplication()).getSchedule();
  
	    setContentView( R.layout.main_activity );

        // Configure tab host
        TabHost host = (TabHost) findViewById(android.R.id.tabhost);
	    host.setup();
	    
	    TabHost.TabSpec tab = host.newTabSpec("tasks");
	    tab.setIndicator( getText(R.string.tab_tasks_title));
	    tab.setContent( R.id.tasks_list_view );
	    host.addTab(tab);


        tab = host.newTabSpec("schedules");
        tab.setIndicator( getText(R.string.tab_schedule_title) );
        tab.setContent( R.id.schedule_list_view);
        host.addTab(tab);
/*
        tab = host.newTabSpec("widgets");
	    tab.setIndicator( getText(R.string.tab_widgets_title) );
	    tab.setContent( R.id.widgets_list_view);
	    host.addTab(tab);

*/
	    // fill tasks list view
        int filterTasks = Task.TASK_TYPE_ALL;
        switch(mActivityMode){
            case MODE_SHORTCUT:
                filterTasks = Task.TASK_TYPE_TASK;
                break;
            case MODE_WIDGET:
                filterTasks = Task.TASK_TYPE_WIDGET;
                break;
        }

        mTasksAdapter = mSchedule.getTasksAdapter(this, R.layout.item_view_icon, filterTasks);
	    AbsListView tasksView = (AbsListView) findViewById(R.id.tasks_list_view);
        tasksView.setAdapter( mTasksAdapter);
	    tasksView.setOnItemClickListener(this);
	    registerForContextMenu(tasksView);

/*
	    // fill widgets list view
        AbsListView widgetsView = (AbsListView) findViewById(R.id.widgets_list_view);
	    ArrayList<HashMap<String, String>> widgetsData = jsonToList( mSchedule.getWidgets() );
        mWidgetsAdapter = new SimpleAdapter(this, widgetsData,R.layout.item_view_icon,
                new String[]{"name"}, new int[]{R.id.item_title}
        );
	    widgetsView.setAdapter( mWidgetsAdapter );
	    widgetsView.setOnItemClickListener(this);
	    registerForContextMenu(widgetsView);
*/

        // fill schedule list view
        AbsListView schedulesView = (AbsListView) findViewById(R.id.schedule_list_view);
        ListAdapter mScheduleAdapter = mSchedule.getSchedulesAdapter(this, R.layout.item_view_line2);
        schedulesView.setAdapter( mScheduleAdapter );
	}

	@Override
	public boolean onCreateOptionsMenu (Menu menu){
		getMenuInflater().inflate(R.menu.main_activity_options_menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item){
		switch ( item.getItemId() ){
			case R.id.miServiceStart:
				startService( ExecService.getStartIntent(this) );
				break;
			case R.id.miServiceStop:
				stopService( ExecService.getServiceIntent(this) );
				break;
			case R.id.miServiceRestart:
				startService( ExecService.getRestartIntent(this) );
				break;

            case R.id.miSettings:
                startActivity(new Intent(this, SettingsActivity.class) );
                break;
		}
		
		return true;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View view,  ContextMenuInfo menuInfo) {
	    AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
	    int position = info.position;

        Task task = (Task)mTasksAdapter.getItem(position);//mSchedule.getTasks().get(position);

        getMenuInflater().inflate( R.menu.main_activity_context_menu, menu );
        menu.setHeaderTitle( task.getName() );

        boolean logEnabled = task.getLogging();

        menu.findItem( R.id.miShowLog ).setVisible(logEnabled);
        menu.findItem( R.id.miClearLog ).setVisible( logEnabled );
        menu.findItem( R.id.miEnableLog ).setVisible( !logEnabled );
        menu.findItem( R.id.miDisableLog ).setVisible( logEnabled );

        boolean scriptEnabled = task.getEnabled();
        menu.findItem( R.id.miEnableScript ).setVisible( !scriptEnabled );
        menu.findItem( R.id.miDisableScript ).setVisible( scriptEnabled );
	}

	@Override
	public boolean onContextItemSelected (MenuItem item){
        int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;

        Task task = (Task)mTasksAdapter.getItem(position); // mSchedule.getTasks().get(position);

        switch( item.getItemId() ){
            case R.id.miShowLog:
                    startActivity( new Intent(this, LogActivity.class).putExtra("name", task.getName() ) );
                    break;
            case R.id.miClearLog:
                    ExecService.getLogFile( task.getName() ).delete();
                    break;
            case R.id.miExecuteScript:
                    executeTask(task);
                    break;

            case R.id.miEnableLog:
            case R.id.miDisableLog:
                    task.setLogging( !task.getLogging() );
                    break;

            case R.id.miEnableScript:
            case R.id.miDisableScript:
                    task.setEnabled(!task.getEnabled());
                    break;

            case R.id.miTaskEdit:
                    Intent editIntent = (new Intent(this, TaskEditActivity.class)).putExtra("task_id", task.getId() );
                    startActivity( editIntent );
                    break;
        }

        return true;
	}

    @Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id){
        Task task = (Task)mTasksAdapter.getItem(position);

        switch( mActivityMode ){
            case MODE_DEFAULT:
                AbsListView listView = (AbsListView) findViewById(R.id.tasks_list_view);
                listView.showContextMenuForChild(view);
                break;

            case MODE_WIDGET:
                configureWidget(task);
                break;

            case MODE_SHORTCUT:
                configureShortcut(task);
                break;

            default:
                break;
        }
	}
	
	private interface SelectAction{
		public void Execute(String scriptName, Map<String,String> params);
	}
	
	private void onTaskSelected(final Task task, Map<String, String> extraParams, final SelectAction action){
        //Task task = mSchedule.getTaskByName(taskName);
		//JSONObject script = mSchedule.getScript(taskName);
		if(task==null) return;

		HashMap<String, String> params_map = new HashMap<String, String>();

		if( extraParams!= null ) params_map.putAll(extraParams);


		if( task.getParams().size()>0 ){
            for(Iterator<String> it = task.getParams().iterator(); it.hasNext(); ){
                params_map.put( it.next(), "" );
            }
		}


		if( !params_map.isEmpty() ){
			// Show configure dialog
			new MultilinePromptDialog(this, getString(R.string.shortcut_title), params_map){
				@Override
				public boolean onOkClicked(Map<String, String> values) {
					action.Execute(task.getName(), values);
					return true;
				}
				
			}.show();
		}else{
			// Direct execute
			action.Execute(task.getName(), null);
		}
	}
	
	
	private void executeTask(final Task task){
		onTaskSelected(task, null,
                new SelectAction() {
                    public void Execute(String scriptName, Map<String, String> params) {
                        ExecService.executeTask(MainActivity.this, scriptName, params);
                    }
                }
        );
		
	}
	
	private void configureShortcut(final Task task){
		Map<String,String> shortcutParams = new HashMap<String,String>();
		shortcutParams.put("title", task.getName());

		onTaskSelected(task, shortcutParams,
                new SelectAction() {
                    public void Execute(String scriptName, Map<String, String> params) {
                        String title = params.get("title");
                        params.remove("title");


                        final Intent shortcutIntent = new Intent();
                        final Intent launchIntent = DummyActivity.getShortcutIntent(MainActivity.this, task.getName(), params);

                        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launchIntent);

                        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
                        shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                                Intent.ShortcutIconResource.fromContext(MainActivity.this, R.drawable.ic_launcher)
                        );
                        MainActivity.this.setResult(RESULT_OK, shortcutIntent);
                        MainActivity.this.finish();
                    }

                }
        );
	}
	
	private void configureWidget(final Task task){
		Bundle extras = getIntent().getExtras();
		final int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);


		Intent configIntent = new Intent(this, WidgetPreferenceActivity.class)
			.putExtra("widget_id", appWidgetId)
			.putExtra("widget_name", task.getName() )
			.putExtra("widget_prefs", (new JSONArray(task.getParams())).toString());
		startActivity( configIntent );
		
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, resultValue);
		finish();
	}

}
