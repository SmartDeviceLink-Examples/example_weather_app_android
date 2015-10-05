package com.ford.mobileweather.location;

import android.util.Log;

import com.ford.mobileweather.smartdevicelink.SmartDeviceLinkApplication;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class PlayServicesConnectionChecker {

	public static boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(SmartDeviceLinkApplication.getInstance());
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d(SmartDeviceLinkApplication.TAG, "Google Play services is available.");
            return true;
        // Google Play services was not available for some reason
        } else {
            Log.e(SmartDeviceLinkApplication.TAG, "Google Play services are not available!");
        }
        return false;
    }
}
