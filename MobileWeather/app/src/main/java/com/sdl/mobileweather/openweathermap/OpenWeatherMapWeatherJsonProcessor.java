package com.sdl.mobileweather.openweathermap;

import android.net.Uri;
import android.util.Log;

import com.sdl.mobileweather.weather.Forecast;
import com.sdl.mobileweather.weather.WeatherAlert;
import com.sdl.mobileweather.weather.WeatherConditions;
import com.sdl.mobileweather.weather.WeatherJsonProcessor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;

//TODO - Convert WUnderground specific code to OpenWeatherMap specific code
public class OpenWeatherMapWeatherJsonProcessor implements WeatherJsonProcessor {
	private static class Constants {
		public static final String TAG = "MobileWeather";
		private static final String CURRENT = "current";
		private static final String ICON_URL = "icon_url";
		private static final String WEATHER = "weather";
		private static final String TEMP = "temp";
		private static final String HUMIDITY = "humidity";
		private static final String WIND_SPEED = "wind_speed";
		private static final String WIND_GUST = "wind_gust";
		private static final String VISIBILITY = "visibility";
		private static final String HEAT_INDEX_C = "heat_index_c";
		private static final String WINDCHILL_C = "windchill_c";
		private static final String RAIN = "rain";
		private static final String SNOW = "snow";
		private static final String TIME_1_HR = "1h";
		private static final String DAILY_FORECAST = "daily";
		private static final String HOURLY_FORECAST = "hourly";
		private static final String FORECASTDAY = "forecastday";
		private static final String SIMPLEFORECAST = "simpleforecast";
		private static final String DATE = "dt";
		private static final String EPOCH = "epoch";
		private static final String HIGH = "high";
		private static final String CELSIUS = "celsius";
		private static final String LOW = "low";
		private static final String CONDITIONS = "conditions";
		private static final String QPF_ALLDAY = "qpf_allday";
		private static final String MM = "mm";
		private static final String SNOW_ALLDAY = "snow_allday";
		private static final String AVEHUMIDITY = "avehumidity";
		private static final String AVEWIND = "avewind";
		private static final String KPH = "kph";
		private static final String CM = "cm";
		private static final String POP = "pop";
		private static final String URI_SCHEME = "https";
		private static final String BASE_URL = "openweathermap.org";
		private static final String IMAGE_PATH = "img";
		private static final String WN_PATH = "wn";
		private static final String ICON_FILE_FORMAT = "%s@2x.png";
		private static final String ICON_PATH = "icon";
		private static final String MAIN = "main";
		private static final int ONLY_INDEX = 0;
		private static final String ALERTS = "alerts";
		private static final String EVENT = "event";
		private static final String DESCRIPTION = "description";
		private static final String START_TIME = "start";
		private static final String END_TIME = "end";
		private static final String PRECIPITATION_CHANCE = "pop";
		private static final float METERS_PER_SEC_TO_KPH = 3.6F;
		private static final String MIN = "min";
		private static final String MAX = "max";
		private static final String DAY = "day";
		private static final String FEELS_LIKE = "feels_like";
	}

