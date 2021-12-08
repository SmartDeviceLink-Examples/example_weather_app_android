package com.sdl.mobileweather.openweathermap;

import com.sdl.mobileweather.artifact.GPSLocation;
import com.sdl.mobileweather.artifact.WeatherLocation;
import com.sdl.mobileweather.location.LocationJsonProcessor;

import org.json.JSONException;
import org.json.JSONObject;

//TODO - Convert WUnderground specific code to OpenWeatherMap specific code
public class OpenWeatherMapLocationJsonProcessor implements LocationJsonProcessor {
	private static final String LOCATION = "location";
	private static final String STATE = "state";
	private static final String CITY = "city";
	private static final String LATITUDE = "lat";
	private static final String LONGITUDE = "lon";
	private static final String ZIPCODE = "zip";

	@Override
	public WeatherLocation getLocation(JSONObject jsonRoot) {
		WeatherLocation location = null;
		
		if (jsonRoot != null) {
			location = new WeatherLocation();
			try {
				JSONObject jsonLocation = jsonRoot.getJSONObject(LOCATION);
				location.state = jsonLocation.getString(STATE);
				location.city = jsonLocation.getString(CITY);
				location.zipCode = jsonLocation.getString(ZIPCODE);
				location.gpsLocation = new GPSLocation();
				location.gpsLocation.latitude = jsonLocation.getString(LATITUDE);
				location.gpsLocation.longitude = jsonLocation.getString(LONGITUDE);					
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return location;
	}
}
