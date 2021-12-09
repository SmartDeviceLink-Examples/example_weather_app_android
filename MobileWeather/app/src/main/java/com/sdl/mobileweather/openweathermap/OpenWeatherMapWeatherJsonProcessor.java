package com.sdl.mobileweather.openweathermap;

import static net.hockeyapp.android.Constants.BASE_URL;

import android.net.Uri;

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
import java.util.Vector;

//TODO - Convert WUnderground specific code to OpenWeatherMap specific code
public class OpenWeatherMapWeatherJsonProcessor implements WeatherJsonProcessor {
	private static class Constants {
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
		private static final String FORECAST = "forecast";
		private static final String FORECASTDAY = "forecastday";
		private static final String SIMPLEFORECAST = "simpleforecast";
		private static final String DATE = "date";
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
	}


	@Override
	public Forecast[] getForecast(JSONObject jsonRoot) {
		Vector<Forecast> forecastVector = new Vector<Forecast>();
		JSONArray forecastDays = null;
		JSONObject simpleForecast = null;
		JSONObject forecastObj = null;

		try {
			forecastObj = jsonRoot.getJSONObject(Constants.FORECAST);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (forecastObj != null) {
			try {
				simpleForecast = forecastObj.getJSONObject(Constants.SIMPLEFORECAST);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (simpleForecast != null) {
				try {
					forecastDays = simpleForecast.getJSONArray(Constants.FORECASTDAY);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (forecastDays != null) {
					int numberOfDays = forecastDays.length();
					for (int dayCounter = 0; dayCounter < numberOfDays; dayCounter++) {
						JSONObject day = null;
						try {
							day = forecastDays.getJSONObject(dayCounter);
						} catch (JSONException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						Forecast currentForecast = new Forecast();
						if (day != null && currentForecast != null) {
							JSONObject date = null;
							try {
								date = day.getJSONObject(Constants.DATE);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (date != null) {
								String epoch = null;
								try {
									epoch = date.getString(Constants.EPOCH);
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								if (epoch != null) {
									long epochLong = Long.parseLong(epoch, 10);
									Calendar forecastDate = Calendar.getInstance();
									forecastDate.setTimeInMillis(epochLong);
									currentForecast.date = forecastDate;
								}										
							}
						
							JSONObject high = null;
							try {
								high = day.getJSONObject(Constants.HIGH);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (high != null) {
								try {
									currentForecast.highTemperature = Float.valueOf(high.getInt(Constants.CELSIUS));
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}										
							}

							JSONObject low = null;
							try {
								low = day.getJSONObject(Constants.LOW);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (low != null) {
								try {
									currentForecast.lowTemperature = Float.valueOf(low.getInt(Constants.CELSIUS));
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}										
							}
							
							try {
								currentForecast.precipitationChance = Integer.valueOf(day.getInt(Constants.POP));
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}										

							try {
								currentForecast.conditionTitle = day.getString(Constants.CONDITIONS);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							try {
								currentForecast.conditionIcon = new URL(day.getString(Constants.ICON_URL));
							} catch (MalformedURLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							JSONObject qpf = null;
							try {
								qpf = day.getJSONObject(Constants.QPF_ALLDAY);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (qpf != null) {
								try {
									currentForecast.precipitation = Float.valueOf((float) qpf.getDouble(Constants.MM));
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}										
							}
							
							JSONObject snow = null;
							try {
								snow = day.getJSONObject(Constants.SNOW_ALLDAY);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (snow != null) {
								try {
									currentForecast.snow = Float.valueOf((float) snow.getDouble(Constants.CM));
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}										
							}
							
							try {
								currentForecast.humidity = Float.valueOf((float) day.getDouble(Constants.AVEHUMIDITY));
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}										
							
							JSONObject wind = null;
							try {
								wind = day.getJSONObject(Constants.AVEWIND);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (wind != null) {
								try {
									currentForecast.windSpeed = Float.valueOf((float) wind.getDouble(Constants.KPH));
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}										
							}
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
	public Forecast[] getHourlyForecast(JSONObject jsonRoot) {
		// TODO Auto-generated method stub
		return null;
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
				.authority(BASE_URL)
				.appendPath(Constants.IMAGE_PATH)
				.appendPath(Constants.WN_PATH)
				.appendPath(filename);

		String uriString = builder.build().toString();

		return new URL(uriString);
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
			weatherConditions.windSpeed = (float) current.getDouble(Constants.WIND_SPEED);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void tryToSetConditionWindSpeedGustFromCurrent(WeatherConditions weatherConditions, JSONObject current) {
		try {
			weatherConditions.windSpeedGust = (float) current.getDouble(Constants.WIND_GUST);
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
			weatherConditions.feelsLikeTemperature = (float) current.getDouble(Constants.HEAT_INDEX_C);
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
			//TODO - set alert type somehow
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
