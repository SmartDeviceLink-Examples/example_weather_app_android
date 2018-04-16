package com.sdl.mobileweather.smartdevicelink;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.smartdevicelink.transport.SdlBroadcastReceiver;
import com.smartdevicelink.transport.SdlRouterService;

public class SmartDeviceLinkReceiver extends SdlBroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        // Start services on BT connection
        if (intent.getAction().compareTo(BluetoothDevice.ACTION_ACL_CONNECTED) == 0) {
            Log.i(SmartDeviceLinkApplication.TAG, "ACL Connect");
            SmartDeviceLinkApplication app = SmartDeviceLinkApplication.getInstance();
            if (app != null) {
                Log.i(SmartDeviceLinkApplication.TAG, "Starting services");
                app.startLocationServices();
                app.startWeatherUpdates();
            }
        }
        // Stop services on BT disconnection
        else if (intent.getAction().compareTo(BluetoothDevice.ACTION_ACL_DISCONNECTED) == 0) {
            Log.i(SmartDeviceLinkApplication.TAG, "ACL Disconnect");
            SmartDeviceLinkApplication app = SmartDeviceLinkApplication.getInstance();
            if (app != null) {
                if (SmartDeviceLinkApplication.getCurrentActivity() == null) {
                    app.stopServices();
                }
            }
        }

        super.onReceive(context, intent);
    }


    @Override
    public void onSdlEnabled(Context context, Intent intent) {
        //Use the provided intent but set the class to the SdlService
        intent.setClass(context, SmartDeviceLinkService.class);


//      SdlService needs to be foregrounded in Android O and above
//      This will prevent apps in the background from crashing when they try to start SdlService
//      Because Android O doesn't allow background apps to start background services
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }


    @Override
    public Class<? extends SdlRouterService> defineLocalSdlRouterClass() {
        //Return a local copy of the SdlRouterService located in your project
        return com.sdl.mobileweather.smartdevicelink.SdlRouterService.class;
    }

}