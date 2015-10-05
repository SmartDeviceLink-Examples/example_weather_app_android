package com.ford.mobileweather.fragments;


import com.ford.mobileweather.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AlertsFragment extends BaseFragment {

	public static final String TEMP_TEXT = "ALERTS";
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_alerts, null);
		TextView tv = (TextView) v.findViewById(R.id.alerts_msg);
		tv.setText(TEMP_TEXT);
	    return v;	    
	}
}
