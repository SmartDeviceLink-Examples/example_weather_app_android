package com.ford.mobileweather.forecastio;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.ford.mobileweather.smartdevicelink.SmartDeviceLinkApplication;
import com.ford.mobileweather.weather.Forecast;
import com.ford.mobileweather.weather.UnitConverter;
import com.ford.mobileweather.weather.WeatherAlert;
import com.ford.mobileweather.weather.WeatherConditions;
import com.ford.mobileweather.weather.WeatherJsonProcessor;

public class ForecastIoWeatherJsonProcessor implements WeatherJsonProcessor {
	
	private static final String CURRENTLY = "currently";
	private static final String HOURLY = "hourly";
	private static final String DAILY = "daily";
	private static final String ALERTS = "alerts";
	private static final String ICON = "icon";
	private static final String SUMMARY = "summary";
	private static final String TEMPERATURE = "temperature";
	private static final String HUMIDITY = "humidity";
	private static final String WIND_SPEED = "windSpeed";
	private static final String VISIBILITY = "visibility";
	private static final String APPARENT_TEMPERATURE = "apparentTemperature";
	private static final String PRECIP_PROBABILITY = "precipProbability";
	private static final String DATA = "data";
	private static final String TIME = "time";
	private static final String TEMPERATURE_MAX = "temperatureMax";
	private static final String TEMPERATURE_MIN = "temperatureMin";
	private static final String PRECIP_ACCUMULATION = "precipAccumulation";
	private static final String EXPIRES = "expires";
	private static final String TITLE = "title";
	private static final String DESCRIPTION = "description";
	
