package com.ford.mobileweather.wunderground;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ford.mobileweather.weather.WeatherAlert;
import com.ford.mobileweather.weather.Forecast;
import com.ford.mobileweather.weather.WeatherConditions;
import com.ford.mobileweather.weather.WeatherJsonProcessor;

public class WUndergroundWeatherJsonProcessor implements WeatherJsonProcessor {	
	private static final String CURRENT_OBSERVATION = "current_observation";
	private static final String ICON_URL = "icon_url";
	private static final String WEATHER = "weather";
	private static final String TEMP_C = "temp_c";
	private static final String RELATIVE_HUMIDITY = "relative_humidity";
	private static final String WIND_KPH = "wind_kph";
	private static final String WIND_GUST_KPH = "wind_gust_kph";
	private static final String VISIBILITY_KM = "visibility_km";
	private static final String HEAT_INDEX_C = "heat_index_c";
	private static final String WINDCHILL_C = "windchill_c";
	private static final String PRECIP_1HR_METRIC = "precip_1hr_metric";
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
	
	@Override
	public Forecast[] getForecast(JSONObject forecastJson) {
		Vector<Forecast> forecastVector = new Vector<Forecast>();
		JSONArray forecastDays = null;
		JSONObject simpleForecast = null;
		JSONObject forecastObj = null;

		try {
			forecastObj = forecastJson.getJSONObject(FORECAST);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (forecastObj != null) {
			try {
				simpleForecast = forecastObj.getJSONObject(SIMPLEFORECAST);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (simpleForecast != null) {
				try {
					forecastDays = simpleForecast.getJSONArray(FORECASTDAY);
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
								date = day.getJSONObject(DATE);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (date != null) {
								String epoch = null;
								try {
									epoch = date.getString(EPOCH);
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
								high = day.getJSONObject(HIGH);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (high != null) {
								try {
									currentForecast.highTemperature = Float.valueOf(high.getInt(CELSIUS));
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}										
							}

							JSONObject low = null;
							try {
								low = day.getJSONObject(LOW);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (low != null) {
								try {
									currentForecast.lowTemperature = Float.valueOf(low.getInt(CELSIUS));
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}										
							}
							
							try {
								currentForecast.precipitationChance = Integer.valueOf(day.getInt(POP));
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}										

							try {
								currentForecast.conditionTitle = day.getString(CONDITIONS);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							try {
								currentForecast.conditionIcon = new URL(day.getString(ICON_URL));
							} catch (MalformedURLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							JSONObject qpf = null;
							try {
								qpf = day.getJSONObject(QPF_ALLDAY);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (qpf != null) {
								try {
									currentForecast.precipitation = Float.valueOf((float) qpf.getDouble(MM));
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}										
							}
							
							JSONObject snow = null;
							try {
								snow = day.getJSONObject(SNOW_ALLDAY);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (snow != null) {
								try {
									currentForecast.snow = Float.valueOf((float) snow.getDouble(CM));
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}										
							}
							
							try {
								currentForecast.humidity = Float.valueOf((float) day.getDouble(AVEHUMIDITY));
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}										
							
							JSONObject wind = null;
							try {
								wind = day.getJSONObject(AVEWIND);
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							if (wind != null) {
								try {
									currentForecast.windSpeed = Float.valueOf((float) wind.getDouble(KPH));
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
	public Forecast[] getHourlyForecast(JSONObject forecast) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public WeatherConditions getWeatherConditions(JSONObject conditions) {
		WeatherConditions weatherConditions = null;
		if (conditions != null) {
			weatherConditions = new WeatherConditions();
			JSONObject currentObservation = null;
			// Parse JSON
			// Individual try/catch blocks used such that one failure will not abort the whole thing
			try {
				currentObservation = conditions.getJSONObject(CURRENT_OBSERVATION);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (currentObservation != null) {
				try {
					weatherConditions.conditionIcon = new URL(currentObservation.getString(ICON_URL));
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					weatherConditions.conditionTitle = currentObservation.getString(WEATHER);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					weatherConditions.temperature = Float.valueOf((float) currentObservation.getDouble(TEMP_C));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String humidity = null;
				try {
					humidity = currentObservation.getString(RELATIVE_HUMIDITY);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (humidity != null) {
					StringBuilder humidityBuilder = new StringBuilder(humidity);
					int percentLocation = humidityBuilder.lastIndexOf("%");
					if (percentLocation > 0) {
						humidityBuilder = humidityBuilder.deleteCharAt(percentLocation);
					}
					weatherConditions.humidity = Float.valueOf(humidityBuilder.toString());
				}
				try {
					weatherConditions.windSpeed = Float.valueOf((float) currentObservation.getDouble(WIND_KPH));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					weatherConditions.windSpeedGust = Float.valueOf((float) currentObservation.getDouble(WIND_GUST_KPH));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					weatherConditions.visibility = Float.valueOf((float) currentObservation.getDouble(VISIBILITY_KM));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					weatherConditions.feelsLikeTemperature = Float.valueOf((float) currentObservation.getDouble(HEAT_INDEX_C));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					weatherConditions.feelsLikeTemperature = Float.valueOf((float) currentObservation.getDouble(WINDCHILL_C));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					weatherConditions.precipitation = Float.valueOf((float) currentObservation.getDouble(PRECIP_1HR_METRIC));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		return weatherConditions;
	}
	
	@Override
	public WeatherAlert[] getAlerts(JSONObject alerts) {
		// TODO Auto-generated method stub
		return null;
	}
}
