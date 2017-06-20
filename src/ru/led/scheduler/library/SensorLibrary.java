package ru.led.scheduler.library;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SyncResult;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import ru.led.scheduler.ServerThread;
import ru.led.scheduler.tools.CircularBuffer;
import ru.led.scheduler.tools.LockObject;

public class SensorLibrary extends BaseLibrary {
    private SensorManager mSensorMan; 
    
    public SensorLibrary(ServerThread thread) {
        super(thread);
        mSensorMan = (SensorManager) mContext.getSystemService( Context.SENSOR_SERVICE );
    }
    
    
    public JSONObject list(JSONObject params) throws Exception{
        Iterator<Sensor> sensors = mSensorMan.getSensorList( params.has("type")?params.getInt("type"):Sensor.TYPE_ALL ).iterator();
        JSONArray result = new JSONArray();

        while(sensors.hasNext()){
            Sensor mSensor = sensors.next();

            JSONObject pData = new JSONObject();

            pData.put("name", mSensor.getName());
            pData.put("type", mSensor.getType());
            pData.put("vendor", mSensor.getVendor());
            pData.put("version", mSensor.getVersion());
            pData.put("resolution", mSensor.getResolution());
            pData.put("power", mSensor.getPower() );
            pData.put("min_delay", mSensor.getMinDelay() );
            pData.put("max_range", mSensor.getMaximumRange() );

            result.put(pData);
        }

        return OKResponse(result);
    }
    
    
    public JSONObject readSensor(JSONObject params) throws Exception{
        final LockObject mLock = new LockObject();
        final int mEventsRequested = params.optInt("events",0);

        Sensor mSensor = mSensorMan.getDefaultSensor( params.getInt("sensor") );

        SensorEventListener mSensorListener = new SensorEventListener(){
            private List<float[]> mEvents = new ArrayList<float[]>();

            public void onAccuracyChanged(Sensor sensor, int accuracy) {}

            public void onSensorChanged(SensorEvent event) {
             mEvents.add( event.values.clone() );
             if( mEvents.size() >= mEventsRequested )
             {
                     synchronized(mLock){
                         mLock.put( "events", mEvents );
                         mLock.notify();
                     }
             }
            }
        };

        mSensorMan.registerListener(mSensorListener, mSensor, params.optInt("interval", SensorManager.SENSOR_DELAY_NORMAL) );
        try{
            synchronized(mLock){
                mLock.wait( params.optLong("timeout",0) );
            }
        }finally{
            mSensorMan.unregisterListener(mSensorListener);
        }

        JSONArray result = new JSONArray();
        List<float[]> mEvents = (List<float[]>) mLock.get("events");

        for( Iterator<float[]> it = mEvents.iterator(); it.hasNext(); ){
                float[] data = it.next();
                JSONArray event_data = new JSONArray();
                for( int i=0; i<data.length; i++){
                event_data.put( data[i] );
            }
            result.put( event_data );
        }

        return OKResponse(result);
    }
    
    
    public JSONObject getOrientation(JSONObject params) throws Exception{
        final LockObject mLock = new LockObject();
        JSONObject result = new JSONObject();

        SensorEventListener mSensorListener = new SensorEventListener() {
            private float[] accelData,magnetData;

            public void onSensorChanged(SensorEvent event) {
                final int type = event.sensor.getType();

                if( type == Sensor.TYPE_ACCELEROMETER ){
                if( accelData==null)  accelData = new float[3];
                accelData = event.values.clone();
                }

                if( type == Sensor.TYPE_MAGNETIC_FIELD ){
                if( magnetData==null)  magnetData = new float[3];
                magnetData = event.values.clone();
                }

                if(  accelData!=null && magnetData!=null )
                synchronized(mLock){
                    mLock.put("accel", accelData);
                    mLock.put("magnet", magnetData);
                    mLock.notify();
                }
            }
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        mSensorMan.registerListener(mSensorListener, mSensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI );
        mSensorMan.registerListener(mSensorListener, mSensorMan.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI );
        try{
            synchronized(mLock){
            mLock.wait(10*1000);
            float[] accelData = (float[]) mLock.get("accel");
            float[] magnetData = (float[]) mLock.get("magnet");
                float[] rotationMatrix = new float[16];
                float[] orientationData = new float[3];

                SensorManager.getRotationMatrix(rotationMatrix , null,   accelData,  magnetData);
                SensorManager.getOrientation(rotationMatrix, orientationData);

                result.put("xy", orientationData[0] );
                result.put("xz", orientationData[1] );
                result.put("zy", orientationData[2] );
           }
        }finally{
            mSensorMan.unregisterListener(mSensorListener);
        }

        return OKResponse(result);
    }

    private class BufferEventListener implements SensorEventListener{
        public CircularBuffer< float[] > buffer;

        public BufferEventListener(int bufferSize){
            buffer = new CircularBuffer<float[]>(bufferSize);
        }
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(SensorEvent event) {
            buffer.put( event.values.clone() );
        }
    }
    
    BufferEventListener mBufferListener;
    
    public JSONObject bufferStartListening(JSONObject params) throws Exception{
        if( mBufferListener!=null )
            throw new Exception("Already listening");

        mBufferListener = new BufferEventListener( params.optInt("size",50) );
        Sensor mSensor = mSensorMan.getDefaultSensor( params.getInt("sensor") );

        mSensorMan.registerListener( mBufferListener,
            mSensor,
            params.optInt("interval", SensorManager.SENSOR_DELAY_NORMAL)
        );

        return OKResponse();
    }

    public JSONObject bufferRead(JSONObject params) throws Exception{
        if( mBufferListener==null )
            throw new Exception("Not listening");

        JSONArray result = new JSONArray();
        if( mBufferListener.buffer.isEmpty() )
            return OKResponse(result);

        float[] data = mBufferListener.buffer.get();
        for(int i=0; i< data.length; i++)
            result.put( data[i] );
        return OKResponse(result);
    }

    public JSONObject bufferStopListening(JSONObject params) throws Exception{
        if( mBufferListener != null ){
            mSensorMan.unregisterListener(mBufferListener);
            mBufferListener = null;
        }

        return OKResponse();
    }
}
