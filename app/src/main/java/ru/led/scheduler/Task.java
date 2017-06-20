package ru.led.scheduler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Observable;
import java.util.UUID;
import java.util.concurrent.Semaphore;

/**
 * Created by Alexey.Ponimash on 17.04.2015.
 */

public class Task extends Observable{
    public static int TASK_TYPE_ALL = -1;
    public static int TASK_TYPE_TASK = 0;
    public static int TASK_TYPE_WIDGET = 1;

    private String  mTaskId;
    private String  mName;
    private String  mCommand;
    private String  mIconUri;
    private Boolean mEnabled = true;
    private Boolean mLockEnabled = false;
    private int     mTaskType = TASK_TYPE_TASK;

    private Boolean mLogging = false;
    private Boolean mNotify  = false;
    private List<TaskSchedule> mSchedules;
    private List<String> mParams;
    private Semaphore mSemaphore;

    public Task(){
        super();
        mTaskId = UUID.randomUUID().toString();
        mSchedules = new ArrayList<TaskSchedule>();
        mParams = new ArrayList<String>();
        mSemaphore = new Semaphore(1);
    }

    public synchronized void acquire() throws InterruptedException{
        mSemaphore.acquire();
    }

    public synchronized void release(){
        mSemaphore.release();
    }

    public synchronized int getPermits(){
        return mSemaphore.availablePermits();
    }

    public String getId(){
        return mTaskId;
    }
    public Boolean getLockEnabled(){
        return mLockEnabled;
    }
    public void setLockEnabled(Boolean lock){
        mLockEnabled=lock;
    }

    public int  getType(){return mTaskType; }
    public Task setType(int taskType){
        mTaskType = taskType;
        notifyObservers();
        return this;
    }

    public String getName(){
        return mName;
    }
    public Task setName(String name){
        mName = name;
        setChanged();
        notifyObservers();
        return this;
    }

    public String getCommand(){
        return mCommand;
    }
    public Task setCommand(String command){
        mCommand = command;
        setChanged();
        notifyObservers();
        return this;
    }

    public Boolean getEnabled(){
        return mEnabled;
    }
    public Task setEnabled(Boolean enabled){
        mEnabled = enabled;
        setChanged();
        notifyObservers();
        return this;
    }

    public String getIcon(){ return mIconUri; }
    public Task setIcon(String uri){
        mIconUri = uri;
        setChanged();
        notifyObservers();
        return this;
    }

    public Boolean getLogging(){
        return mLogging;
    }
    public Task setLogging(Boolean enabled){
        mLogging = enabled;
        setChanged();
        notifyObservers();
        return this;
    }

    public Boolean getNotify(){
        return mNotify;
    }
    public Task setNotify(Boolean enabled){
        mNotify = enabled;
        setChanged();
        notifyObservers();
        return this;
    }
    public List<TaskSchedule> getSchedules(){
        return mSchedules;
    }
    public Iterator<TaskSchedule> getSchedulesIterator(){ return mSchedules.iterator(); }
    public List<String> getParams(){
        return mParams;
    }

    public JSONObject store() throws JSONException{
        JSONObject obj = new JSONObject();

        JSONArray schedules = new JSONArray();

        for(Iterator<TaskSchedule> it = getSchedulesIterator(); it.hasNext(); ){
            schedules.put(it.next().getSchedule());
        }

        JSONArray params = new JSONArray();
        for(Iterator<String> it= getParams().iterator(); it.hasNext(); )
            params.put( it.next() );

        obj.put("id",       mTaskId)
           .put("name",     getName() )
           .put("type",     getType() )
           .put("lock",     getLockEnabled() )
           .put("cmd", getCommand())
           .put("enabled",  getEnabled() )
           .put("log",      getLogging() )
           .put("notify",   getNotify() )
           .put("icon",     getIcon() )
           .put("schedule", schedules )
           .put("params",   params );

        return obj;
    }


    public void load(JSONObject object) throws JSONException{
        mTaskId = object.optString("id", mTaskId);

        setName( object.getString("name") );
        setType(object.optInt("type", TASK_TYPE_TASK));
        setCommand(object.getString("cmd"));
        setEnabled(object.optBoolean("enabled", true));
        setLogging(object.optBoolean("log", false));
        setLockEnabled( object.optBoolean("lock",false) );
        setNotify(object.optBoolean("notify", false));
        setIcon(object.optString("icon", null));

        JSONArray schedules = object.optJSONArray("schedule");
        for(int idx=0; schedules!=null && idx<schedules.length(); idx++){
            TaskSchedule ts = new TaskSchedule();
            ts.setSchedule(schedules.getString(idx));
            mSchedules.add( ts );
        }

        JSONArray params = object.optJSONArray("params");
        for(int idx=0; params!=null && idx<params.length(); idx++){
            mParams.add( params.getString(idx) );
        }
    }

}
