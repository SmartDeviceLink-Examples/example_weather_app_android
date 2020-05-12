package com.sdl.mobileweather.smartdevicelink;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.sdl.mobileweather.weather.AbbreviationDictionary;

public class SdlActivity extends Activity {
	private boolean activityOnTop;
	
	/**
	 * Activity is moving to the foreground.
	 * Set this activity as the current activity and that it is on top.
	 * Update the lockscreen.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		SdlApplication.setCurrentActivity(this);
		activityOnTop = true;
	}
	
	/**
	 * Activity becoming partially visible (obstructed by another).
	 * Activity if no longer on top.
	 */
	@Override
	protected void onPause() {
		activityOnTop = false;
		// Notify App that activity is leaving forground
		if (SdlApplication.getCurrentActivity() == this) {
			SdlApplication.setCurrentActivity(null);
		}else{
			if(SdlApplication.getCurrentActivity() == null){
				Log.v(SdlApplication.TAG, "Current activity already null.");
			}else{
				Log.v(SdlApplication.TAG, "This activity is not on top.");
			}
		}
		super.onPause();
	}
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		if(!AbbreviationDictionary.isPrepared())
			AbbreviationDictionary.loadDictionary(this);
    }

	/**
	 * Activity is no longer visible on the screen.
	 */
	@Override
	protected void onStop() {
		// Stop services if no other weather activity has taken foreground
		SdlApplication app = SdlApplication.getInstance();
		if (app != null) {
			app.stopServices();
		}else{
			Log.d(SdlApplication.TAG, "onStop app==null");
		}
    	super.onStop();		
	}
	
    @Override
	protected void onDestroy() {
    	super.onDestroy();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	return true;	            
	}
    
	public boolean isActivityonTop(){
		return activityOnTop;
	}
}
