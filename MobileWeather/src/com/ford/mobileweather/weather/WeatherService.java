package com.ford.mobileweather.weather;

import java.net.URL;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ford.mobileweather.artifact.WeatherLocation;

public abstract class WeatherService extends IntentService {
	
	protected boolean mFirstLocationUpdate = true; // Used to trigger a manual weather update on the first location update
	protected WeatherDataManager mDataManager = null;
	protected WeatherJsonProcessor mWeatherProcessor = null;

	protected abstract URL[] getURLs(WeatherLocation currentLocation);
	
	protected abstract void updateWeatherData(URL... urls);
	
	public WeatherService() {
		super("WeatherService");
	}
				
	@Override
    protected void onHandleIntent(Intent intent) {

    	Log.d("MobileWeather", "WeatherService - Intent recieved");
		WeatherLocation currentLocation = null;
		mDataManager = WeatherDataManager.getInstance();
		
		if (mDataManager != null) {
			currentLocation = mDataManager.getCurrentLocation();
		}
		
		if (currentLocation != null) {
			Log.d("MobileWeather", "WeatherService - valid location");
			URL[] weatherURLs = getURLs(currentLocation);
			updateWeatherData(weatherURLs);
		}
		
		WeatherUpdateWakefulReceiver.completeWakefulIntent(intent);
	}
	
	protected void reportConnectionAvail(boolean avail){
		if(mDataManager != null){
			if(mDataManager.isNetworkAvailable() != avail){
				ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);					 
				NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
				boolean isConnected = activeNetwork != null &&
				                      activeNetwork.isConnectedOrConnecting();
				mDataManager.setNetworkAvailable(isConnected);
				if(!isConnected){
					Intent intent = new Intent("com.ford.mobileweather.ErrorUpdate");
					LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
				}
			}
		}
	}
	
	protected void reportApiAvail(boolean avail){
		if (mDataManager != null){
			if(avail != mDataManager.isAPIAvailable()){
				mDataManager.setAPIAvailable(avail);
				Intent intent = new Intent("com.ford.mobileweather.ErrorUpdate");
				LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
			}
		}
	}
}
