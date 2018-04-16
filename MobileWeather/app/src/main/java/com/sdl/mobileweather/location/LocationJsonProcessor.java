package com.sdl.mobileweather.location;

import org.json.JSONObject;

import com.sdl.mobileweather.artifact.WeatherLocation;

public interface LocationJsonProcessor {

	public WeatherLocation getLocation(JSONObject location);
}
