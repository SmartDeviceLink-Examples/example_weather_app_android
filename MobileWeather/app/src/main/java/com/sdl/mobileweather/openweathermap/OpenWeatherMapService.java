package com.sdl.mobileweather.openweathermap;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.sdl.mobileweather.artifact.GPSLocation;
import com.sdl.mobileweather.artifact.WeatherLocation;
import com.sdl.mobileweather.connection.HttpConnection;
import com.sdl.mobileweather.processor.JsonProcessor;
import com.sdl.mobileweather.smartdevicelink.SdlApplication;
import com.sdl.mobileweather.weather.Forecast;
import com.sdl.mobileweather.weather.WeatherAlert;
import com.sdl.mobileweather.weather.WeatherConditions;
import com.sdl.mobileweather.weather.WeatherService;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;

//TODO - Convert WUnderground and ForecastIo specific code to OpenWeatherMap specific code
public class OpenWeatherMapService extends WeatherService {

	/**
	 * API key used for OpenWeatherMap API
	 * <p/>
	 * I acquired this free one for testing. We may want to use a different one for broader use.
	 * <p/>
	 * -Noah Stanford
	 */
	private static final String API_KEY = "e81bf4160a279541cdaff8a8fc4cdda1";
	private static final String URI_SCHEME = "https";
	/**
	 * Base request URL for OpenWeatherMap
	 */
	private static final String BASE_URL = "api.openweathermap.org";
	private static final String DATA_PATH = "data";
	/**
	 * A number value in the suggested API call to OpenWeatherMap
	 * <p/>
	 * I believe this value in the API call represents the version number of the API.
	 * It may need to change if OpenWeatherMap updates their API
	 * <p/>
	 * - Noah Stanford (noah@livio.io)
	 */
	private static final String VERSION = "2.5";
	/**
	 * This API call type provides current weather, minute forecast for 1 hour, hourly forecast for 48 hours,
	 * daily forecast for 7 days, global weather alerts and historical data for 5 previous day for any location
	 */
	private static final String ONE_CALL_API_CALL_TYPE = "onecall";
	private static final String LATITUDE_PARAMETER = "lat";
	private static final String LONGITUDE_PARAMETER = "lon";
	private static final String UNITS_PARAMETER = "units";
	private static final String METRIC = "metric";
	/**
	 * This parameter is used to specify the API key for the API call
	 */
	private static final String APP_ID_PARAMETER = "appid";
	/**
	 * This is 0 because it's the first index and only one URL is used for weather data at a time
	 */
	private static final int ONLY_URL_INDEX = 0;
	public static final int REQUEST_SUCCESS = -1;
	public static final int REQUEST_FAILURE = -2;
	public static final String TAG = "MobileWeather";


	public OpenWeatherMapService() {
		super();
		mWeatherProcessor = new OpenWeatherMapWeatherJsonProcessor();
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

		Log.d(TAG, "updateWeatherData");
		if (urls.length == 1 && mDataManager != null && mWeatherProcessor != null) {
			LocalBroadcastManager lbManager = LocalBroadcastManager.getInstance(this);
			Log.d(TAG, "updateWeatherData - valid urls");
			if (urls[ONLY_URL_INDEX] != null) {
				Log.d(TAG, urls[ONLY_URL_INDEX].toString());
				Log.d(TAG, "updateWeatherData - getting json");
				httpResult = httpRequest.sendRequest(urls[ONLY_URL_INDEX], HttpConnection.RequestMethod.GET, null, "application/json");
				int statusCode = httpRequest.getStatusCode(httpResult);
				if (statusCode == REQUEST_SUCCESS) {
					Log.d(TAG, "updateWeatherData - parsing conditions json");
					JSONObject jsonRoot = jsonProcessor.getJsonFromString(httpResult);
					if (jsonRoot != null) {
						Log.d(TAG, "updateWeatherData - parsing conditions");
						conditions = mWeatherProcessor.getWeatherConditions(jsonRoot);
						mDataManager.setWeatherConditions(conditions);
						if (conditions != null) {
							Log.d(TAG, "updateWeatherData - new conditions");
							Intent intent = new Intent("com.sdl.mobileweather.WeatherConditions");
							lbManager.sendBroadcast(intent);
						}

						Log.d(TAG, "updateWeatherData - parsing forecast");
						forecast = mWeatherProcessor.getForecast(jsonRoot);
						mDataManager.setForecast(forecast);
						if (forecast != null) {
							Log.d(TAG, "updateWeatherData - new forecast");
							Intent intent = new Intent("com.sdl.mobileweather.Forecast");
							lbManager.sendBroadcast(intent);
						}

						Log.d(TAG, "updateWeatherData - parsing hourly forecast");
						hourlyForecast = mWeatherProcessor.getHourlyForecast(jsonRoot);
						mDataManager.setHourlyForecast(hourlyForecast);
						if (hourlyForecast != null) {
							Intent intent = new Intent("com.sdl.mobileweather.HourlyForecast");
							lbManager.sendBroadcast(intent);
						}

						alerts = mWeatherProcessor.getAlerts(jsonRoot);
						mDataManager.setAlerts(alerts);
						if (alerts != null) {
							Log.d(TAG, "updateWeatherData - new Alerts");
							Intent intent = new Intent("com.sdl.mobileweather.Alerts");
							lbManager.sendBroadcast(intent);
						}
					}

					reportApiAvail(true);

				} else if (statusCode == REQUEST_FAILURE){
					reportConnectionAvail(false);
				} else {
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

	/**
	 * Creates service specific weather data URLs.
	 */
	@Override
	protected URL[] getURLs(WeatherLocation currentLocation) {
		URL oneCallURL = null;
		try {
			oneCallURL = buildOneCallURL(currentLocation);
			Log.d(SdlApplication.TAG, currentLocation.gpsLocation.latitude + "," + currentLocation.gpsLocation.longitude);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return new URL[] { oneCallURL };
	}

	private URL buildOneCallURL(WeatherLocation currentLocation) throws MalformedURLException {
		Uri.Builder builder = new Uri.Builder();
		builder.scheme(URI_SCHEME)
				.authority(BASE_URL)
				.appendPath(DATA_PATH)
				.appendPath(VERSION)
				.appendPath(ONE_CALL_API_CALL_TYPE)
				.appendQueryParameter(LATITUDE_PARAMETER, currentLocation.gpsLocation.latitude)
				.appendQueryParameter(LONGITUDE_PARAMETER, currentLocation.gpsLocation.longitude)
				.appendQueryParameter(UNITS_PARAMETER, METRIC)
				.appendQueryParameter(APP_ID_PARAMETER, API_KEY);

		Uri everythingUri = builder.build();
		return new URL(everythingUri.toString());
	}

	public int getRequestStatus() {
		String httpResult = null;
		HttpConnection httpRequest = new HttpConnection();
		WeatherLocation location = new WeatherLocation();
		location.gpsLocation = new GPSLocation();
		location.gpsLocation.latitude = "0.0";
		location.gpsLocation.longitude = "0.0";

		URL[] urls = getURLs(location);
		httpResult = httpRequest.sendRequest(urls[ONLY_URL_INDEX], HttpConnection.RequestMethod.GET, null, "application/json");
		return httpRequest.getStatusCode(httpResult);
	}
}
