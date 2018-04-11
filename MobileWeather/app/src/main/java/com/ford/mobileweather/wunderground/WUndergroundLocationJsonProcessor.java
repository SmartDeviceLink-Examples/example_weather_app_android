package com.ford.mobileweather.wunderground;

import org.json.JSONException;
import org.json.JSONObject;

import com.ford.mobileweather.artifact.GPSLocation;
import com.ford.mobileweather.artifact.WeatherLocation;
import com.ford.mobileweather.location.LocationJsonProcessor;

public class WUndergroundLocationJsonProcessor implements LocationJsonProcessor {
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
