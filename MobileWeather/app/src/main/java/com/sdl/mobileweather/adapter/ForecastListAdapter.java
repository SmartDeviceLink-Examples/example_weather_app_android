package com.sdl.mobileweather.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sdl.mobileweather.R;
import com.sdl.mobileweather.processor.ImageProcessor;
import com.sdl.mobileweather.weather.Forecast;
import com.sdl.mobileweather.weather.UnitConverter;
import com.sdl.mobileweather.weather.WeatherDataManager;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

public class ForecastListAdapter extends ArrayAdapter<Forecast> {
	private final Context mContext;
	private final Forecast[] mForecast;
	private WeatherDataManager mDataManager;
	private boolean[] mForecastType;
	private LayoutInflater mInflater;
	private Activity parentActivity;
	
	public ForecastListAdapter(Context context, Forecast[] forecast, Activity parentActivity) {
		super(context, R.layout.forecast_list_item, forecast);
		this.mContext = context;
		this.mForecast = forecast;
		this.mDataManager = WeatherDataManager.getInstance();
		this.mForecastType = new boolean[forecast.length];
		this.parentActivity = parentActivity;
		Arrays.fill(this.mForecastType, false);
		mInflater = ((Activity) this.mContext).getLayoutInflater();
	}
	
	public void toggleForecastType(int position) {
		if (position < this.mForecast.length) {
			this.mForecastType[position] = !this.mForecastType[position]; 
		}
	}
	
	@Override
	public View getView(int position, View view, ViewGroup parent) {
		View rowView;
		
		if (!this.mForecastType[position]) {
			rowView = mInflater.inflate(R.layout.forecast_list_item, null, true);
			ImageView forecastImageView = (ImageView) rowView.findViewById(R.id.forecastImageView);
			TextView shortDayTextView = (TextView) rowView.findViewById(R.id.forecastShortDayTextView);
			TextView precipTextView = (TextView) rowView.findViewById(R.id.forecastPrecip);
			TextView lowTempTextView = (TextView) rowView.findViewById(R.id.forecastLowTemp);
			TextView highTempTextView = (TextView) rowView.findViewById(R.id.forecastHighTemp);
			
			String units = null;
			if (mDataManager != null) {
				units = mDataManager.getUnits();
			}
			
			forecastImageView.setImageBitmap(null);
			shortDayTextView.setText("");
			precipTextView.setText("");
			lowTempTextView.setText("");
			highTempTextView.setText("");
			
			if (position < this.mForecast.length) {
				Forecast day = this.mForecast[position];	
							
				if ((mContext.getResources().getString(R.string.conditions_loading)).equals(day.conditionTitle)) {
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f);
					precipTextView.setLayoutParams(params);
					precipTextView.setGravity(Gravity.CENTER | Gravity.CENTER);
					precipTextView.setText(day.conditionTitle);
					forecastImageView.setImageBitmap(null);
				}
				else if((day.conditionId != null) &&
						(day.precipitationChance != null) &&
						(day.highTemperature != null) &&
						(day.lowTemperature != null) &&
						(day.date != null)) {
					String conditionId = day.conditionId;
					Integer precip = day.precipitationChance;
					Float highTemperature = day.highTemperature;
					Float lowTemperature = day.lowTemperature;
					Calendar forecastDate = day.date;
	
					SimpleDateFormat dateFormat = new SimpleDateFormat(mContext.getResources().getString(R.string.weather_forecast_day_simpleDateFormat), Locale.getDefault());
					String dateString = dateFormat.format(forecastDate.getTime().getTime()*1000);
					String precipChance = null;
					String highTemp = null;
					String lowTemp = null;
								
					if ((mContext.getResources().getString(R.string.units_imperial)).equalsIgnoreCase(units) || units == null) {
						if (highTemperature != null)
							highTemperature = Float.valueOf(UnitConverter.convertTemperatureToImperial(highTemperature.floatValue()));
						if (lowTemperature != null)
							lowTemperature = Float.valueOf(UnitConverter.convertTemperatureToImperial(lowTemperature.floatValue()));
					}
					if (precip != null) {
						precipChance = String.format(Locale.getDefault(), "%d%%", precip.intValue());
					}
					else {
						precipChance = "   ";
					}
					if (highTemperature != null)
						highTemp = String.format(Locale.getDefault(), mContext.getResources().getString(R.string.weather_forecast_high_temp) + "%.0f\u00B0", highTemperature);
					if (lowTemperature != null)
						lowTemp = String.format(Locale.getDefault(), mContext.getResources().getString(R.string.weather_forecast_low_temp) + "%.0f\u00B0", lowTemperature);
					
					shortDayTextView.setText(dateString);
					precipTextView.setText(precipChance);
					lowTempTextView.setText(lowTemp);
					highTempTextView.setText(highTemp);
					if (conditionId != null)
						ImageProcessor.setConditionsImage(forecastImageView, conditionId);
				}
			}

		}
		else {
			rowView = mInflater.inflate(R.layout.forecast_list_item, null, true);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1.0f);
			TextView precipTextView = (TextView) rowView.findViewById(R.id.forecastPrecip);
			ImageView forecastImageView = (ImageView) rowView.findViewById(R.id.forecastImageView);
			precipTextView.setLayoutParams(params);
			precipTextView.setGravity(Gravity.CENTER | Gravity.CENTER);
			precipTextView.setText(mContext.getResources().getString(R.string.weather_forecast_hourly));
			forecastImageView.setImageBitmap(null);
		}
		return rowView;
	}
}
