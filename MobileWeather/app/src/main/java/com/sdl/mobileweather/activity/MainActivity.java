package com.sdl.mobileweather.activity;


import android.Manifest;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.legacy.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.drawerlayout.widget.DrawerLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.sdl.mobileweather.BuildConfig;
import com.sdl.mobileweather.R;
import com.sdl.mobileweather.fragments.ConditionsFragment;
import com.sdl.mobileweather.fragments.ForecastFragment;
import com.sdl.mobileweather.smartdevicelink.SdlActivity;
import com.sdl.mobileweather.smartdevicelink.SdlApplication;
import com.sdl.mobileweather.smartdevicelink.SdlReceiver;


public class MainActivity extends SdlActivity implements ActionBar.TabListener {

	private static final String SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	private static final String APP_ID = "bf2c3a7bad6b0c79152f50cc42ba1ace";
	private static final int PERMISSIONS_REQUEST_CODE = 100;

    private Fragment mCurrentFragment;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
	private String[] mDrawerTitles;
	private ArrayAdapter<String> mDrawerAdapter;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

	/**
	 * Receiver to handle updates to weather conditions.
	 */
	private final BroadcastReceiver mWeatherConditionsReceiver = new BroadcastReceiver() {
		
        @Override
        public void onReceive(Context context, Intent intent) {
			if (mCurrentFragment != null && mCurrentFragment.getClass().equals(ConditionsFragment.class)) {
				((ConditionsFragment) mCurrentFragment).updateConditions();
			}
        }
	};

	/**
	 * Receiver to handle updates to the forecast.
	 */
	private final BroadcastReceiver mForecastReceiver = new BroadcastReceiver() {
		
        @Override
        public void onReceive(Context context, Intent intent) {
			if (mCurrentFragment.getClass().equals(ForecastFragment.class)) {
				((ForecastFragment) mCurrentFragment).updateForecast();
			}
        }
	};
	
	/**
	 * Receiver to handle updates to the hourly forecast.
	 */
	private final BroadcastReceiver mHourlyForecastReceiver = new BroadcastReceiver() {
		
        @Override
        public void onReceive(Context context, Intent intent) {
			if (mCurrentFragment.getClass().equals(ForecastFragment.class)) {
				((ForecastFragment) mCurrentFragment).updateForecast();
			}
        }
	};
		
	/**
	 * Receiver for changes in location from the app UI.
	 */
	protected final BroadcastReceiver mChangeLocationReceiver = new BroadcastReceiver() {
		
        @Override
        public void onReceive(Context context, Intent intent) {
			if (mCurrentFragment != null) {
				if (mCurrentFragment.getClass().equals(ForecastFragment.class)) {
					((ForecastFragment) mCurrentFragment).updateLocation();
				}
				else if (mCurrentFragment.getClass().equals(ConditionsFragment.class)) {
					((ConditionsFragment) mCurrentFragment).updateLocation();
				}
			}
        }
	};
	
