package com.ford.mobileweather.smartdevicelink;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SmartDeviceLinkReceiver extends BroadcastReceiver {	
	public void onReceive(Context context, Intent intent) {
		// Start the SmartDeviceLinkService on BT connection
		if (intent.getAction().compareTo(BluetoothDevice.ACTION_ACL_CONNECTED) == 0) {
			Log.i(SmartDeviceLinkApplication.TAG, "ACL Connect");
			SmartDeviceLinkApplication app = SmartDeviceLinkApplication.getInstance();
			if (app != null) {
				Log.i(SmartDeviceLinkApplication.TAG, "Starting services");
				app.startSdlProxyService();
				app.startServices();
			}
		}
		// Stop the SmartDeviceService on BT disconnection
		else if (intent.getAction().compareTo(BluetoothDevice.ACTION_ACL_DISCONNECTED) == 0) {
			Log.i(SmartDeviceLinkApplication.TAG, "ACL Disconnect");
			SmartDeviceLinkApplication app = SmartDeviceLinkApplication.getInstance();
			if (app != null) {
				SmartDeviceLinkService als = SmartDeviceLinkService.getInstance();
				if (als != null) {
					Log.i(SmartDeviceLinkApplication.TAG, "Stopping als");
					app.endSdlProxyService();
				}
		    	if (SmartDeviceLinkApplication.getCurrentActivity() == null) {
		    		app.stopServices();
		    	}
			}
		}
	}
}