	private Forecast[] processForecast(JSONObject forecastJson, String type) {
		Vector<Forecast> forecastVector = new Vector<Forecast>();
		JSONArray data = null;
		JSONObject section = null;

		try {
			section = forecastJson.getJSONObject(type);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (section != null) {
			try {
				data = section.getJSONArray(DATA);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (data != null) {
				int numberOfDays = data.length();
				for (int dayCounter = 0; dayCounter < numberOfDays; dayCounter++) {
					JSONObject day = null;
					try {
						day = data.getJSONObject(dayCounter);
					} catch (JSONException e1) {
						Log.v(SmartDeviceLinkApplication.TAG, "No data in JSONArray for day " + dayCounter);
					}

					Forecast currentForecast = new Forecast();
					if (day != null && currentForecast != null) {
						long time = 0;
						try {
							time = day.getLong(TIME);
						} catch (JSONException e) {
							Log.v(SmartDeviceLinkApplication.TAG, "No TIME in JSON for day " + dayCounter);
							e.printStackTrace();
						}
						if (time != 0) {
							Calendar forecastDate = Calendar.getInstance();
							forecastDate.setTimeInMillis(time);
							currentForecast.date = forecastDate;
						}
						
						try {
							currentForecast.conditionTitle = day.getString(SUMMARY);
						} catch (JSONException e) {
							Log.v(SmartDeviceLinkApplication.TAG, "No SUMMARY in JSON for day " + dayCounter);
						}

						try {
							currentForecast.conditionIcon = new URL("http://localhost/" + day.getString(ICON) + ".gif");
						} catch (MalformedURLException e) {
							Log.v(SmartDeviceLinkApplication.TAG, "Bad icon URL for day " + dayCounter);
						} catch (JSONException e) {
							Log.v(SmartDeviceLinkApplication.TAG, "Unable to get condition icon for JSON for day " + dayCounter);
						}

						try {
							currentForecast.temperature = UnitConverter.convertTemperatureToMetric(Float.valueOf((float) day.getDouble(TEMPERATURE)));
						} catch (JSONException e) {
							Log.v(SmartDeviceLinkApplication.TAG, "No TEMPERTATURE in JSON for day " + dayCounter);
						}

						try {
							currentForecast.highTemperature = UnitConverter.convertTemperatureToMetric(Float.valueOf((float) day.getDouble(TEMPERATURE_MAX)));
						} catch (JSONException e) {
							Log.v(SmartDeviceLinkApplication.TAG, "No TEMPERATURE_MAX in JSON for day " + dayCounter);
						}

						try {
							currentForecast.lowTemperature = UnitConverter.convertTemperatureToMetric(Float.valueOf((float) day.getDouble(TEMPERATURE_MIN)));
						} catch (JSONException e) {
							Log.v(SmartDeviceLinkApplication.TAG, "No TEMPERATURE_MIN in JSON for day " + dayCounter);
						}
						
						try {
							currentForecast.humidity = Float.valueOf((float) day.getDouble(HUMIDITY)) * 100.0f;
						} catch (JSONException e) {
							Log.v(SmartDeviceLinkApplication.TAG, "No HUMIDITY in JSON for day " + dayCounter);
						}
						
						try {
							currentForecast.windSpeed = UnitConverter.convertSpeedToMetric(Float.valueOf((float) day.getDouble(WIND_SPEED)));
						} catch (JSONException e) {
							Log.v(SmartDeviceLinkApplication.TAG, "No WIND_SPEED in JSON for day " + dayCounter);
						}
						
						try {
							currentForecast.precipitationChance = (int) (Float.valueOf((float) day.getDouble(PRECIP_PROBABILITY)) * 100.0);
						} catch (JSONException e) {
							Log.v(SmartDeviceLinkApplication.TAG, "No PERCIP_PROBABILITY in JSON for day " + dayCounter);
						}
						
						try {
							currentForecast.snow = UnitConverter.convertLengthToMetric(Float.valueOf((float) day.getDouble(PRECIP_ACCUMULATION)));
						} catch (JSONException e) {
							Log.v(SmartDeviceLinkApplication.TAG, "No PERCIP_ACCUMULATION in JSON for day " + dayCounter);
						}

						forecastVector.add(currentForecast);
					}
				}
			}
		}
		if (forecastVector.size() > 0) {
			Forecast[] forecastArray = forecastVector.toArray(new Forecast[forecastVector.size()]);
			return forecastArray;
		}
		else {
			return null;
		}
	}
	
	@Override
	public Forecast[] getForecast(JSONObject forecastJson) {
		return processForecast(forecastJson, DAILY);
	}

	@Override
	public Forecast[] getHourlyForecast(JSONObject forecastJson) {
		return processForecast(forecastJson, HOURLY);
	}

	@Override
	public WeatherConditions getWeatherConditions(JSONObject conditions) {
		WeatherConditions weatherConditions = null;
		if (conditions != null) {
			weatherConditions = new WeatherConditions();
			JSONObject currently = null;
			// Parse JSON
			// Individual try/catch blocks used such that one failure will not abort the whole thing
			try {
				currently = conditions.getJSONObject(CURRENTLY);
			} catch (JSONException e) {
				Log.v(SmartDeviceLinkApplication.TAG, "No JSONObject available for CURRENTLY (Current conditions)");
			}
			if (currently != null) {
				// Parse JSON
				// Individual try/catch blocks used such that one failure will not abort the whole thing
				try {
					weatherConditions.conditionIcon = new URL("http://localhost/" + currently.getString(ICON) + ".gif");
				} catch (MalformedURLException e) {
					Log.d(SmartDeviceLinkApplication.TAG, "Bad icon URL for currently");
				} catch (JSONException e) {
					Log.d(SmartDeviceLinkApplication.TAG, "Unable to get condition icon for JSON for currently");
				}
				
				try {
					weatherConditions.conditionTitle = currently.getString(SUMMARY);
				} catch (JSONException e) {
					Log.v(SmartDeviceLinkApplication.TAG, "No SUMMARY in JSON for currently.");
				}
				
				try {
					weatherConditions.temperature = UnitConverter.convertTemperatureToMetric(Float.valueOf((float) currently.getDouble(TEMPERATURE)));
				} catch (JSONException e) {
					Log.v(SmartDeviceLinkApplication.TAG, "No TEMPERATURE in JSON for currently.");
				}
				
				try {
					weatherConditions.humidity = Float.valueOf((float) currently.getDouble(HUMIDITY)) * 100.0f;
				} catch (JSONException e) {
					Log.v(SmartDeviceLinkApplication.TAG, "No HUMIDITY in JSON for currently.");
				}
				
				try {
					weatherConditions.windSpeed = UnitConverter.convertSpeedToMetric(Float.valueOf((float) currently.getDouble(WIND_SPEED)));
				} catch (JSONException e) {
					Log.v(SmartDeviceLinkApplication.TAG, "No WIND_SPEED in JSON for currently.");
				}
				
				try {
					weatherConditions.visibility = UnitConverter.convertLengthToMetric(Float.valueOf((float) currently.getDouble(VISIBILITY)));
				} catch (JSONException e) {
					Log.v(SmartDeviceLinkApplication.TAG, "No VISIBILITY in JSON for currently.");
				}
				
				try {
					weatherConditions.feelsLikeTemperature = UnitConverter.convertTemperatureToMetric(Float.valueOf((float) currently.getDouble(APPARENT_TEMPERATURE)));
				} catch (JSONException e) {
					Log.v(SmartDeviceLinkApplication.TAG, "No APPARENT_TEMPERATURE in JSON for currently.");
				}
				
				try {
					weatherConditions.precipitation = Float.valueOf((float) currently.getDouble(PRECIP_PROBABILITY)) * 100.0f;
				} catch (JSONException e) {
					Log.v(SmartDeviceLinkApplication.TAG, "No PRECIP_PROBABILITY in JSON for currently.");
				}
			}
		}
		
		return weatherConditions;
	}

	@Override
	public WeatherAlert[] getAlerts(JSONObject alertsJson) {
		Vector<WeatherAlert> alertVector = new Vector<WeatherAlert>();
		JSONArray data = null;

		try {
			data = alertsJson.getJSONArray(ALERTS);
		} catch (JSONException e) {
			Log.d(SmartDeviceLinkApplication.TAG, "No ALERTS JSONArray available.");
		}
		if (data != null) {
			int numberOfAlerts = data.length();
			for (int alertCounter = 0; alertCounter < numberOfAlerts; alertCounter++) {
				JSONObject alert = null;
				try {
					alert = data.getJSONObject(alertCounter);
				} catch (JSONException e1) {
					Log.d(SmartDeviceLinkApplication.TAG, "No JSON available for alert " + alertCounter);
				}

				WeatherAlert currentAlert = new WeatherAlert();
				if (alert != null && currentAlert != null) {
					long time = 0;
					try {
						time = alert.getLong(EXPIRES);
					} catch (JSONException e) {
						Log.v(SmartDeviceLinkApplication.TAG, "No EXPIRES available for alert " + alertCounter);
					}
					if (time != 0) {
						Calendar expiresDate = Calendar.getInstance();
						expiresDate.setTimeInMillis(time);
						currentAlert.dateExpires = expiresDate;
					}
					try {
						currentAlert.message = alert.getString(TITLE);
					} catch (JSONException e) {
						Log.v(SmartDeviceLinkApplication.TAG, "No TITLE available for alert " + alertCounter);
					}
					try {
						currentAlert.description = alert.getString(DESCRIPTION);
					} catch (JSONException e) {
						Log.v(SmartDeviceLinkApplication.TAG, "No DESCRIPTION available for alert " + alertCounter);
					}

					alertVector.add(currentAlert);
				}
			}
		}
		if (alertVector.size() > 0) {
			WeatherAlert[] alertArray = alertVector.toArray(new WeatherAlert[alertVector.size()]);
			return alertArray;
		}
		else {
			return null;
		}
	}
}
