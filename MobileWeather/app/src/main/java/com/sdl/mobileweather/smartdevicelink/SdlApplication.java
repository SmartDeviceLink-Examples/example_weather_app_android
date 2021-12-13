package com.sdl.mobileweather.smartdevicelink;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.util.Log;

import com.sdl.mobileweather.R;
//import com.sdl.mobileweather.forecastio.ForecastIoService;
import com.sdl.mobileweather.localization.LocalizationUtil;
import com.sdl.mobileweather.location.PlayServicesConnectionChecker;
import com.sdl.mobileweather.location.WeatherLocationServices;
import com.sdl.mobileweather.openweathermap.OpenWeatherMapService;
import com.sdl.mobileweather.weather.WeatherAlarmManager;
import com.sdl.mobileweather.weather.WeatherDataManager;
import com.sdl.mobileweather.weather.WeatherUpdateWakefulReceiver;
import com.sdl.mobileweather.BuildConfig;
import com.smartdevicelink.managers.SdlManager;

import java.util.Locale;


public class SdlApplication extends Application {
	
	public static final String TAG = "MobileWeather";
	private static SdlApplication instance;
	private static Activity currentUIActivity;
	private WeatherLocationServices mLocationServices;
	private WeatherDataManager mDataManager;
	private WeatherAlarmManager mWeatherAlarm;	
	private LocalizationUtil mLocalizationUtil;

	
	static {
		instance = null;
	}
	
	private static synchronized void setInstance(SdlApplication app) {
		instance = app;
	}
	
	public static synchronized SdlApplication getInstance() {
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
		SdlApplication.setInstance(this);
		mDataManager = new WeatherDataManager();
		// TODO: Fix magic number assignment of update interval
		mDataManager.setUpdateInterval(5);
		mDataManager.setUnits(getResources().getString(R.string.units_default));
		mLocalizationUtil = new LocalizationUtil();
		//mWeatherAlarm = new WeatherAlarmManager(ForecastIoService.class);
		mWeatherAlarm = new WeatherAlarmManager(OpenWeatherMapService.class);
		mLocationServices = null;
		if (PlayServicesConnectionChecker.servicesConnected()) {
			mLocationServices = new WeatherLocationServices();
		}
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "onConfigurationChanged received");
		Context mAppContext =  SdlApplication.getInstance().getApplicationContext();
		Intent mUpdateIntent = new Intent(mAppContext, WeatherUpdateWakefulReceiver.class);
    	//mUpdateIntent.putExtra("weather_update_service", ForecastIoService.class.getName());
		mUpdateIntent.putExtra("weather_update_service", OpenWeatherMapService.class.getName());
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
		Log.i(SdlApplication.TAG, "Starting SmartDeviceLink service");
		if (BuildConfig.TRANSPORT.equals("MULTI")) {
			Log.i(TAG, "startSdlProxyService: Multiplex Selected");
			SdlReceiver.queryForConnectedService(this);
		} else if (BuildConfig.TRANSPORT.equals("TCP")) {
			Log.i(TAG, "startSdlProxyService: TCP Selected");
			Intent proxyIntent = new Intent(this, SdlService.class);
			startService(proxyIntent);
		}
	}
    
    // Recycle the proxy
	public void endSdlProxyInstance() {	
		SdlService serviceInstance = SdlService.getInstance();
		if (serviceInstance != null){
			SdlManager sdlManagerInstance = serviceInstance.getSdlManager();
			// if proxy exists, reset it
			if(sdlManagerInstance != null){
				serviceInstance.reset();
			// if proxy == null create proxy
			} else {
				serviceInstance.startProxy();
			}
		}
	}

    public void startWeatherUpdates() {
    	Log.i(SdlApplication.TAG, "Starting weather updates");
    	mWeatherAlarm.startPendingLocation();
    }
    
    public void endWeatherUpdates() {
    	Log.i(SdlApplication.TAG, "Stopping weather updates");
    	mWeatherAlarm.stop();
    }
    
    public void startLocationServices() {
    	Log.i(SdlApplication.TAG, "Starting location service");
    	if (mLocationServices != null) {
    		mLocationServices.start();
    	}
    }

    public void endLocationServices() {
    	Log.i(SdlApplication.TAG, "Stopping location service");
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
    	noApplinkService = (SdlService.getInstance() == null);
    	
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
    	SdlService serviceInstance = SdlService.getInstance();
    	try {
    		appMessage = getResources().getString(R.string.mobileweather_ver) + 
    				getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    	} catch (NameNotFoundException e) {
    		Log.d(SdlApplication.TAG, "Can't get package info", e);
    	}

		proxyMessage = getResources().getString(R.string.proxy_ver) + BuildConfig.VERSION_NAME;
		new AlertDialog.Builder(context).setTitle((getResources().getString(R.string.app_ver)))
    	.setMessage(appMessage + "\r\n" + proxyMessage)
    	.setNeutralButton(android.R.string.ok, null).create().show();
    }
}
