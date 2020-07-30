package com.sdl.mobileweather.forecastio;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.json.JSONObject;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.sdl.mobileweather.artifact.WeatherLocation;
import com.sdl.mobileweather.connection.HttpConnection;
import com.sdl.mobileweather.connection.HttpConnection.RequestMethod;
import com.sdl.mobileweather.processor.JsonProcessor;
import com.sdl.mobileweather.smartdevicelink.SdlApplication;
import com.sdl.mobileweather.weather.Forecast;
import com.sdl.mobileweather.weather.WeatherAlert;
import com.sdl.mobileweather.weather.WeatherConditions;
import com.sdl.mobileweather.weather.WeatherService;


public class ForecastIoService extends WeatherService {

	private static final String API_KEY = "8ca293f8b0e23c52ffdf7d69cbd4dbfd";	// API key used for API 
	private static final String BASE_URL = "https://api.forecast.io/forecast/";	// Base request URL
	
	public ForecastIoService() {
		super();
		mWeatherProcessor = new ForecastIoWeatherJsonProcessor();
	}

	@Override
	protected URL[] getURLs(WeatherLocation currentLocation) {
		URL everythingURL = null;
		try {
			everythingURL = new URL(BASE_URL + API_KEY +"/" + currentLocation.gpsLocation.latitude + "," + 
					currentLocation.gpsLocation.longitude + "?" + "lang=" + (Locale.getDefault()).getLanguage());			
			Log.d(SdlApplication.TAG, currentLocation.gpsLocation.latitude + "," + currentLocation.gpsLocation.longitude);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return new URL[] { everythingURL };
	}

	@Override
	protected void updateWeatherData(URL... urls) {
		HttpConnection httpRequest = new HttpConnection();
		JsonProcessor jsonProcessor = new JsonProcessor();
		WeatherConditions conditions = null;
		Forecast[] forecast = null;
		Forecast[] hourlyForecast = null;
		WeatherAlert[] alerts = null;
		String httpResult = null;
		
		Log.d("MobileWeather", "updateWeatherData");
		if (urls.length == 1 && mDataManager != null && mWeatherProcessor != null) {
			LocalBroadcastManager lbManager = LocalBroadcastManager.getInstance(this);
			Log.d("MobileWeather", "updateWeatherData - valid urls");
			if (urls[0] != null) {
				Log.d("MobileWeather", "updateWeatherData - getting json");
				httpResult = httpRequest.sendRequest(urls[0], RequestMethod.GET, null, "application/json");
				int statusCode = httpRequest.getStatusCode(httpResult);
				if (statusCode == -1) {					
					Log.d("MobileWeather", "updateWeatherData - parsing conditions json");
					JSONObject jsonRoot = jsonProcessor.getJsonFromString(httpResult);
					if (jsonRoot != null) {
						Log.d("MobileWeather", "updateWeatherData - parsing conditions");
						conditions = mWeatherProcessor.getWeatherConditions(jsonRoot);
						mDataManager.setWeatherConditions(conditions);					
						if (conditions != null) {
							Log.d("MobileWeather", "updateWeatherData - new conditions");
							Intent intent = new Intent("com.sdl.mobileweather.WeatherConditions");
							lbManager.sendBroadcast(intent);
						}
	
						Log.d("MobileWeather", "updateWeatherData - parsing forecast");
						forecast = mWeatherProcessor.getForecast(jsonRoot);
						mDataManager.setForecast(forecast);
						if (forecast != null) {
							Log.d("MobileWeather", "updateWeatherData - new forecast");
							Intent intent = new Intent("com.sdl.mobileweather.Forecast");
							lbManager.sendBroadcast(intent);
						}
						
						Log.d("MobileWeather", "updateWeatherData - parsing hourly forecast");	
						hourlyForecast = mWeatherProcessor.getHourlyForecast(jsonRoot);
						mDataManager.setHourlyForecast(hourlyForecast);
						if (hourlyForecast != null) {
							Intent intent = new Intent("com.sdl.mobileweather.HourlyForecast");
							lbManager.sendBroadcast(intent);
						}
	
						alerts = mWeatherProcessor.getAlerts(jsonRoot);
						mDataManager.setAlerts(alerts);
						Log.d("MobileWeather", "updateWeatherData - new Alerts");
						Intent intent = new Intent("com.sdl.mobileweather.Alerts");
						lbManager.sendBroadcast(intent);						
					}
					reportApiAvail(true);
					
				} else if (statusCode == -2){
					reportConnectionAvail(false);
				}else{
					reportApiAvail(false);
				}
			}
			
			
			WeatherLocation loc = mDataManager.getCurrentLocation();
			if (loc != null) {
				mDataManager.setLastCity(loc.city);
				mDataManager.setLastState(loc.state);
			}
		}
	}
}
