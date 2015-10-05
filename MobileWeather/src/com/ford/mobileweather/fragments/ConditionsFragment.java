package com.ford.mobileweather.fragments;


import java.util.Locale;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.ford.mobileweather.R;
import com.ford.mobileweather.artifact.WeatherLocation;
import com.ford.mobileweather.processor.ImageProcessor;
import com.ford.mobileweather.weather.UnitConverter;
import com.ford.mobileweather.weather.WeatherConditions;
import com.ford.mobileweather.weather.WeatherDataManager;

public class ConditionsFragment extends BaseFragment {
	
	private ImageView mConditionsIconView;
	private TextView mConditionsTextView;
	private TextView mCurrentTempView;
	private TextView mAdjustedTempView;
	private TextView mWindSpeedView;
	private TextView mHumidityView;
	private TextView mLocationTextView;
	private WeatherDataManager mDataManager;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_conditions, null);
		mConditionsIconView = (ImageView) v.findViewById(R.id.conditionsIcon);
		mConditionsTextView = (TextView) v.findViewById(R.id.conditionsText);
		mCurrentTempView = (TextView) v.findViewById(R.id.currentTemp);
		mAdjustedTempView = (TextView) v.findViewById(R.id.adjustedTemp);
		mWindSpeedView = (TextView) v.findViewById(R.id.windSpeed);
		mHumidityView = (TextView) v.findViewById(R.id.humidity);
		
	    
		
		
		mLocationTextView = (TextView) v.findViewById(R.id.conditionsLocationTextView);

		mDataManager = WeatherDataManager.getInstance();
		if (mDataManager != null) {
			WeatherConditions conditions = mDataManager.getWeatherConditions();
			String units = mDataManager.getUnits();
			WeatherLocation location = mDataManager.getCurrentLocation();
			if (conditions != null) {
				setConditions(conditions, units);
			}
			if (location != null) {
				setLocation(location);
			}
		}

	    return v;
	}
	
	public void updateLocation() {
		if (mDataManager != null) {
			WeatherLocation location = mDataManager.getCurrentLocation();
			if (location != null) {
				setLocation(location);
			}
		}		
	}

	
	
	public void setLocation(WeatherLocation location) {
		if (location != null) { 
			String locationtext = "";
			if (mLocationTextView != null){
				if (location.city != null){
					locationtext = location.city;
				}
				if (location.state != null){
					if (location.city != null){
						locationtext += ", " + location.state;
					}
					else{
						locationtext = location.state;
					}
				}
				mLocationTextView.setText(locationtext);
			}
		}
	}

	public void updateConditions() {
		if (mDataManager != null) {
			WeatherConditions conditions = mDataManager.getWeatherConditions();
			String units = mDataManager.getUnits();
			if (conditions != null) {
				setConditions(conditions, units);
			}
		}
	}
	
	/**
	 * Store the current conditions in the fragment
	 * @param conditions
	 */
	public void setConditions(WeatherConditions conditions, String units) {
		if (conditions != null) {
			Float temperature = conditions.temperature;
			Float windSpeed = conditions.windSpeed;
			Float humidity = conditions.humidity;
			Float adjustedTemperature = conditions.feelsLikeTemperature;
			String temp = null;
			String wind = null;
			String humid = null;
			String adjTemp = null;
			String tempUnits = null;
			String speedUnits = null;
						
			if ((getResources().getString(R.string.units_imperial)).equalsIgnoreCase(units) || units == null) {
				if (temperature != null)
					temperature = Float.valueOf(UnitConverter.convertTemperatureToImperial(temperature.floatValue()));
				if (windSpeed != null)
					windSpeed = Float.valueOf(UnitConverter.convertSpeedToImperial(windSpeed.floatValue()));
				if (adjustedTemperature != null)
					adjustedTemperature = Float.valueOf(UnitConverter.convertTemperatureToImperial(adjustedTemperature.floatValue()));

				tempUnits = getResources().getString(R.string.units_imp_temp_short);
				speedUnits = getResources().getString(R.string.units_imp_speed_short);
			}else {
				tempUnits = getResources().getString(R.string.units_metric_temp_short);
				speedUnits = getResources().getString(R.string.units_metric_speed_short);
			}
			if (temperature != null)
				temp = String.format(Locale.getDefault(), "%.0f \u00B0 %s", temperature.floatValue(), tempUnits);
			if (windSpeed != null)
				wind = String.format(Locale.getDefault(), getResources().getString(R.string.conditions_windspeed) + ": %.0f %s", windSpeed.floatValue(), speedUnits);
			if (humidity != null)
				humid = String.format(Locale.getDefault(), getResources().getString(R.string.conditions_humidity) + ": %.0f %%", humidity.floatValue());
			if (adjustedTemperature != null)
				adjTemp = String.format(Locale.getDefault(), getResources().getString(R.string.conditions_feelslike) + ": %.0f \u00B0 %s", adjustedTemperature.floatValue(), tempUnits);
			
			mCurrentTempView.setText(temp);
			mConditionsTextView.setText(conditions.conditionTitle);
			mAdjustedTempView.setText(adjTemp);			
			mWindSpeedView.setText(wind);
			mHumidityView.setText(humid);
			
			if (conditions.conditionIcon != null)
				ImageProcessor.setConditionsImage(mConditionsIconView, conditions.conditionIcon, false);
		}
	}
}
