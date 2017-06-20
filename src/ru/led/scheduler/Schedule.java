package ru.led.scheduler;

import android.content.Context;
import android.database.DataSetObserver;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.List;

public class Schedule implements Observer {
    private String mConfigFilename;

    private ArrayList<Task> mTasks;
    private Map<String,String> mEnv;

    public Schedule(String filename){
        mConfigFilename = filename;
        mTasks = new ArrayList<Task>();
        mEnv   = new HashMap<String, String>();

        try {
            load(mConfigFilename);
        }catch(Exception e){
            Log.e( this.getClass().getName(), "Load schedule error", e);
        }
    }

    public Map<String,String> getGlobalEnv(){
        return mEnv;
    }

    public List<Task> getTasks(int taskType){
        ArrayList<Task> result = (ArrayList<Task>)mTasks.clone();
        for(Iterator<Task> it=result.iterator(); taskType!=Task.TASK_TYPE_ALL && it.hasNext(); ){
            if( it.next().getType() != taskType )
                it.remove();
        }
        return result;
    }
    public List<Task> getTasks(){
        return getTasks(Task.TASK_TYPE_ALL);
    }


    public Task getTaskByName(String name){
        for(Iterator<Task> it=mTasks.iterator();it.hasNext();){
            Task task = it.next();
            if( task.getName().equals(name) )
                return task;
        }
        return null;
    }

    public Task getTaskByID(String id){
        for(Iterator<Task> it=mTasks.iterator();it.hasNext();){
            Task task = it.next();
            if( task.getId().equals(id) )
                return task;
        }
        return null;
    }


    private void load(String filename) throws Exception{
        String str = new Scanner(new File(filename)).useDelimiter("\\A").next();
        load((JSONObject) new JSONTokener(str).nextValue());
    }

    private void load(JSONObject obj) throws JSONException {
        JSONArray tasks = obj.optJSONArray("tasks");
        for(int idx=0; tasks!=null && idx<tasks.length(); idx++ ){
            Task task = new Task();
            task.load( tasks.optJSONObject(idx) );

            mTasks.add(task);
            task.addObserver(this);
        }

        JSONObject env = obj.optJSONObject("env");
        for(Iterator<String> it = (env==null?null:env.keys()); env!=null && it.hasNext(); ){
            String key = it.next();
            mEnv.put( key, env.getString(key) );
        }
    }

    private JSONObject store() throws JSONException{
        JSONObject obj = new JSONObject();
        JSONArray tasks = new JSONArray();
        for(Iterator<Task> it=mTasks.iterator();it.hasNext();){
            tasks.put( it.next().store() );
        }

        JSONObject env = new JSONObject();
        for( Iterator<String> it = mEnv.keySet().iterator(); it.hasNext(); ){
            String key = it.next();
            env.put( key, mEnv.get(key) );
        }

        obj.put( "tasks", tasks);
        obj.put( "env", env );

        return obj;
    }

    private void store(String filename) throws Exception{
        FileWriter w = new FileWriter(filename);
        w.write( store().toString(4) );
        w.close();
    }

    public void update(Observable observable, Object o) {
        Log.i(this.getClass().getName(), "Task updated");
        try {
            store(mConfigFilename);
        }catch(Exception e){
            Log.e( this.getClass().getName(), "Store schedule error", e);
        }
    }


    private class TaskListAdapter extends ArrayAdapter<Task>{
        private final int mRowLayout;
        private final List<Task> mTasks;

        public TaskListAdapter(Context context, int rowLayout, List<Task> tasks){
            super(context, rowLayout);
            this.mTasks = tasks;
            addAll( this.mTasks );
            mRowLayout = rowLayout;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent ){
            View rowView = convertView;
            if( rowView == null){
                LayoutInflater inf = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inf.inflate( mRowLayout, null );
            }

            Task mTask = this.mTasks.get(position);
            ((TextView)rowView.findViewById( R.id.item_title )).setText( mTask.getName() );
            ImageView icon = ((ImageView)rowView.findViewById( R.id.item_icon ));
            if( mTask.getIcon()!=null ) icon.setImageURI(Uri.parse(mTask.getIcon()) );
            else icon.setImageResource( R.drawable.ic_launcher );

            return rowView;
        }
    }

    public ListAdapter getTasksAdapter(Context context, int rowLayout, int taskType  ) {
        return new TaskListAdapter(context, rowLayout, getTasks(taskType) );
    }

    public ListAdapter getSchedulesAdapter(Context context, int rowLayout ){
        List<Map<String,String>> data = new ArrayList<Map<String, String>>();
        for(Iterator<Task> it=getTasks().iterator(); it.hasNext();  ){
            Task task = it.next();
            for(Iterator<TaskSchedule> its=task.getSchedulesIterator(); its.hasNext(); ){
                Map<String,String> item = new HashMap<String, String>();
                item.put("name", task.getName() );
                item.put("schedule", its.next().getSchedule() );
                data.add(item);
            }
        }

        return new SimpleAdapter(context, data, rowLayout, new String[]{"name","schedule"}, new int[]{R.id.list_row1, R.id.list_row2});
    }

}
