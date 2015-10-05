package com.ford.mobileweather.location;

import org.json.JSONObject;

import com.ford.mobileweather.artifact.WeatherLocation;

public interface LocationJsonProcessor {

	public WeatherLocation getLocation(JSONObject location);
}