	/**
	 * Drawer item click listener that handles menu actions in the navigation drawer.
	 */
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
	    @Override
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	        selectItem(position);
	    }
	}
	
    private void selectItem(int position) {
    	// TODO: act on selections in the nav drawer
    	String item = mDrawerAdapter.getItem(position);
    	if ((getResources().getString(R.string.drawer_item_update_weather)).equals(item)){
    			Intent intent = new Intent("com.sdl.mobileweather.WeatherUpdate");
    			LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    	}
    	else if ((getResources().getString(R.string.drawer_item_reset_sync)).equals(item)){
			SdlApplication.getInstance().endSdlProxyInstance();
			SdlApplication.getInstance().startSdlProxyService();
    	}
    	else if ((getResources().getString(R.string.drawer_item_about)).equals(item)){
    		SdlApplication.getInstance().showAppVersion(this);
    	}
    	mDrawerList.setItemChecked(position, false);
        mDrawerLayout.closeDrawer(mDrawerList);
    }
	
    
    
	private void checkForCrashes() {}

	private void checkForUpdates() {}

	private boolean checkPermissions() {
		boolean permissionsGranted;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			boolean bluetoothGranted = PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT);
			boolean locationGranted = PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
			permissionsGranted =  (BuildConfig.TRANSPORT.equals("TCP") || bluetoothGranted) && locationGranted;
		}
		else {
			permissionsGranted = PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
		}

		return permissionsGranted;
	}

	private void requestPermissions() {
		String[] permissions;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			permissions = new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION};
		}
		else {
			permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
		}

		ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
	}

	@Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.v(SdlApplication.TAG, "onCreate main");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		// Create tabs
		ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		//bar.addTab(bar.newTab().setText(R.string.title_alerts).setTabListener(this));
		bar.addTab(bar.newTab().setText(R.string.title_conditions).setTabListener(this));
		bar.addTab(bar.newTab().setText(R.string.title_forecast).setTabListener(this));
		//bar.setSelectedNavigationItem(1);
		bar.setSelectedNavigationItem(0);
		
		// Init Drawer list
        mTitle = mDrawerTitle = getTitle();
		mDrawerTitles = getResources().getStringArray(R.array.nav_drawer_items);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item, mDrawerTitles);
        
        // Set the adapter for the list view
        mDrawerList.setAdapter(mDrawerAdapter);
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        
        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

		checkForUpdates();
    }
	
    @Override
	protected void onStart() {
    	Log.v(SdlApplication.TAG, "onStart main");
		super.onStart();
		LocalBroadcastManager lbManager = LocalBroadcastManager.getInstance(this);
        lbManager.registerReceiver(mChangeLocationReceiver, new IntentFilter("com.sdl.mobileweather.Location"));
        lbManager.registerReceiver(mWeatherConditionsReceiver, new IntentFilter("com.sdl.mobileweather.WeatherConditions"));
        lbManager.registerReceiver(mForecastReceiver, new IntentFilter("com.sdl.mobileweather.Forecast"));
        lbManager.registerReceiver(mHourlyForecastReceiver, new IntentFilter("com.sdl.mobileweather.HourlyForecast"));

		// Ask for permissions
		if (!checkPermissions()) {
			requestPermissions();
		} else {
			startServices();
		}


	}

	private void startServices() {
		SdlApplication app = SdlApplication.getInstance();
		if (app != null) {
            app.startServices();
        }
	}

	@Override
	protected void onResume() {
    	Log.v(SdlApplication.TAG, "onResume main");
		super.onResume();
		checkForCrashes();
    }
    
    @Override
	protected void onPause() {
		Log.v(SdlApplication.TAG, "onPause main");
		super.onPause();
	}
    
	@Override
	protected void onStop() {
		Log.v(SdlApplication.TAG, "onStop main");
		try {
			LocalBroadcastManager lbManager = LocalBroadcastManager.getInstance(this);
			lbManager.unregisterReceiver(mChangeLocationReceiver);
			lbManager.unregisterReceiver(mWeatherConditionsReceiver);
			lbManager.unregisterReceiver(mForecastReceiver);
			lbManager.unregisterReceiver(mHourlyForecastReceiver);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		Log.v(SdlApplication.TAG, "onDestroy main");
		super.onDestroy();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    /** 
     * Called whenever we call invalidateOptionsMenu()
     * */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        /*boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);*/
        return super.onPrepareOptionsMenu(menu);
    }
    
    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

	    return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {
	    moveTaskToBack(true);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent e) {
	    if (keyCode == KeyEvent.KEYCODE_MENU) {
	        if (!mDrawerLayout.isDrawerOpen(mDrawerList)) {
	            mDrawerLayout.openDrawer(mDrawerList);
	        }
	        else {
	        	mDrawerLayout.closeDrawer(mDrawerList);
	        }
	        return true;
	    }
	    return super.onKeyDown(keyCode, e);
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		Fragment fragment = null;
		/*if (tab.getPosition() == 0) {
			fragment = new AlertsFragment();
		}
		else if (tab.getPosition() == 1) {
			fragment = new ConditionsFragment();
		}
		else if (tab.getPosition() == 2) {
			fragment = new ForecastFragment();
		}*/
		if (tab.getPosition() == 0) {
			fragment = new ConditionsFragment();
		}
		else if (tab.getPosition() == 1) {
			fragment = new ForecastFragment();
		}
		
		if (fragment != null) {
			mCurrentFragment = fragment;
			getFragmentManager().beginTransaction().replace(R.id.content_frame, fragment).commit();
		}
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState.containsKey(SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(savedInstanceState.getInt(SELECTED_NAVIGATION_ITEM));
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(SELECTED_NAVIGATION_ITEM, getActionBar().getSelectedNavigationIndex());
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == PERMISSIONS_REQUEST_CODE) {
			if (!checkPermissions()) {
				Toast.makeText(this, "The app cannot run without these permissions!", Toast.LENGTH_SHORT).show();
				finish();
			} else {
				startServices();
			}
		}
	}
}
