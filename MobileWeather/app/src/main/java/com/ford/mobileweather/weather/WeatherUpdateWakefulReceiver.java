package com.ford.mobileweather.weather;

import com.ford.mobileweather.smartdevicelink.SmartDeviceLinkApplication;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class WeatherUpdateWakefulReceiver extends WakefulBroadcastReceiver {
	
	protected final static String WEATHER_UPDATE_SERVICE = "weather_update_service";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String className = intent.getStringExtra(WEATHER_UPDATE_SERVICE);
		Intent service;
		if(className != null){
			try {
				service = new Intent(context, Class.forName(className));
		        startWakefulService(context, service);
			} catch (ClassNotFoundException e) {
				Log.d(SmartDeviceLinkApplication.TAG, "WakefulBroadcastReceiver - invalid class name");
				e.printStackTrace();
			}
		}else{
			Log.d(SmartDeviceLinkApplication.TAG, "WakefulBroadcastReceiver - no service specified");
		}
	}
}
