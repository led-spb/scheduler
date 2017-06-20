package ru.led.scheduler;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Alexey.Ponimash on 17.04.2015.
 */
public class TaskSchedule {
    public static int SCHEDULE_TYPE_EVENT = 0;
    public static int SCHEDULE_TYPE_TIME = 1;
    private Task mAssignedTask;
    private String mSchedule;

    private int mScheduleType = SCHEDULE_TYPE_EVENT;

    public TaskSchedule(){
        super();
    }

    public String getSchedule(){
        return mSchedule;
    }
    public void setSchedule(String schedule){
        mSchedule = schedule;
        mScheduleType = mSchedule.split(" ").length==5? SCHEDULE_TYPE_TIME: SCHEDULE_TYPE_EVENT;
    }

    public int getType(){
        return mScheduleType;
    }

    public void assignTo(Task task){
        mAssignedTask = task;
    }
    public Task getAssignedTask(){
        return mAssignedTask;
    }

    public Calendar getNextTime(){
        if( getType()!= SCHEDULE_TYPE_TIME ) return null;

        Calendar c = Calendar.getInstance();

        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MINUTE, 1);

        String[] parts = getSchedule().trim().split(" ");
        while( !check(parts[0], c.get(Calendar.MINUTE)) ){
            c.add(Calendar.MINUTE,1);
        }
        while( !check(parts[1], c.get(Calendar.HOUR_OF_DAY)) ){
            c.add(Calendar.HOUR_OF_DAY,1);
        }
        while( !check(parts[2], c.get(Calendar.DAY_OF_MONTH)) ){
            c.add(Calendar.DAY_OF_MONTH,1);
        }
        while( !check(parts[4], c.get(Calendar.DAY_OF_WEEK)-1==0?7:c.get(Calendar.DAY_OF_WEEK)-1) ){
            c.add(Calendar.DAY_OF_MONTH,1);
        }
        while( !check(parts[3], c.get(Calendar.MONTH)+1) ){
            c.add(Calendar.MONTH, 1);
        }

        return c;
    }

    public boolean isNow(){
        if( getType()!=SCHEDULE_TYPE_TIME ) return false;

        Calendar c = Calendar.getInstance();

        c.set(Calendar.SECOND,0);
        c.set(Calendar.MILLISECOND, 0);

        String[] parts = getSchedule().trim().split(" ");

        int dow = c.get(Calendar.DAY_OF_WEEK)-1==0?7:c.get(Calendar.DAY_OF_WEEK)-1;

        return check(parts[0], c.get(Calendar.MINUTE)) &&
               check(parts[1], c.get(Calendar.HOUR_OF_DAY)) &&
               check(parts[2], c.get(Calendar.DAY_OF_MONTH)) &&
               check(parts[3], c.get(Calendar.MONTH)+1) &&
               check(parts[4], dow);

    }

    private Pattern mDiap = Pattern.compile("(\\d+)-(\\d+)");
    private Pattern mDiv = Pattern.compile("(.+)/(\\d+)");

    private int s2int(String s, int def){
        try{
            return Integer.parseInt(s);
        }catch(NumberFormatException e){
            return def;
        }
    }
    private boolean check_simple(String s, int d){
        if( s.equals("*") ) return true;
        return s2int(s,0)==d;
    }

    private boolean check_diap(String s, int d){
        Matcher m = mDiap.matcher(s);
        if( m.matches() ){
            int left = s2int(m.group(1), 0);
            int right = s2int(m.group(2), 0);
            return left<=d && d<=right;
        }else
            return check_simple(s,d);
    }

    private boolean check_div(String s, int d){
        Matcher m = mDiv.matcher(s);
        if( m.matches() ){
            String s1 = m.group(1);
            int div = s2int(m.group(2), 1);

            if( !check_diap(s1,d) ) return false;
            return d%div == 0;
        }else
            return check_diap(s,d);
    }

    private boolean check_list(String s, int d){
        for(String item: s.split(",") ){
            if( check_div(item,d) )	return true;
        }
        return false;
    }

    private boolean check(String s, int d){
        if( s.equals("*") ) return true;
        return check_list(s,d);
    }
}
