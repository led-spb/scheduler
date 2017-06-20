package ru.led.scheduler.library;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;
import ru.led.scheduler.ServerThread;
import ru.led.scheduler.tools.LockObject;

public class LocationLibrary extends BaseLibrary {
    private LocationManager mLocationMan;
    
    public LocationLibrary(ServerThread thread) {
        super(thread);
        mLocationMan = (LocationManager) mContext.getSystemService( Context.LOCATION_SERVICE );
    }

    public JSONObject restart(JSONObject params) throws Exception{
        mLocationMan = (LocationManager) mContext.getSystemService( Context.LOCATION_SERVICE );
        return OKResponse();
    }

    public JSONObject getProviders(JSONObject params) throws Exception{
        return OKResponse( new JSONArray( mLocationMan.getAllProviders() ) );
    }
    
    private JSONObject locationToJSON(Location loc) throws JSONException{
        JSONObject result = new JSONObject();

        if(loc!=null) {
            result.put( "provider", loc.getProvider() );
            result.put( "accuracy", loc.getAccuracy() );
            result.put( "latitude", loc.getLatitude() );
            result.put( "longitude", loc.getLongitude() );
            result.put( "speed", loc.getSpeed() );
            result.put( "time", loc.getTime() );
        }

        return result;
    }
    
    public JSONObject getLastLocation(JSONObject params) throws Exception{
	    String provider = params.optString("provider", "");
        Location location = null;

        if( provider.equals("") ){

            for(String prov: mLocationMan.getProviders(true) ){
                Location l = mLocationMan.getLastKnownLocation(prov);
                if(l==null){
                    continue;
                }
                if(location==null || l.getAccuracy()<location.getAccuracy() ){
                    location = l;
                }
            }
        }else {
            location = mLocationMan.getLastKnownLocation(provider);
        }

	    return OKResponse( locationToJSON(location) );
    }
    


    private LocationListener mLocationListener = null;
    private LockObject mLocationLock = new LockObject();

    public JSONObject startLocation(JSONObject params) throws Exception {
        final String provider = params.getString("provider");
        final float distance = params.optLong("distance", 10);
        final long time = params.optLong("time", 3000);


        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                synchronized(mLocationLock) {
                    mLocationLock.put("location", location);
                    mLocationLock.notifyAll();
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
            }
        };

        startInMainThread( new Runnable() {
            @Override
            public void run() {
                mLocationMan.requestLocationUpdates(provider, time, distance, mLocationListener );
            }
        });

        return OKResponse();
    }


    public JSONObject stopLocation(JSONObject params) throws Exception {
        if( mLocationListener==null )
            throw new Exception("Start location before");

        synchronized (mLocationLock) {
            mLocationMan.removeUpdates(mLocationListener);
            mLocationLock.put("location", null);
        }

        return OKResponse();
    }

    public JSONObject getLocation(JSONObject params) throws Exception{
        long timeout = params.optLong("timeout", 0);

        if( mLocationListener==null )
            throw new Exception("Start location before");

        synchronized (mLocationLock){
            if( timeout>0 ){
                mLocationLock.wait(timeout);
            }
            Location location = (Location) mLocationLock.get("location");
            return OKResponse( locationToJSON(location) );
        }
    }

    
}
