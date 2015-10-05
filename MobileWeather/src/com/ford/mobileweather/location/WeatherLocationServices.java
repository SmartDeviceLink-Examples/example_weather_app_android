package com.ford.mobileweather.location;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ford.mobileweather.artifact.GPSLocation;
import com.ford.mobileweather.artifact.WeatherLocation;
import com.ford.mobileweather.smartdevicelink.SmartDeviceLinkApplication;
import com.ford.mobileweather.weather.WeatherDataManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class WeatherLocationServices implements
	LocationListener,
	GoogleApiClient.ConnectionCallbacks,
	GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
	
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int UPDATE_INTERVAL_IN_SECONDS = 30;
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    private static final int FASTEST_INTERVAL_IN_SECONDS = 10;
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    private static final double MIN_TIME_BETWEEN_LOCATION_UPDATES = 30.0;
    
    // Define an object that holds accuracy and frequency parameters
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    
    private Geocoder mGeocoder; 
    private boolean mUpdatesRequested;
    private SmartDeviceLinkApplication mApplicationContext;
    private WeatherDataManager mDataManager;
    protected long mLastLocationTime = 0;
	

    public WeatherLocationServices() {
    	mApplicationContext = SmartDeviceLinkApplication.getInstance();
    	// Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        buildGoogleApiClient();
        mGeocoder = new Geocoder(mApplicationContext, Locale.getDefault());
        mUpdatesRequested = false;
        mDataManager = WeatherDataManager.getInstance();
    }

	@Override
	public void onLocationChanged(Location location) {
		if (location != null) {
			PowerManager powerManager = (PowerManager) mApplicationContext.getSystemService(Context.POWER_SERVICE);
			WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileWeatherLocationChanged");

			try {
				wakeLock.acquire();
				Intent intent = new Intent("com.ford.mobileweather.Location");
				WeatherLocation loc = processLocation(location);
				
				if (loc != null) {
					if (mDataManager != null) {
						mDataManager.setCurrentLocation(loc);
		        		reportLocationAvail(true);
			        	if (((System.currentTimeMillis() - mLastLocationTime) / 1000.0 > MIN_TIME_BETWEEN_LOCATION_UPDATES)) {
			        		LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(intent);
			        		mLastLocationTime = System.currentTimeMillis();
			            }
					}
				} else {
					Log.e(SmartDeviceLinkApplication.TAG, "onLocationChanged: loc == null");
					reportLocationAvail(false);
				}
			} finally {
				wakeLock.release();
			}
		} else {
			Log.e(SmartDeviceLinkApplication.TAG, "onLocationChanged: location == null");
			reportLocationAvail(false);
			
		}
	}
	
	private void reportLocationAvail(boolean avail){
		if (mDataManager!=null){
			if (avail != mDataManager.isLocationAvailable()){
				mDataManager.setLocationAvailable(avail);
				Intent intent = new Intent("com.ford.mobileweather.ErrorUpdate");
				LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(intent);
			}
		}
	}
	
	private void reportNetworkAvail(boolean avail) {
		if (mDataManager != null){
			if (avail != mDataManager.isNetworkAvailable()){
				mDataManager.setNetworkAvailable(avail);
				Intent intent = new Intent("com.ford.mobileweather.ErrorUpdate");
				LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(intent);
			}
		}
	}
	
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(SmartDeviceLinkApplication.getCurrentActivity(), CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            showErrorDialog(connectionResult.getErrorCode());
        }
	}

	@Override
	public void onConnected(Bundle arg0) {
		getLastKnownLocation();
		if (mUpdatesRequested) {
			LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
	}
	
	public void start() {
		mUpdatesRequested = true;
		if (!mGoogleApiClient.isConnected()) {
			mGoogleApiClient.connect();
		}
	}
	
	public void stop() {
		// If the client is connected
        if (mGoogleApiClient.isConnected()) {
            /*
             * Remove location updates for a listener.
             * The current Activity is the listener, so
             * the argument is "this".
             */
        	LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
        /*
         * After disconnect() is called, the client is
         * considered "dead".
         */
        mGoogleApiClient.disconnect();
        reportLocationAvail(false);
        mUpdatesRequested = false;
	}
	
	private void getLastKnownLocation() {
		if (mGoogleApiClient.isConnected()) {
			Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
			WeatherLocation loc = processLocation(location);
			if (mDataManager != null && loc != null) {
				mDataManager.setCurrentLocation(loc);
				Intent intent = new Intent("com.ford.mobileweather.Location");
				LocalBroadcastManager.getInstance(mApplicationContext).sendBroadcast(intent);
				reportLocationAvail(true);
			} else if(loc == null){
				Log.e(SmartDeviceLinkApplication.TAG, "getLastKnownLocation: loc == null");
				reportLocationAvail(false);
			}
		}
	}
	
	private WeatherLocation processLocation(Location location) {
		if (location != null) {
			PowerManager powerManager = (PowerManager) mApplicationContext.getSystemService(Context.POWER_SERVICE);
			WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MobileWeatherProcessLocation");

			try {
				wakeLock.acquire();
				WeatherLocation loc = null;
				
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && Geocoder.isPresent()) {
					loc = reverseGeocode(location);
				}
				else {
					loc = new WeatherLocation();
					GPSLocation gpsLoc = new GPSLocation();
					gpsLoc.latitude = String.valueOf(location.getLatitude());
					gpsLoc.longitude = String.valueOf(location.getLongitude());
					loc.gpsLocation = gpsLoc;		
				}
				return loc;
			} finally {
				wakeLock.release();
			}
		}
		return null;
	}
	
	private WeatherLocation reverseGeocode(Location location) {
        List<Address> addresses = null;
        
        try {
            addresses = mGeocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            reportNetworkAvail(true);
        } catch (IOException e1) {
        	Log.e(SmartDeviceLinkApplication.TAG, "IO Exception in getFromLocation()");
        	reportNetworkAvail(false);
        	e1.printStackTrace();
        	return null;
        } catch (IllegalArgumentException e2) {
        	Log.e(SmartDeviceLinkApplication.TAG, "Illegal Argument Exception in getFromLocation()");
        	reportLocationAvail(false);
        	e2.printStackTrace();
        	return null;
        }
        
        // If the reverse geocode returned an address
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
        	WeatherLocation resolvedLoc = new WeatherLocation();
        	resolvedLoc.gpsLocation = new GPSLocation();
        	resolvedLoc.gpsLocation.latitude = String.valueOf(location.getLatitude());
        	resolvedLoc.gpsLocation.longitude = String.valueOf(location.getLongitude());
            resolvedLoc.city = address.getLocality();
            resolvedLoc.state = address.getAdminArea();
            resolvedLoc.zipCode = address.getPostalCode();
            return resolvedLoc;
        } else {
            return null;
        }
	}
	
    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
            errorCode,
            SmartDeviceLinkApplication.getCurrentActivity(),
            CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
        	errorDialog.show();
        }
    }
    
    protected synchronized void buildGoogleApiClient() {
    	Log.i(SmartDeviceLinkApplication.TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(mApplicationContext)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();
    }

}
