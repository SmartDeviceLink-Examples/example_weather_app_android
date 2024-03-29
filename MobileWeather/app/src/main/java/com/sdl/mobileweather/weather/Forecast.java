package com.sdl.mobileweather.weather;

import java.util.Calendar;

public class Forecast {
	public Calendar date;
	public String conditionTitle;
	public String conditionId;
	public Float temperature = (float)0;
	public Float highTemperature = (float)0;
	public Float lowTemperature = (float)0;
	public Float humidity = (float)0;
	public Float windSpeed = (float)0;
	public Float precipitation = (float)0;
	public Integer precipitationChance = 0;
	public Float snow = (float)0;	
}
