package com.ford.mobileweather.weather;

import com.ford.mobileweather.artifact.WeatherLocation;

public class WeatherDataManager {

	private static WeatherDataManager instance;
	
	private WeatherLocation mCurrentLocation = null;	// Stores the current location for weather
	private String mUnits = null;	// Stores the current units
	private int mUpdateInterval = 0;	// Stores the update interval 
	private WeatherConditions mWeatherConditions = null;	// Stores the current weather conditions
	private RoadConditions mRoadConditions = null;	// Stores the current road conditions
	private WeatherAlert[] mAlerts = null;	// Stores the current weather alerts
	private Forecast[] mForecast = null;	// Stores the current forecast
	private Forecast[] mHourlyForecast = null;	// Stores the current hourly forecast
	private String mLastCity = null;	// Stores the city of the last weather update
	private String mLastState = null;	// Stores the state of the last weather update
	
	// Locks used for thread safety
	private final Object mCurrentLocationLock = new Object();
	private final Object mUnitsLock = new Object();
	private final Object mUpdateIntervalLock = new Object();
	private final Object mWeatherConditionsLock = new Object();
	private final Object mRoadConditionsLock = new Object();
	private final Object mAlertsLock = new Object();
	private final Object mForecastLock = new Object();
	private final Object mHourlyForecastLock = new Object();
	private final Object mLastCityLock = new Object();
	private final Object mLastStateLock = new Object();

	// Error state booleans
	private boolean mLocationAvailable = false;
	private boolean mAPIAvailable = false;
	private boolean mNetworkAvailable = false;

	private final Object mErrorLock = new Object();

	static {
		instance = null;
	}
	
	private static synchronized void setInstance(WeatherDataManager man) {
		instance = man;
	}
	
	public static synchronized WeatherDataManager getInstance() {
		return instance;
	}

	public WeatherDataManager() {
		WeatherDataManager.setInstance(this);
	}

	public WeatherLocation getCurrentLocation() {
		synchronized (mCurrentLocationLock) {
			return mCurrentLocation;
		}
	}

	public void setCurrentLocation(WeatherLocation location) {
		synchronized (mCurrentLocationLock) {
			this.mCurrentLocation = location;
		}
	}

	public WeatherConditions getWeatherConditions() {
		synchronized (mWeatherConditionsLock) {
			return mWeatherConditions;
		}
	}

	public void setWeatherConditions(WeatherConditions conditions) {
		synchronized (mWeatherConditionsLock) {
			this.mWeatherConditions = conditions;
		}
	}

	public RoadConditions getRoadConditions() {
		synchronized (mRoadConditionsLock) {
			return mRoadConditions;
		}
	}

	public void setRoadConditions(RoadConditions roadConditions) {
		synchronized (mRoadConditionsLock) {
			this.mRoadConditions = roadConditions;
		}
	}

	public WeatherAlert[] getAlerts() {
		synchronized (mAlertsLock) {
			return mAlerts;
		}
	}

	public void setAlerts(WeatherAlert[] alerts) {
		synchronized (mAlertsLock) {
			this.mAlerts = alerts;
		}
	}

	public Forecast[] getForecast() {
		synchronized (mForecastLock) {
			return mForecast;
		}
	}

	public void setForecast(Forecast[] forecast) {
		synchronized (mForecastLock) {
			this.mForecast = forecast;
		}
	}

	public Forecast[] getHourlyForecast() {
		synchronized (mHourlyForecastLock) {
			return mHourlyForecast;
		}
	}

	public void setHourlyForecast(Forecast[] hourlyForecast) {
		synchronized (mHourlyForecastLock) {
			this.mHourlyForecast = hourlyForecast;
		}
	}

	public String getUnits() {
		synchronized (mUnitsLock) {
			return mUnits;
		}
	}

	public void setUnits(String units) {
		synchronized (mUnitsLock) {
			this.mUnits = units;
		}
	}

	public int getUpdateInterval() {
		synchronized (mUpdateIntervalLock) {
			return mUpdateInterval;
		}
	}

	public void setUpdateInterval(int mUpdateInterval) {
		synchronized (mUpdateIntervalLock) {
			this.mUpdateInterval = mUpdateInterval;
		}
	}

	public String getLastCity() {
		synchronized (mLastCityLock) {
			return mLastCity;
		}
	}

	public void setLastCity(String city) {
		synchronized (mLastCityLock) {
			this.mLastCity = city;
		}
	}

	public String getLastState() {
		synchronized (mLastStateLock) {
			return mLastState;
		}
	}

	public void setLastState(String state) {
		synchronized (mLastStateLock) {
			this.mLastState = state;
		}
	}

	public boolean isLocationAvailable() {
		synchronized(mErrorLock){
			return mLocationAvailable;
		}
	}
	
	public void setLocationAvailable(boolean available){
		synchronized(mErrorLock){
			mLocationAvailable = available;
			if(available){
				mNetworkAvailable = true;
			}
		}
	}
	
	public boolean isAPIAvailable() {
		synchronized (mErrorLock) {
			return mAPIAvailable;
		}
	}

	public void setAPIAvailable(boolean available) {
		synchronized (mErrorLock) {
			mAPIAvailable = available;
			if(available){
				mNetworkAvailable = true;
			}
		}
	}
	
	public boolean isNetworkAvailable(){
		synchronized(mErrorLock){
			return mNetworkAvailable;
		}
	}
	
	public void setNetworkAvailable(boolean available){
		synchronized (mErrorLock){
			mNetworkAvailable = available;
		}
	}
	
	public boolean isInErrorState(){
		synchronized(mErrorLock){
			return !(mNetworkAvailable && mAPIAvailable && mLocationAvailable);
		}
	}
}
