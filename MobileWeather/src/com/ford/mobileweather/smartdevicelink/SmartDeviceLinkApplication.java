package com.ford.mobileweather.smartdevicelink;

import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.util.Log;

import com.ford.mobileweather.R;
import com.ford.mobileweather.forecastio.ForecastIoService;
import com.ford.mobileweather.location.PlayServicesConnectionChecker;
import com.ford.mobileweather.location.WeatherLocationServices;
import com.ford.mobileweather.smartdevicelink.SmartDeviceLinkService;
import com.ford.mobileweather.weather.WeatherAlarmManager;
import com.ford.mobileweather.weather.WeatherDataManager;
import com.ford.mobileweather.weather.WeatherUpdateWakefulReceiver;
import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.proxy.SdlProxyALM;
import com.ford.mobileweather.localization.LocalizationUtil;


public class SmartDeviceLinkApplication extends Application {
	
	public static final String TAG = "MobileWeather";
	private static SmartDeviceLinkApplication instance;
	private static Activity currentUIActivity;
	private WeatherLocationServices mLocationServices;
	private WeatherDataManager mDataManager;
	private WeatherAlarmManager mWeatherAlarm;	
	private LocalizationUtil mLocalizationUtil;

	
	static {
		instance = null;
	}
	
	private static synchronized void setInstance(SmartDeviceLinkApplication app) {
		instance = app;
	}
	
	public static synchronized SmartDeviceLinkApplication getInstance() {
		return instance;
	}
	
	public static synchronized void setCurrentActivity(Activity act) {
		currentUIActivity = act;
	}
	
	public static synchronized Activity getCurrentActivity() {
		return currentUIActivity;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		SmartDeviceLinkApplication.setInstance(this);
		mDataManager = new WeatherDataManager();
		// TODO: Fix magic number assignment of update interval
		mDataManager.setUpdateInterval(5);
		mDataManager.setUnits(getResources().getString(R.string.units_default));
		mLocalizationUtil = new LocalizationUtil();
		mWeatherAlarm = new WeatherAlarmManager(ForecastIoService.class);
		mLocationServices = null;
		if (PlayServicesConnectionChecker.servicesConnected()) {
			mLocationServices = new WeatherLocationServices();
		}
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "onConfigurationChanged received");
		Context mAppContext =  SmartDeviceLinkApplication.getInstance().getApplicationContext();
		Intent mUpdateIntent = new Intent(mAppContext, WeatherUpdateWakefulReceiver.class);
    	mUpdateIntent.putExtra("weather_update_service", ForecastIoService.class.getName());
    	mAppContext.sendBroadcast(mUpdateIntent);
    	if (mDataManager != null) {
    		mDataManager.setUnits(getResources().getString(R.string.units_default));	
    	}
    	mLocalizationUtil.setLocaleCountry((Locale.getDefault()).getCountry());
    	mLocalizationUtil.setLocaleLanguage((Locale.getDefault()).getLanguage());	    	
	}
	

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}
	
    public void startSdlProxyService() {
    	Log.i(SmartDeviceLinkApplication.TAG, "Starting SmartDeviceLink service");
        // Get the local Bluetooth adapter
        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // If BT adapter exists, is enabled, and there are paired devices, start service/proxy
        if (mBtAdapter != null)
		{
			if ((mBtAdapter.isEnabled() && mBtAdapter.getBondedDevices().isEmpty() == false)) 
			{
	        	Intent startIntent = new Intent(this, SmartDeviceLinkService.class);
	        	startService(startIntent);
			}
		}
	}
    
    // Recycle the proxy
	public void endSdlProxyInstance() {	
		SmartDeviceLinkService serviceInstance = SmartDeviceLinkService.getInstance();
		if (serviceInstance != null){
			SdlProxyALM proxyInstance = serviceInstance.getProxy();
			// if proxy exists, reset it
			if(proxyInstance != null){			
				serviceInstance.reset();
			// if proxy == null create proxy
			} else {
				serviceInstance.startProxy();
			}
		}
	}
	
	// Stop the SmartDeviceLinkService
	public void endSdlProxyService() {
		Log.i(SmartDeviceLinkApplication.TAG, "Ending SmartDeviceLink service");
		SmartDeviceLinkService serviceInstance = SmartDeviceLinkService.getInstance();
		if (serviceInstance != null){
			serviceInstance.stopService();
		}
	}

    public void startWeatherUpdates() {
    	Log.i(SmartDeviceLinkApplication.TAG, "Starting weather updates");
    	mWeatherAlarm.startPendingLocation();
    }
    
    public void endWeatherUpdates() {
    	Log.i(SmartDeviceLinkApplication.TAG, "Stopping weather updates");
    	mWeatherAlarm.stop();
    }
    
    public void startLocationServices() {
    	Log.i(SmartDeviceLinkApplication.TAG, "Starting location service");
    	if (mLocationServices != null) {
    		mLocationServices.start();
    	}
    }

    public void endLocationServices() {
    	Log.i(SmartDeviceLinkApplication.TAG, "Stopping location service");
    	if (mLocationServices != null) {
    		mLocationServices.stop();
    	}
    }
    
    public void startServices() {
    	startSdlProxyService();
        startLocationServices();
        startWeatherUpdates();
    }
    
    public void stopServices() {
    	boolean noUIActivity;
    	boolean noApplinkService;
    	
    	noUIActivity = (currentUIActivity == null);
    	noApplinkService = (SmartDeviceLinkService.getInstance() == null);
    	
    	Log.d(TAG, "Attempting to stop services");
		if(noUIActivity && noApplinkService){
			Log.d(TAG, "Stopping services");
			endWeatherUpdates();
    		endLocationServices();
    		return;		
    	}
    	Log.d(TAG, "Not stopping services");
    }
    
    public void stopServices(boolean appLinkStopping) {
    	if(appLinkStopping){
    		if(currentUIActivity == null){
    			Log.d(TAG, "Attempt force stop services");
    			endWeatherUpdates();
    			endLocationServices();
    		} else {
    			Log.d(TAG, "Application in foreground on phone. Not stopping loc + weath services");
    		}
    	} else {
    		stopServices();
    	}    	
    }

    public void showAppVersion(Context context) {

    	String appMessage = getResources().getString(R.string.mobileweather_ver_not_available);
    	String proxyMessage = getResources().getString(R.string.proxy_ver_not_available);    		    		    		
    	SmartDeviceLinkService serviceInstance = SmartDeviceLinkService.getInstance();
    	try {
    		appMessage = getResources().getString(R.string.mobileweather_ver) + 
    				getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    	} catch (NameNotFoundException e) {
    		Log.d(SmartDeviceLinkApplication.TAG, "Can't get package info", e);
    	}

    	try {
    		if (serviceInstance != null){
    			SdlProxyALM syncProxy = serviceInstance.getProxy();
    			if (syncProxy != null){
    				String proxyVersion = syncProxy.getProxyVersionInfo();
    				if (proxyVersion != null){
    					proxyMessage = getResources().getString(R.string.proxy_ver) + proxyVersion;
    				}    	    			
    			}
    		}	
    	} catch (SdlException e) {
    		Log.d(SmartDeviceLinkApplication.TAG, "Can't get Proxy Version", e);
    		e.printStackTrace();
    	}
    	new AlertDialog.Builder(context).setTitle((getResources().getString(R.string.app_ver)))
    	.setMessage(appMessage + "\r\n" + proxyMessage)
    	.setNeutralButton(android.R.string.ok, null).create().show();
    }
}
