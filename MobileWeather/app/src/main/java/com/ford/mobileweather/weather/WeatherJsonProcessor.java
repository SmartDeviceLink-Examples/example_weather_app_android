package com.ford.mobileweather.weather;

import org.json.JSONObject;

public interface WeatherJsonProcessor {

	public Forecast[] getForecast(JSONObject forecastJson);
	public Forecast[] getHourlyForecast(JSONObject forecastJson);
	public WeatherConditions getWeatherConditions(JSONObject conditionsJson);
	public WeatherAlert[] getAlerts(JSONObject alertsJson);

}