	private JSONArray getForecastArray(JSONObject jsonRoot) {
		JSONArray forecastObj = null;

		try {
			forecastObj = jsonRoot.getJSONArray(Constants.DAILY_FORECAST);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return forecastObj;
	}

	private JSONArray getHourForecastArray(JSONObject jsonRoot) {
		JSONArray forecastObj = null;

		try {
			forecastObj = jsonRoot.getJSONArray(Constants.HOURLY_FORECAST);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return forecastObj;
	}

	private void tryToSetForecastDate(Forecast forecast, JSONObject forecastObj) {
		try {
			Calendar date = Calendar.getInstance();
			date.setTimeInMillis(forecastObj.getLong(Constants.DATE));
			forecast.date = date;
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetForecastHumidity(Forecast forecast, JSONObject forecastObj) {
		try {
			forecast.humidity = Float.valueOf(forecastObj.getString(Constants.HUMIDITY));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetDayForecastPrecipitation(Forecast forecast, JSONObject forecastObj) {
		forecast.precipitation = 0.0F;
		try {
			forecast.precipitation += (float) forecastObj.getDouble(Constants.RAIN);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			forecast.precipitation += (float) forecastObj.getDouble(Constants.SNOW);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetHourForecastPrecipitation(Forecast forecast, JSONObject forecastObj) {
		forecast.precipitation = 0.0F;
		try {
			forecast.precipitation += (float) forecastObj.getJSONObject(Constants.RAIN).getDouble(Constants.TIME_1_HR);
			//forecast.precipitation += (float) forecastObj.getDouble(Constants.RAIN);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		try {
			forecast.precipitation += (float) forecastObj.getJSONObject(Constants.SNOW).getDouble(Constants.TIME_1_HR);
			//forecast.precipitation += (float) forecastObj.getDouble(Constants.SNOW);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetDayForecastSnow(Forecast forecast, JSONObject forecastObj) {
		try {
			forecast.precipitation = (float) forecastObj.getDouble(Constants.SNOW);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetHourForecastSnow(Forecast forecast, JSONObject forecastObj) {
		try {
			forecast.precipitation = (float) forecastObj.getJSONObject(Constants.SNOW).getDouble(Constants.TIME_1_HR);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetForecastPrecipitationChance(Forecast forecast, JSONObject forecastObj) {
		try {
			//The API returns the probability as a float between 0 and 1, therefore it must be mapped to an integer between 0 and 100
			forecast.precipitationChance = (int)(forecastObj.getDouble(Constants.PRECIPITATION_CHANCE) * 100.0);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetForecastWindSpeed(Forecast forecast, JSONObject forecastObj) {
		try {
			forecast.windSpeed = (float) forecastObj.getDouble(Constants.WIND_SPEED) * Constants.METERS_PER_SEC_TO_KPH;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void tryToSetForecastHighTemperature(Forecast forecast, JSONObject forecastObj) {
		try {
			forecast.highTemperature = (float) forecastObj.getJSONObject(Constants.TEMP).getDouble(Constants.MAX);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetForecastLowTemperature(Forecast forecast, JSONObject forecastObj) {
		try {
			forecast.lowTemperature = (float) forecastObj.getJSONObject(Constants.TEMP).getDouble(Constants.MIN);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetForecastDayTemperature(Forecast forecast, JSONObject forecastObj) {
		try {
			forecast.temperature = (float) forecastObj.getJSONObject(Constants.TEMP).getDouble(Constants.DAY);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetForecastHourTemperature(Forecast forecast, JSONObject forecastObj) {
		try {
			forecast.temperature = (float) forecastObj.getDouble(Constants.TEMP);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetForecastIcon(Forecast forecast, JSONObject weather) {
		try {
			String iconId = weather.getString(Constants.ICON_PATH);

			forecast.conditionIcon = getWeatherIconURL(iconId);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void tryToSetForecastTitle(Forecast forecast, JSONObject weather) {
		try {
			forecast.conditionTitle = weather.getString(Constants.MAIN);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Forecast getSingleDayForecast(JSONArray forecastArray, int index) {
		Forecast singleDayForecast = null;
		JSONObject forecast = null;

		try {
			forecast = forecastArray.getJSONObject(index);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		if (forecast != null) {
			singleDayForecast = new Forecast();
			tryToSetForecastDate(singleDayForecast, forecast);
			tryToSetForecastHumidity(singleDayForecast, forecast);
			tryToSetDayForecastPrecipitation(singleDayForecast, forecast);
			tryToSetDayForecastSnow(singleDayForecast, forecast);
			tryToSetForecastPrecipitationChance(singleDayForecast, forecast);
			tryToSetForecastWindSpeed(singleDayForecast, forecast);
			tryToSetForecastHighTemperature(singleDayForecast, forecast);
			tryToSetForecastLowTemperature(singleDayForecast, forecast);
			tryToSetForecastDayTemperature(singleDayForecast, forecast);
			JSONObject weather = getWeather(forecast);
			if (weather != null) {
				tryToSetForecastIcon(singleDayForecast, weather);
				tryToSetForecastTitle(singleDayForecast, weather);
			}
		}

		return singleDayForecast;
	}

	@Override
	public Forecast[] getForecast(JSONObject jsonRoot) {
		Forecast[] forecasts = null;
		JSONArray forecastArray = getForecastArray(jsonRoot);

		if (forecastArray != null) {
			int forecastLength = forecastArray.length();
			forecasts = new Forecast[forecastLength];
			for (int i = 0; i < forecastLength; i++) {
				forecasts[i] = getSingleDayForecast(forecastArray, i);
			}
		}

		return forecasts;
	}

	private Forecast getSingleHourForecast(JSONArray forecastArray, int index) {
		Forecast singleHourForecast = null;
		JSONObject forecast = null;

		try {
			forecast = forecastArray.getJSONObject(index);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		if (forecast != null) {
			singleHourForecast = new Forecast();
			tryToSetForecastDate(singleHourForecast, forecast);
			tryToSetForecastHumidity(singleHourForecast, forecast);
			tryToSetHourForecastPrecipitation(singleHourForecast, forecast);
			tryToSetHourForecastSnow(singleHourForecast, forecast);
			tryToSetForecastPrecipitationChance(singleHourForecast, forecast);
			tryToSetForecastWindSpeed(singleHourForecast, forecast);
			//tryToSetForecastHighTemperature(singleHourForecast, forecast);
			//tryToSetForecastLowTemperature(singleHourForecast, forecast);
			tryToSetForecastHourTemperature(singleHourForecast, forecast);
			JSONObject weather = getWeather(forecast);
			if (weather != null) {
				tryToSetForecastIcon(singleHourForecast, weather);
				tryToSetForecastTitle(singleHourForecast, weather);
			}
		}

		return singleHourForecast;
	}

	@Override
	public Forecast[] getHourlyForecast(JSONObject jsonRoot) {
		Forecast[] forecasts = null;
		JSONArray forecastArray = getHourForecastArray(jsonRoot);

		if (forecastArray != null) {
			int forecastLength = forecastArray.length();
			forecasts = new Forecast[forecastLength];
			for (int i = 0; i < forecastLength; i++) {
				forecasts[i] = getSingleHourForecast(forecastArray, i);
			}
		}

		return forecasts;
	}

	private JSONObject getCurrent(JSONObject conditions) {
		JSONObject current = null;
		try {
			current = conditions.getJSONObject(Constants.CURRENT);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return current;
	}
	private JSONObject getWeather(JSONObject conditionAtTime) {
		JSONObject weather = null;
		try {
			weather = conditionAtTime.getJSONArray(Constants.WEATHER).getJSONObject(Constants.ONLY_INDEX);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return weather;
	}
	private URL getWeatherIconURL(String iconId) throws MalformedURLException {
		String filename = String.format(Constants.ICON_FILE_FORMAT, iconId);

		Uri.Builder builder = new Uri.Builder();
		builder.scheme(Constants.URI_SCHEME)
				.authority(Constants.BASE_URL)
				.appendPath(Constants.IMAGE_PATH)
				.appendPath(Constants.WN_PATH)
				.appendPath(filename);

		String uriString = builder.build().toString();
		URL url = new URL(uriString);
		String urlString = url.toString();
		Log.d(Constants.TAG, urlString);
		return url;
	}
	private void tryToSetConditionIconFromCurrent(WeatherConditions weatherConditions, JSONObject weather) {
		try {
			String iconId = weather.getString(Constants.ICON_PATH);

			weatherConditions.conditionIcon = getWeatherIconURL(iconId);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void tryToSetConditionTitleFromCurrent(WeatherConditions weatherConditions, JSONObject weather) {
		try {
			weatherConditions.conditionTitle = weather.getString(Constants.MAIN);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void tryToSetConditionTemperatureFromCurrent(WeatherConditions weatherConditions, JSONObject current) {
		try {
			weatherConditions.temperature = (float) current.getDouble(Constants.TEMP);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void tryToSetConditionHumidityFromCurrent(WeatherConditions weatherConditions, JSONObject current) {
		try {
			weatherConditions.humidity = Float.valueOf(current.getString(Constants.HUMIDITY));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void tryToSetConditionWindSpeedFromCurrent(WeatherConditions weatherConditions, JSONObject current) {
		try {
			weatherConditions.windSpeed = (float) current.getDouble(Constants.WIND_SPEED) * Constants.METERS_PER_SEC_TO_KPH;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void tryToSetConditionWindSpeedGustFromCurrent(WeatherConditions weatherConditions, JSONObject current) {
		try {
			weatherConditions.windSpeedGust = (float) current.getDouble(Constants.WIND_GUST) * Constants.METERS_PER_SEC_TO_KPH;
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void tryToSetConditionVisibilityFromCurrent(WeatherConditions weatherConditions, JSONObject current) {
		try {
			weatherConditions.visibility = (float) current.getDouble(Constants.VISIBILITY);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void tryToSetConditionFeelsLikeTempFromCurrent(WeatherConditions weatherConditions, JSONObject current) {
		try {
			weatherConditions.feelsLikeTemperature = (float) current.getDouble(Constants.FEELS_LIKE);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void tryToSetConditionPrecipitationFromCurrent(WeatherConditions weatherConditions, JSONObject current) {
		weatherConditions.precipitation = 0.0F;
		try {
			weatherConditions.precipitation += (float) current.getJSONObject(Constants.RAIN).getDouble(Constants.TIME_1_HR);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			weatherConditions.precipitation += (float) current.getJSONObject(Constants.SNOW).getDouble(Constants.TIME_1_HR);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public WeatherConditions getWeatherConditions(JSONObject jsonRoot) {
		WeatherConditions weatherConditions = null;
		if (jsonRoot != null) {
			weatherConditions = new WeatherConditions();
			JSONObject current = getCurrent(jsonRoot);
			// Parse JSON
			if (current != null) {
				JSONObject weather = getWeather(current);
				if (weather != null) {
					tryToSetConditionIconFromCurrent(weatherConditions, weather);
					tryToSetConditionTitleFromCurrent(weatherConditions, weather);
				}
				tryToSetConditionTemperatureFromCurrent(weatherConditions, current);
				tryToSetConditionHumidityFromCurrent(weatherConditions, current);
				tryToSetConditionWindSpeedFromCurrent(weatherConditions, current);
				tryToSetConditionWindSpeedGustFromCurrent(weatherConditions, current);
				tryToSetConditionVisibilityFromCurrent(weatherConditions, current);
				tryToSetConditionFeelsLikeTempFromCurrent(weatherConditions, current);
				tryToSetConditionPrecipitationFromCurrent(weatherConditions, current);
			}
		}
		
		return weatherConditions;
	}

	private JSONArray getAlertArray(JSONObject jsonRoot) {
		JSONArray alertArray = null;

		try {
			alertArray = jsonRoot.getJSONArray(Constants.ALERTS);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}

		return alertArray;
	}

	private void tryToSetAlertMessage(WeatherAlert weatherAlert, JSONObject alert) {
		try {
			weatherAlert.message = alert.getString(Constants.EVENT);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetAlertDescription(WeatherAlert weatherAlert, JSONObject alert) {
		try {
			weatherAlert.description = alert.getString(Constants.DESCRIPTION);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetAlertDateIssued(WeatherAlert weatherAlert, JSONObject alert) {
		try {
			Calendar dateIssued = Calendar.getInstance();
			dateIssued.setTimeInMillis(alert.getLong(Constants.START_TIME));
			weatherAlert.dateIssued = dateIssued;
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}
	private void tryToSetAlertDateExpires(WeatherAlert weatherAlert, JSONObject alert) {
		try {
			Calendar dateExpires = Calendar.getInstance();
			dateExpires.setTimeInMillis(alert.getLong(Constants.END_TIME));
			weatherAlert.dateExpires = dateExpires;
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private WeatherAlert getAlert(JSONArray alertArray, int index) {
		WeatherAlert weatherAlert = null;
		JSONObject alert = null;

		try {
			 alert = alertArray.getJSONObject(index);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if (alert != null) {
			weatherAlert = new WeatherAlert();
			tryToSetAlertMessage(weatherAlert, alert);
			tryToSetAlertDescription(weatherAlert, alert);
			tryToSetAlertDateIssued(weatherAlert, alert);
			tryToSetAlertDateExpires(weatherAlert, alert);
			/*TODO - Set alert type somehow
			 * This could be difficult because the current implementation uses enum types,
			 * but the API provides more verbose strings
			 */
		}

		return weatherAlert;
	}

	@Override
	public WeatherAlert[] getAlerts(JSONObject jsonRoot) {
		WeatherAlert[] alerts = null;
		JSONArray alertArray = getAlertArray(jsonRoot);
		if (alertArray != null) {
			int arrayLength = alertArray.length();
			alerts = new WeatherAlert[arrayLength];

			for (int i = 0; i < arrayLength; i++) {
				alerts[i] = getAlert(alertArray, i);
			}
		}

		return alerts;
	}
}
