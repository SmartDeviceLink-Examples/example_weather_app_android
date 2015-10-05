package com.ford.mobileweather.fragments;


import com.ford.mobileweather.R;
import com.ford.mobileweather.adapter.ForecastListAdapter;
import com.ford.mobileweather.artifact.WeatherLocation;
import com.ford.mobileweather.weather.Forecast;
import com.ford.mobileweather.weather.WeatherDataManager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

public class ForecastFragment extends BaseFragment {
	private View mFragmentView;
	private TextView mLocationTextView;
	private ListView mForecastListView;
	private WeatherDataManager mDataManager;
	private ForecastListAdapter mAdapter;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mFragmentView = inflater.inflate(R.layout.fragment_forecast, null);
		mLocationTextView = (TextView) mFragmentView.findViewById(R.id.forecastLocationTextView);
		mForecastListView = (ListView) mFragmentView.findViewById(R.id.forecastListView);
		
		mDataManager = WeatherDataManager.getInstance();
		if (mDataManager != null) {
			Forecast[] forecast = mDataManager.getForecast();
			String units = mDataManager.getUnits();
			WeatherLocation location = mDataManager.getCurrentLocation();
			if (location != null) {
				setLocation(location);
			}
			if (forecast != null) {
				setForecast(forecast, units);
			}
			else {
				Forecast loadingForecast = new Forecast();
				loadingForecast.conditionTitle = getResources().getString(R.string.forecast_loading);
				Forecast[] loading = { loadingForecast };
				setForecast(loading, units);
			}
		}
	
		return mFragmentView;
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
	
	
	public void updateForecast() {
		if (mDataManager != null) {
			Forecast[] forecast = mDataManager.getForecast();
			String units = mDataManager.getUnits();
			if (forecast != null) {
				setForecast(forecast, units);
			}
		}
	}

	/**
	 * Store the current forecast in the fragment
	 * @param forecast
	 */
	public void setForecast(Forecast[] forecast, String units) {
		if (forecast != null) {
			mAdapter = new ForecastListAdapter(getActivity(), forecast);
			mForecastListView.setAdapter(mAdapter);
			mForecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
	            @Override
	            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	            	mAdapter.toggleForecastType(position);
	            	mAdapter.notifyDataSetChanged();
	            }
	        });
		}
	}
}
