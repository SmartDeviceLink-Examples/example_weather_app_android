package com.ford.mobileweather.weather;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ford.mobileweather.artifact.WeatherLocation;
import com.ford.mobileweather.smartdevicelink.SmartDeviceLinkApplication;

public class WeatherAlarmManager {
	
	private static int PENDING_INTENT_ID = 100000;
	
	private boolean mAlarmRunning = false;
	private int mUpdateInterval = 5;	// The number of minutes between weather updates
	private AlarmManager mAlarmManager = null; // Alarm manager to manage recurring updates
	private PendingIntent mAlarmIntent = null; // Intent used to trigger an update
	private boolean mFirstLocationUpdate = true; // Used to trigger a manual weather update on the first location update
	private WeatherDataManager mDataManager = null;
	private Context mAppContext = null;
	private Intent mUpdateIntent = null;

	/**
	 * Receiver 
	 */
	private final BroadcastReceiver mChangeUpdateIntervalReceiver = new BroadcastReceiver() {
		
        @Override
        public void onReceive(Context context, Intent intent) {
        	Log.d(SmartDeviceLinkApplication.TAG, "UpdateIntervalReceiver");
        	if (mAlarmRunning && mDataManager != null) {
        		mUpdateInterval = mDataManager.getUpdateInterval();
        		restartAlarm(context);
        	}
        }
	};

	/**
	 * Receiver 
	 */
	private final BroadcastReceiver mChangeLocationReceiver = new BroadcastReceiver() {
		
        @Override
        public void onReceive(Context context, Intent intent) {
        	WeatherLocation currentLocation = null;
        	String lastCity = null;
        	String lastState = null;
        	Log.d("MobileWeather", "WeatherAlarmManager - ChangeLocationReceiver");
    		if (mDataManager != null) {
    			currentLocation = mDataManager.getCurrentLocation();
    			lastCity = mDataManager.getLastCity();
    			lastState = mDataManager.getLastState();
    		}        	
    		if (mFirstLocationUpdate) {
    			Log.d("MobileWeather", "WeatherAlarmManager - first location - starting weather update alarm");
    			restartAlarm(context);
    			mFirstLocationUpdate = false;
    			mAppContext.sendBroadcast(mUpdateIntent);
    		}
    		else if (currentLocation != null && currentLocation.city != null) {
    			if((!currentLocation.city.equals(lastCity)) || 
    					((currentLocation.state != null) && (!currentLocation.state.equals(lastState)))) {
    				Log.d("MobileWeather", "city/state change - weather update");
    				mAppContext.sendBroadcast(mUpdateIntent);
    			}
    		}		   		
        }
	};
	
	/**
	 * Receiver 
	 */
	private final BroadcastReceiver mRoamingStatusReceiver = new BroadcastReceiver() {
		
        @Override
        public void onReceive(Context context, Intent intent) {
    		Bundle bundle = intent.getExtras();
    		if (bundle != null) {
    			boolean roaming = bundle.getBoolean("roaming");
    			if (roaming) {	    			
	    			if (mAlarmManager != null && mAlarmIntent != null) {
	    				mAlarmManager.cancel(mAlarmIntent);
	    				mAlarmIntent = null;
	    				mAlarmRunning = false;
	    			}
    			}
    			else {
    	        	Log.d("MobileWeather", "WeatherAlarmManager - restarting alarm");
    				restartAlarm(context);
    			}
    		}
        }
	};
	
	public WeatherAlarmManager(Class<?> weatherService) {
		Log.d(SmartDeviceLinkApplication.TAG, "WeatherAlarmManager - constructed");
		mAlarmRunning = false;
		mDataManager = WeatherDataManager.getInstance();
		mAppContext = SmartDeviceLinkApplication.getInstance().getApplicationContext();
		if (mDataManager != null) {
			mUpdateInterval = mDataManager.getUpdateInterval();
		}
		String weatherServiceName = weatherService.getName();
		mUpdateIntent = new Intent(mAppContext, WeatherUpdateWakefulReceiver.class);
		mUpdateIntent.putExtra(WeatherUpdateWakefulReceiver.WEATHER_UPDATE_SERVICE, weatherServiceName);
	}
	
	public void startPendingLocation() {
		Log.d(SmartDeviceLinkApplication.TAG, "WeatherAlarmManager - registering local receivers");
		LocalBroadcastManager lbManager = LocalBroadcastManager.getInstance(mAppContext);
		lbManager.registerReceiver(mChangeUpdateIntervalReceiver, new IntentFilter("com.ford.mobileweather.ChangeUpdateInterval"));
		lbManager.registerReceiver(mChangeLocationReceiver, new IntentFilter("com.ford.mobileweather.Location"));
		lbManager.registerReceiver(mRoamingStatusReceiver, new IntentFilter("com.ford.mobileweather.RoamingStatus"));		
	}
	
	public void stop() {
		Log.d(SmartDeviceLinkApplication.TAG, "WeatherAlarmManager - unregistering local receivers");
		cancelUpdates();
		try {
			LocalBroadcastManager lbManager = LocalBroadcastManager.getInstance(mAppContext);
			lbManager.unregisterReceiver(mChangeUpdateIntervalReceiver);
			lbManager.unregisterReceiver(mChangeLocationReceiver);
			lbManager.unregisterReceiver(mRoamingStatusReceiver);

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Stop and cancel any update alarms, and start/restart the timer.
	 * This is used for periodic weather updates.
	 */
	private void restartAlarm(Context context) {
		Log.d(SmartDeviceLinkApplication.TAG, "WeatherAlarmManager - restartAlarm");
		if (mAlarmManager == null) {
			mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		}
		
		if (mAlarmManager != null && mAlarmIntent != null) {
			mAlarmManager.cancel(mAlarmIntent);
		}
		
		mAlarmIntent = PendingIntent.getBroadcast(context, PENDING_INTENT_ID, mUpdateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.MINUTE, mUpdateInterval);
        Log.d(SmartDeviceLinkApplication.TAG, "restartAlarm mUpdateInterval = " + mUpdateInterval);

		mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), (mUpdateInterval*60*1000), mAlarmIntent);
		mAlarmRunning = true;
	}
	
	/**
	 * Stop and cancel any update alarm.
	 */
	private void cancelUpdates() {
		Log.i(SmartDeviceLinkApplication.TAG, "Canceling periodic weather updates..");
		if (mAlarmManager != null && mAlarmIntent != null) {
			mAlarmManager.cancel(mAlarmIntent);
			mAlarmRunning = false;
		}
	}
}
