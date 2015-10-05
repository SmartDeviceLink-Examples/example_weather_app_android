package com.ford.mobileweather.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.ford.mobileweather.R;
import com.ford.mobileweather.artifact.WeatherLocation;
import com.ford.mobileweather.processor.ImageProcessor;
import com.ford.mobileweather.smartdevicelink.SmartDeviceLinkApplication;
import com.ford.mobileweather.weather.WeatherDataManager;


public class LockScreenActivity extends Activity {
	private static LockScreenActivity instance;
	private WeatherLocation mCurrentLocation = null;
	private TextView mLocationTextView = null;
	private WeatherDataManager mDataManager = null;
	
	static {
		LockScreenActivity.instance = null;
	}
	
	/**
	 * Receiver for changes in location from the app UI.
	 */
	protected final BroadcastReceiver mChangeLocationReceiver = new BroadcastReceiver() {
		
        @Override
        public void onReceive(Context context, Intent intent) {
        	if (mDataManager != null) {
        		mCurrentLocation = mDataManager.getCurrentLocation();
        		updateLocation();
        	}
        }
	};
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.v(SmartDeviceLinkApplication.TAG, "onCreate lock");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lockscreen);
        mDataManager = WeatherDataManager.getInstance();
		LockScreenActivity.instance = this;
		SmartDeviceLinkApplication.setCurrentActivity(this);
		
		mLocationTextView = (TextView) findViewById(R.id.locationTextView);
		ImageView providerImageView = (ImageView) findViewById(R.id.providerImage);
		//Bitmap mappedImage = ImageProcessor.getBitmapFromResources("wunderground");
		Bitmap logoImage = ImageProcessor.getBitmapFromResources("powered");
		providerImageView.setImageBitmap(logoImage);	
    }
    
    @Override
	protected void onStart() {
    	if (mDataManager != null) {
			mCurrentLocation = mDataManager.getCurrentLocation();
			updateLocation();
		}
		LocalBroadcastManager.getInstance(this).registerReceiver(mChangeLocationReceiver, new IntentFilter("com.ford.mobileweather.Location"));
		super.onStart();
	}

	@Override
	protected void onPause() {
		Log.v(SmartDeviceLinkApplication.TAG, "onPause lock");
		// Notify App that activity is leaving the foreground
		if (SmartDeviceLinkApplication.getCurrentActivity() == this) {
			Log.v(SmartDeviceLinkApplication.TAG, "onPause clearing");
			SmartDeviceLinkApplication.setCurrentActivity(null);
		}
		super.onPause();
	}

	@Override
	protected void onStop() {
		Log.v(SmartDeviceLinkApplication.TAG, "onStop lock");		
		// Stop services if no other weather activity has taken foreground
		SmartDeviceLinkApplication app = SmartDeviceLinkApplication.getInstance();
		if (app != null) {
			app.stopServices();
		}else{
			Log.d(SmartDeviceLinkApplication.TAG, "Lock onStop app==null");
		}
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mChangeLocationReceiver);
		super.onStop();
	}

	// Disable back button on lockscreen
    @Override
    public void onBackPressed() {
    }

    @Override
    public void onDestroy() {
    	Log.v(SmartDeviceLinkApplication.TAG, "onDestroy lock");
    	LockScreenActivity.instance = null;
    	super.onDestroy();
    }

    public void exit() {
    	super.finish();
    }

    public static LockScreenActivity getInstance() {
    	return instance;
    }

    private void updateLocation() {  	
    	if (mCurrentLocation != null) {
    		String locationtext = "";
    		if (mLocationTextView != null){
    			if (mCurrentLocation.city != null){
    				locationtext = mCurrentLocation.city;
    			}
    			if (mCurrentLocation.state != null){
    				if (mCurrentLocation.city != null){
    					locationtext += ", " + mCurrentLocation.state;
    				}
    				else{
    					locationtext = mCurrentLocation.state;
    				}
    			}
    			mLocationTextView.setText(locationtext);
    		}
    	}
    }
    
}
