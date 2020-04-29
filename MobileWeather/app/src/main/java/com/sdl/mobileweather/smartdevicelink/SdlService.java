package com.sdl.mobileweather.smartdevicelink;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;

import com.sdl.mobileweather.R;
import com.sdl.mobileweather.artifact.WeatherLocation;
import com.sdl.mobileweather.localization.LocalizationUtil;
import com.sdl.mobileweather.processor.ImageProcessor;
import com.sdl.mobileweather.weather.AbbreviationDictionary;
import com.sdl.mobileweather.weather.Forecast;
import com.sdl.mobileweather.weather.InfoType;
import com.sdl.mobileweather.weather.UnitConverter;
import com.sdl.mobileweather.weather.WeatherAlert;
import com.sdl.mobileweather.weather.WeatherConditions;
import com.sdl.mobileweather.weather.WeatherDataManager;
import com.smartdevicelink.managers.BaseSubManager;
import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.SdlManager;
import com.smartdevicelink.managers.SdlManagerListener;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.managers.lifecycle.LifecycleConfigurationUpdate;
import com.smartdevicelink.managers.lockscreen.LockScreenConfig;
import com.smartdevicelink.managers.screen.SoftButtonObject;
import com.smartdevicelink.managers.screen.SoftButtonState;
import com.smartdevicelink.managers.screen.choiceset.ChoiceCell;
import com.smartdevicelink.managers.screen.choiceset.ChoiceSet;
import com.smartdevicelink.managers.screen.choiceset.ChoiceSetLayout;
import com.smartdevicelink.managers.screen.choiceset.ChoiceSetSelectionListener;
import com.smartdevicelink.managers.screen.menu.MenuCell;
import com.smartdevicelink.managers.screen.menu.MenuSelectionListener;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.TTSChunkFactory;
import com.smartdevicelink.proxy.rpc.Alert;
import com.smartdevicelink.proxy.rpc.ChangeRegistration;
import com.smartdevicelink.proxy.rpc.DeviceStatus;
import com.smartdevicelink.proxy.rpc.DisplayCapabilities;
import com.smartdevicelink.proxy.rpc.ListFiles;
import com.smartdevicelink.proxy.rpc.ListFilesResponse;
import com.smartdevicelink.proxy.rpc.OnButtonEvent;
import com.smartdevicelink.proxy.rpc.OnButtonPress;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.OnLanguageChange;
import com.smartdevicelink.proxy.rpc.OnVehicleData;
import com.smartdevicelink.proxy.rpc.SetDisplayLayout;
import com.smartdevicelink.proxy.rpc.SetGlobalProperties;
import com.smartdevicelink.proxy.rpc.Speak;
import com.smartdevicelink.proxy.rpc.SubscribeButton;
import com.smartdevicelink.proxy.rpc.TTSChunk;
import com.smartdevicelink.proxy.rpc.TextField;
import com.smartdevicelink.proxy.rpc.enums.ButtonName;
import com.smartdevicelink.proxy.rpc.enums.DisplayType;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.InteractionMode;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.Result;
import com.smartdevicelink.proxy.rpc.enums.SpeechCapabilities;
import com.smartdevicelink.proxy.rpc.enums.SystemCapabilityType;
import com.smartdevicelink.proxy.rpc.enums.TextAlignment;
import com.smartdevicelink.proxy.rpc.enums.TextFieldName;
import com.smartdevicelink.proxy.rpc.enums.TriggerSource;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.MultiplexTransportConfig;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import static com.smartdevicelink.trace.enums.Mod.proxy;


public class SdlService extends Service {
    private static final String TAG = "SdlService";
    private SdlManager sdlManager = null;
    private final int FOREGROUND_SERVICE_ID = 200;
    private static final String APP_ID = "330533107";
    private static final int STANDARD_FORECAST_DAYS = 3;
    private static final int DAILY_FORECAST_DAYS = 8; /* max. 8 days might be shown */
    private static final int HOURLY_FORECAST_HOURS = 12;
    private static final int TIMED_SHOW_DELAY = 8000;
    private static final String APP_ICON_NAME = "icon";
    private static final String APP_ICON = APP_ICON_NAME + ".png";
    // Service shutdown timing constants
    private static final int CONNECTION_TIMEOUT = 120000;
    private static final int STOP_SERVICE_DELAY = 5000;
    private int conditionsID = 0;
    // variable used to increment correlation ID for every request sent to SDL
    public int autoIncCorrId = 0;
    // variable to contain the current state of the service
    private static SdlService instance = null;
    // variable to access the BluetoothAdapter
    private BluetoothAdapter mBtAdapter;
    // variable to create and call functions of the SyncProxy
    private boolean mFirstHmiNone = true;
    private DisplayType mDisplayType = null; // Keeps track of the HMI display type
    private boolean mGraphicsSupported = false; // Keeps track of whether graphics are supported on the display
    private boolean mDisplayLayoutSupported = false;
    private int mNumberOfTextFields = 1;
    private int mLengthOfTextFields = 40;
    private ArrayList<TextField> mTextFields = null; // Keeps track of the text fields supported
    private Language mCurrentSdlLanguage = null; // Stores the current language
    private Language mCurrentHmiLanguage = null; // Stores the current language of the display
    private static Language mRegisteredAppSdlLanguage = Language.EN_US; // Stores the language used at AppInterface registering
    private static Language mRegisteredAppHmiLanguage = Language.EN_US; // Stores the language of the display used at AppInterface registering
    private static Language mDesiredAppSdlLanguage = Language.EN_US; // Stores the language to be used for next AppInterface registering e.g. after onOnLanguageChange occurred
    private static Language mDesiredAppHmiLanguage = Language.EN_US; // Stores the language of the display to be used for next AppInterface registering e.g. after onOnLanguageChange occurred
    // private Double mSpeed = 0.0; // Stores the current vehicle speed
    // private Double mExternalTemperature = 0.0; // Stores the current external temperature
    private DeviceStatus mDeviceStatus = null; // Stores the current device (phone) status
    private InfoType mActiveInfoType = InfoType.NONE; // Stores the current type of information being displayed
    private WeatherLocation mCurrentLocation = null; // Stores the current location for weather
    private WeatherConditions mWeatherConditions = null; // Stores the current weather conditions
    // private RoadConditions mRoadConditions = null; // Stores the current road conditions
    private WeatherAlert[] mAlerts = null; // Stores the current weather alerts
    private WeatherAlert[] mPreviousAlerts = {}; // Stores the last known weather alerts
    private Forecast[] mForecast = null; // Stores the current forecast
    private Forecast[] mHourlyForecast = null; // Stores the current hourly forecast
    private SoftButtonObject mShowConditions = null;
    private SoftButtonObject mShowStandardForecast = null;
    private SoftButtonObject mShowDailyForecast = null;
    private SoftButtonObject mShowHourlyForecast = null;
    private SoftButtonObject mShowAlerts = null;
    private String mShowPrevItemState1Name = "mShowPrevItemState1";
    private String mShowPrevItemState2Name = "mShowPrevItemState2";
    private SoftButtonObject mShowPrevItem = null;
    private String mShowNextItemState1Name = "mShowNextItemState1";
    private String mShowNextItemState2Name = "mShowNextItemState2";
    private SoftButtonObject mShowNextItem = null;
    private SoftButtonObject mShowListItems = null;
    private SoftButtonObject mShowBack = null;
    private ForecastItem[] forecast_items = null;
    private static int forecast_item_counter = 0;
    private Handler mTimedShowHandler = null;
    private ArrayList<String> mUploadedFiles = null;
    private SparseArray<String> mPutFileMap = null;
    private String mConditionIconFileName = null;
    private WeatherDataManager mDataManager = null;
    private Boolean unitsInMetric = true;
    private String tempUnitsShort = "C";
    private String speedUnitsShort = "KPH";
    private String speedUnitsFull = "kilometers per hour";
    private String lengthUnitsFull = "millimeters";
    private Handler mHandler = null;
    private HMILevel currentHMILevel = HMILevel.HMI_NONE;
    private LinkedList<WeatherAlert> mAlertQueue = new LinkedList<WeatherAlert>();
    private int mLastAlertId;
    private int mWelcomeCorrId;
    private boolean mLocationRdy = false;
    private boolean mConditionsRdy = false;
    private boolean mWelcomeComplete = false;
    // Error message tracking bools
    private boolean mFirstConnectionError = true;
    private boolean mFirstLocationError = true;
    private boolean mFirstAPIError = true;
    private boolean mFirstUnknownError = true;
    private LocalizationUtil mLocalizationUtil = null;
    //choice set variables
    private List<ChoiceCell> choiceCellList = null;
    private List<ChoiceCell> changeUnitCellList = null;
    private List<MenuCell> menuCells = null;
    private MenuCell mainCell1 = null;
    private MenuCell mainCell2 = null;
    private MenuCell mainCell3 = null;
    private MenuCell mainCell4 = null;
    private MenuCell mainCell5 = null;

    /**
     * Runnable that stops this service if there hasn't been a connection to SDL
     * within a reasonable amount of time since ACL_CONNECT.
     */
    private Runnable mCheckConnectionRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i(SdlApplication.TAG, "CheckConnectionRunnable");
            Boolean stopService = true;
            // If the proxy has connected to SDL, do NOT stop the service
            if (sdlManager != null && (sdlManager.getState() == BaseSubManager.READY || sdlManager.getState() == BaseSubManager.LIMITED)) {
                stopService = false;
            }
            if (stopService) {
                Log.i(SdlApplication.TAG, "No connection - stopping SmartDeviceLink service");
                mHandler.removeCallbacks(mCheckConnectionRunnable);
                mHandler.removeCallbacks(mStopServiceRunnable);
                SdlApplication app = SdlApplication.getInstance();
                if (app != null) app.stopServices(true);
                stopSelf();
            }
        }
    };

    /**
     * Runnable that stops this service on ACL_DISCONNECT after a short time delay.
     * This is a workaround until some synchronization issues are fixed within the proxy.
     */
    private Runnable mStopServiceRunnable = new Runnable() {
        @Override
        public void run() {
            Log.i(SdlApplication.TAG, "StopServiceRunnable");
            // As long as the proxy is null or not connected to SDL, stop the service
            if (sdlManager == null || sdlManager.getState() == BaseSubManager.SHUTDOWN || sdlManager.getState() == BaseSubManager.ERROR) {
                Log.i(SdlApplication.TAG, "StopServiceRunnable stopping service");
                mHandler.removeCallbacks(mCheckConnectionRunnable);
                mHandler.removeCallbacks(mStopServiceRunnable);
                SdlApplication app = SdlApplication.getInstance();
                if (app != null) app.stopServices(true);
                stopSelf();
            }
        }
    };


    public SdlManager getSdlManager() {
        return sdlManager;
    }

    private class ForecastItem extends Forecast {
        private Integer numberOfForecasts; //TODO we set some of theses itmes, do we use them?
        private String title;
        private String fullDateString;
        private String shortDateString;
        private String dateString;
        private String timeString;
        private String precipChanceStringFull;
        private String precipChanceStringShort;
        private String speakString;
        private String showString;
        private String showString_field1;
        private String showString_field2;
        private String showString_field3;
        private String showString_field4;
        private String showString_mediaTrack;
    }

    private class TimedShowRunnable implements Runnable {

        protected Vector<String> mShowStrings;
        protected List<SoftButtonObject> mSoftButtonObjects;
        protected int mFieldIndex;
        protected int mDelayTime;
        protected int mFieldsLeft;

        public TimedShowRunnable(Vector<String> showStrings, List<SoftButtonObject> softButtonObjects, int index, int delay) {
            this.mShowStrings = showStrings;
            this.mSoftButtonObjects = softButtonObjects;
            this.mFieldIndex = index;
            this.mDelayTime = delay;
        }

        private void performShow() {
            String field1 = null;
            String field2 = null;
            String field3 = null;
            String field4 = null;

            this.mFieldsLeft = this.mShowStrings.size() - this.mFieldIndex;
            if (this.mFieldsLeft > 0) {
                if (mNumberOfTextFields >= 1) {
                    if (this.mFieldsLeft > 0) {
                        field1 = this.mShowStrings.get(this.mFieldIndex);
                        this.mFieldIndex++;
                        this.mFieldsLeft = this.mShowStrings.size() - this.mFieldIndex;
                    } else {
                        field1 = "";
                    }
                }

                if (mNumberOfTextFields >= 2) {
                    if (this.mFieldsLeft > 0) {
                        field2 = this.mShowStrings.get(this.mFieldIndex);
                        this.mFieldIndex++;
                        this.mFieldsLeft = this.mShowStrings.size() - this.mFieldIndex;
                    } else {
                        field2 = "";
                    }
                }
                if (mNumberOfTextFields >= 3) {
                    if (this.mFieldsLeft > 0) {
                        field3 = this.mShowStrings.get(this.mFieldIndex);
                        this.mFieldIndex++;
                        this.mFieldsLeft = this.mShowStrings.size() - this.mFieldIndex;
                    } else {
                        field3 = "";
                    }
                }
                if (mNumberOfTextFields >= 4) {
                    if (this.mFieldsLeft > 0) {
                        field4 = this.mShowStrings.get(this.mFieldIndex);
                        this.mFieldIndex++;
                        this.mFieldsLeft = this.mShowStrings.size() - this.mFieldIndex;
                    } else {
                        field4 = "";
                    }
                }

                sdlManager.getScreenManager().beginTransaction();
                sdlManager.getScreenManager().setSoftButtonObjects(mSoftButtonObjects);
                sdlManager.getScreenManager().setTextField1(field1);
                sdlManager.getScreenManager().setTextField2(field2);
                sdlManager.getScreenManager().setTextField3(field3);
                sdlManager.getScreenManager().setTextField4(field4);
                sdlManager.getScreenManager().setTextAlignment(TextAlignment.LEFT_ALIGNED);
                sdlManager.getScreenManager().setPrimaryGraphic(null);
                sdlManager.getScreenManager().commit(null);
            }
        }

        @Override
        public void run() {
            performShow();
            if (this.mFieldsLeft > 0) {
                mTimedShowHandler.postDelayed(this, this.mDelayTime);
            }
        }
    }

    private TimedShowRunnable mTimedShowRunnable;

    /**
     * Receiver for changes in location from the app UI.
     */
    private final BroadcastReceiver mChangeLocationReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDataManager != null) {
                mCurrentLocation = mDataManager.getCurrentLocation();
                if (!mLocationRdy) mLocationRdy = true;
                if (mActiveInfoType == InfoType.NONE && mConditionsRdy &&
                        mLocationRdy && mWelcomeComplete) {
                    mActiveInfoType = InfoType.WEATHER_CONDITIONS;
                    writeDisplay(false);

                }
            }
        }
    };

    /**
     * Receiver to handle updates to weather conditions.
     */
    private final BroadcastReceiver mWeatherConditionsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDataManager != null) {
                mWeatherConditions = mDataManager.getWeatherConditions();
                if (currentHMILevel.equals(HMILevel.HMI_FULL) &&
                        mActiveInfoType == InfoType.WEATHER_CONDITIONS) {
                    updateHmi(false);
                }
                if (!mConditionsRdy) mConditionsRdy = true;
                if (mActiveInfoType == InfoType.NONE && mConditionsRdy &&
                        mLocationRdy && mWelcomeComplete) {
                    mActiveInfoType = InfoType.WEATHER_CONDITIONS;
                    updateHmi(false);
                }
            }
        }
    };

    /**
     * Receiver to handle updates to weather alerts.
     */
    private final BroadcastReceiver mAlertsReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDataManager != null) {
                mAlerts = mDataManager.getAlerts();
                if (currentHMILevel.equals(HMILevel.HMI_FULL) &&
                        mActiveInfoType == InfoType.ALERTS) {
                    writeDisplay(false);

                } else if (!currentHMILevel.equals(HMILevel.HMI_NONE)) {
                    if (checkNewAlerts()) {
                        performWeatherAlert(mAlertQueue.pop());
                    }
                }

                if (mAlerts != null) {
                    mPreviousAlerts = mAlerts.clone();
                } else {
                    mPreviousAlerts = null;
                }
            }
        }
    };

    /**
     * Receiver to handle updates to the forecast.
     */
    private final BroadcastReceiver mForecastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDataManager != null) {
                mForecast = mDataManager.getForecast();
                if (currentHMILevel.equals(HMILevel.HMI_FULL) &&
                        mActiveInfoType == InfoType.DAILY_FORECAST) {
                    writeDisplay(false);

                } else if (currentHMILevel.equals(HMILevel.HMI_FULL) &&
                        mActiveInfoType == InfoType.STANDARD_FORECAST) {
                    writeDisplay(false);
                }
            }
        }
    };

    /**
     * Receiver to handle updates to the hourly forecast.
     */
    private final BroadcastReceiver mHourlyForecastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDataManager != null) {
                mHourlyForecast = mDataManager.getHourlyForecast();
                if (currentHMILevel.equals(HMILevel.HMI_FULL) &&
                        mActiveInfoType == InfoType.HOURLY_FORECAST) {
                    writeDisplay(false);
                }
            }
        }
    };

    /**
     * Receiver to handle updates in error cases
     */
    private final BroadcastReceiver mErrorReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (mDataManager != null) {
                if (mDataManager.isInErrorState()) {
                    writeDisplay(false);

                } else {
                    resetFirstErrorFlags();
                    writeDisplay(false);
                }
            }
        }
    };

    /**
     * Receiver to handle updates to road conditions.
     */
	// Check if IOS has implemented
	/*private final BroadcastReceiver mRoadConditionsReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
		}
	};*/

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mHandler = new Handler();
        LocalBroadcastManager lbManager = LocalBroadcastManager.getInstance(this);
        lbManager.registerReceiver(mChangeLocationReceiver, new IntentFilter("com.ford.mobileweather.Location"));
        lbManager.registerReceiver(mWeatherConditionsReceiver, new IntentFilter("com.ford.mobileweather.WeatherConditions"));
        lbManager.registerReceiver(mAlertsReceiver, new IntentFilter("com.ford.mobileweather.Alerts"));
        lbManager.registerReceiver(mForecastReceiver, new IntentFilter("com.ford.mobileweather.Forecast"));
        lbManager.registerReceiver(mHourlyForecastReceiver, new IntentFilter("com.ford.mobileweather.HourlyForecast"));
        lbManager.registerReceiver(mErrorReceiver, new IntentFilter("com.ford.mobileweather.ErrorUpdate"));
        // lbManager.registerReceiver(mRoadConditionsReceiver, new IntentFilter("com.ford.mobileweather.RoadConditions"));

        SoftButtonState mShowConditionsState = new SoftButtonState("mShowConditionsState", getResources().getString(R.string.sb1), null);
        mShowConditions = new SoftButtonObject("mShowConditions", Collections.singletonList(mShowConditionsState), mShowConditionsState.getName(), new SoftButtonObject.OnEventListener() {
            @Override
            public void onPress(SoftButtonObject softButtonObject, OnButtonPress onButtonPress) {
                int mtemp_counter = forecast_item_counter;
                mActiveInfoType = InfoType.WEATHER_CONDITIONS;
                forecast_item_counter = mtemp_counter;
                updateHmi(true);
            }

            @Override
            public void onEvent(SoftButtonObject softButtonObject, OnButtonEvent onButtonEvent) {

            }
        });

        SoftButtonState mShowStandardForecastState = new SoftButtonState("mShowStandardForecastState", getResources().getString(R.string.sb2), null);
        mShowStandardForecast = new SoftButtonObject("mShowStandardForecast", Collections.singletonList(mShowStandardForecastState), mShowStandardForecastState.getName(), new SoftButtonObject.OnEventListener() {
            @Override
            public void onPress(SoftButtonObject softButtonObject, OnButtonPress onButtonPress) {
                int mtemp_counter = forecast_item_counter;
                mActiveInfoType = InfoType.STANDARD_FORECAST;
                forecast_item_counter = mtemp_counter;
                updateHmi(true);
            }

            @Override
            public void onEvent(SoftButtonObject softButtonObject, OnButtonEvent onButtonEvent) {

            }
        });

        SoftButtonState mShowDailyForecastState = new SoftButtonState("mShowDailyForecastState", getResources().getString(R.string.sb3), null);
        mShowDailyForecast = new SoftButtonObject("mShowDailyForecast", Collections.singletonList(mShowDailyForecastState), mShowDailyForecastState.getName(), new SoftButtonObject.OnEventListener() {
            @Override
            public void onPress(SoftButtonObject softButtonObject, OnButtonPress onButtonPress) {
                int mtemp_counter = forecast_item_counter;
                mActiveInfoType = InfoType.DAILY_FORECAST;
                forecast_item_counter = mtemp_counter;
                updateHmi(true);
            }

            @Override
            public void onEvent(SoftButtonObject softButtonObject, OnButtonEvent onButtonEvent) {

            }
        });

        SoftButtonState mShowHourlyForecastState = new SoftButtonState("mShowHourlyForecastState", getResources().getString(R.string.sb4), null);
        mShowHourlyForecast = new SoftButtonObject("mShowHourlyForecast", Collections.singletonList(mShowHourlyForecastState), mShowHourlyForecastState.getName(), new SoftButtonObject.OnEventListener() {
            @Override
            public void onPress(SoftButtonObject softButtonObject, OnButtonPress onButtonPress) {
                int mtemp_counter = forecast_item_counter;
                mActiveInfoType = InfoType.HOURLY_FORECAST;
                forecast_item_counter = mtemp_counter;
                updateHmi(true);
            }

            @Override
            public void onEvent(SoftButtonObject softButtonObject, OnButtonEvent onButtonEvent) {

            }
        });

        SoftButtonState mShowAlertsState = new SoftButtonState("mShowStandardForecastState", getResources().getString(R.string.sb5), null);
        mShowAlerts = new SoftButtonObject("mShowStandardForecast", Collections.singletonList(mShowAlertsState), mShowAlertsState.getName(), new SoftButtonObject.OnEventListener() {
            @Override
            public void onPress(SoftButtonObject softButtonObject, OnButtonPress onButtonPress) {
                int mtemp_counter = forecast_item_counter;
                mActiveInfoType = InfoType.ALERTS;
                forecast_item_counter = mtemp_counter;
                updateHmi(true);
            }

            @Override
            public void onEvent(SoftButtonObject softButtonObject, OnButtonEvent onButtonEvent) {

            }
        });

        SoftButtonState mShowPrevItemState1 = new SoftButtonState(mShowPrevItemState1Name, getResources().getString(R.string.sb1_prev), null);
        SoftButtonState mShowPrevItemState2 = new SoftButtonState(mShowPrevItemState2Name, "-", null);
        mShowPrevItem = new SoftButtonObject("mShowPrevItem", Arrays.asList(mShowPrevItemState1, mShowPrevItemState2), mShowPrevItemState2.getName(), new SoftButtonObject.OnEventListener() {
            @Override
            public void onPress(SoftButtonObject softButtonObject, OnButtonPress onButtonPress) {
                int mtemp_counter = forecast_item_counter;
                mtemp_counter--;
                if (mtemp_counter >= 0) {
                    forecast_item_counter = mtemp_counter;
                    writeDisplay(true);
                } else {
                    speak("You have reached the beginning of the forecast list", autoIncCorrId++);
                }
            }

            @Override
            public void onEvent(SoftButtonObject softButtonObject, OnButtonEvent onButtonEvent) {

            }
        });

        SoftButtonState mShowNextItemState1 = new SoftButtonState(mShowNextItemState1Name, getResources().getString(R.string.sb2_next), null);
        SoftButtonState mShowNextItemState2 = new SoftButtonState(mShowNextItemState2Name, "-", null);
        mShowNextItem = new SoftButtonObject("mShowNextItem", Arrays.asList(mShowNextItemState1, mShowNextItemState2), mShowNextItemState1.getName(), new SoftButtonObject.OnEventListener() {
            @Override
            public void onPress(SoftButtonObject softButtonObject, OnButtonPress onButtonPress) {
                int mtemp_counter = forecast_item_counter;
                mtemp_counter++;
                if (mtemp_counter < forecast_items.length) {
                    forecast_item_counter = mtemp_counter;
                    writeDisplay(true);
                }
                if (mtemp_counter >= forecast_items.length) {
                    speak("You have reached the end of the forecast list", autoIncCorrId++);
                }
            }

            @Override
            public void onEvent(SoftButtonObject softButtonObject, OnButtonEvent onButtonEvent) {

            }
        });

        SoftButtonState mShowListItemsState = new SoftButtonState("mShowListItemsState", getResources().getString(R.string.sb3_list), null);
        mShowListItems = new SoftButtonObject("mShowListItems", Collections.singletonList(mShowListItemsState), mShowListItemsState.getName(), new SoftButtonObject.OnEventListener() {
            @Override
            public void onPress(SoftButtonObject softButtonObject, OnButtonPress onButtonPress) {
                forecast_item_counter = 0;
                String choiceSetText = "";
                if (mActiveInfoType == InfoType.HOURLY_FORECAST) {
                    choiceSetText = "Please select a Time:";
                } else {
                    choiceSetText = "Please select a day:";
                }
                ChoiceSet choiceSet = new ChoiceSet(choiceSetText, choiceCellList, new ChoiceSetSelectionListener() {
                    @Override
                    public void onChoiceSelected(ChoiceCell choiceCell, TriggerSource triggerSource, int rowIndex) {
                        forecast_item_counter = rowIndex;
                        writeDisplay(true);
                    }

                    @Override
                    public void onError(String error) {

                    }
                });
                choiceSet.setLayout(ChoiceSetLayout.CHOICE_SET_LAYOUT_TILES);
                sdlManager.getScreenManager().presentChoiceSet(choiceSet, InteractionMode.MANUAL_ONLY);
            }

            @Override
            public void onEvent(SoftButtonObject softButtonObject, OnButtonEvent onButtonEvent) {

            }
        });

        SoftButtonState mShowBackState = new SoftButtonState("mShowBackState", getResources().getString(R.string.sb3_back), null);
        mShowBack = new SoftButtonObject("mShowBack", Collections.singletonList(mShowBackState), mShowBackState.getName(), new SoftButtonObject.OnEventListener() {
            @Override
            public void onPress(SoftButtonObject softButtonObject, OnButtonPress onButtonPress) {
                mActiveInfoType = InfoType.WEATHER_CONDITIONS;
                forecast_item_counter = 0;
                // TODO not sure if createMenuCells is needed anymore here.
                createMenuCells();
                updateHmi(false);
            }

            @Override
            public void onEvent(SoftButtonObject softButtonObject, OnButtonEvent onButtonEvent) {

            }
        });

        if (!AbbreviationDictionary.isPrepared())
            AbbreviationDictionary.loadDictionary(this);
        setUnitsMetric();

        mTimedShowHandler = new Handler();
        mDataManager = WeatherDataManager.getInstance();
        mLocalizationUtil = LocalizationUtil.getInstance();

        mUploadedFiles = new ArrayList<String>();
        mPutFileMap = new SparseArray<String>();

        // Initialize weather and location data
        mCurrentLocation = mDataManager.getCurrentLocation();
        mWeatherConditions = mDataManager.getWeatherConditions();
        mAlerts = mDataManager.getAlerts();
        mForecast = mDataManager.getForecast();
        mHourlyForecast = mDataManager.getHourlyForecast();

        // See if the location and current conditions are already available
        mLocationRdy = (mCurrentLocation != null);
        mConditionsRdy = (mWeatherConditions != null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterForeground();
        }
    }

    @SuppressLint("NewApi")
    private void enterForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("MobileWeather", "SmartDeviceLink", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Notification serviceNotification = new Notification.Builder(this, channel.getId())
                        .setContentTitle("SmartDeviceLink")
                        .setSmallIcon(R.drawable.ic_sdl)
                        .setTicker("SmartDeviceLink")
                        .build();
                startForeground(FOREGROUND_SERVICE_ID, serviceNotification);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Remove any previous stop service runnables that could be from a recent ACL Disconnect
        mHandler.removeCallbacks(mStopServiceRunnable);

        if (intent != null) {
            mBtAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBtAdapter != null) {
                if (mBtAdapter.isEnabled()) {
                    //Check if this was started with a flag to force a transport connect
                    startProxy();
                }
            }
        }

        // Queue the check connection runnable to stop the service if no connection is made
        mHandler.removeCallbacks(mCheckConnectionRunnable);
        mHandler.postDelayed(mCheckConnectionRunnable, CONNECTION_TIMEOUT);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        shutdown();

        // Stop service background mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void shutdown() {
        disposeSyncProxy();
        instance = null;
        try {
            LocalBroadcastManager lbManager = LocalBroadcastManager.getInstance(this);
            lbManager.unregisterReceiver(mChangeLocationReceiver);
            lbManager.unregisterReceiver(mWeatherConditionsReceiver);
            lbManager.unregisterReceiver(mAlertsReceiver);
            lbManager.unregisterReceiver(mForecastReceiver);
            lbManager.unregisterReceiver(mHourlyForecastReceiver);
            lbManager.unregisterReceiver(mErrorReceiver);
            // lbManager.unregisterReceiver(mRoadConditionsReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    public static SdlService getInstance() {
        return instance;
    }

    /**
     * Queue's a runnable that stops the service after a small delay,
     * unless the proxy reconnects to SDL.
     */
    public void stopService() {
        mHandler.removeCallbacks(mStopServiceRunnable);
        mHandler.postDelayed(mStopServiceRunnable, STOP_SERVICE_DELAY);
    }

    public void startProxy() {
        if (sdlManager == null) {
            Log.i(TAG, "Starting SDL Proxy");
            BaseTransportConfig transport = new MultiplexTransportConfig(getBaseContext(), APP_ID, MultiplexTransportConfig.FLAG_MULTI_SECURITY_OFF);

            // The manager listener helps you know when certain events that pertain to the SDL Manager happen
            // Here we will listen for ON_HMI_STATUS and ON_COMMAND notifications
            SdlManagerListener listener = new SdlManagerListener() {
                @Override
                public void onStart() {
                    sdlManager.addOnRPCNotificationListener(FunctionID.ON_VEHICLE_DATA, new OnRPCNotificationListener() {
                        @Override
                        public void onNotified(RPCNotification notification) {

                            OnVehicleData onVehicleData = (OnVehicleData) notification;

                            // mSpeed = notification.getSpeed();
                            // mExternalTemperature = notification.getExternalTemperature();
                            mDeviceStatus = onVehicleData.getDeviceStatus();

                            // TODO: act on these

                            // Stop the background weather updates when roaming
                            boolean roaming = mDeviceStatus.getPhoneRoaming();
                            Intent intent = new Intent("com.ford.mobileweather.RoamingStatus");
                            intent.putExtra("roaming", roaming);
                            LocalBroadcastManager.getInstance(SdlService.this).sendBroadcast(intent);
                        }
                    });

                    sdlManager.addOnRPCNotificationListener(FunctionID.ON_BUTTON_PRESS, new OnRPCNotificationListener() {
                        @Override
                        public void onNotified(RPCNotification notification) {
                            OnButtonPress onButtonPress = (OnButtonPress) notification;
                            switch (onButtonPress.getButtonName()) {
                                case PRESET_1:
                                    updateHmi(true);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });

                    sdlManager.addOnRPCNotificationListener(FunctionID.ON_LANGUAGE_CHANGE, new OnRPCNotificationListener() {
                        @Override
                        public void onNotified(RPCNotification notification) {
                            OnLanguageChange onLanguageChange = (OnLanguageChange) notification;
                            mDesiredAppSdlLanguage = onLanguageChange.getLanguage();
                            mDesiredAppHmiLanguage = onLanguageChange.getHmiDisplayLanguage();
                            Log.i(SdlApplication.TAG, "onOnLanguageChange: Language = " + mDesiredAppSdlLanguage);
                            Log.i(SdlApplication.TAG, "onOnLanguageChange: HmiDisplayLanguage = " + mDesiredAppHmiLanguage);
                        }
                    });
                }

                @Override
                public void onDestroy() {
                    mFirstHmiNone = true;
                    mActiveInfoType = InfoType.NONE;
                    mLocalizationUtil.changeLocale(mLocalizationUtil.getLocaleLanguage(), mLocalizationUtil.getLocaleCountry(), getApplicationContext());
                    SdlService.this.stopSelf();
                }

                @Override
                public void onError(String info, Exception e) {
                }

                @Override
                public LifecycleConfigurationUpdate managerShouldUpdateLifecycle(Language language) {
                    return null;
                }
            };

            // Create App Icon, this is set in the SdlManager builder
            SdlArtwork appIcon = new SdlArtwork(APP_ICON, FileType.GRAPHIC_PNG, R.drawable.icon, true);
            //sets up LockScreen
            LockScreenConfig lockScreenConfig = new LockScreenConfig();
            lockScreenConfig.setCustomView(R.layout.lockscreen);
            lockScreenConfig.setDisplayMode(LockScreenConfig.DISPLAY_MODE_ALWAYS);
            // The manager builder sets options for your session
            SdlManager.Builder builder = new SdlManager.Builder(this, APP_ID, "MobileWeather", listener);
            builder.setTransportType(transport);
            builder.setLanguage(mDesiredAppSdlLanguage);
            builder.setAppIcon(appIcon);
            builder.setLockScreenConfig(lockScreenConfig);
            // Set listeners list
            Map<FunctionID, OnRPCNotificationListener> onRPCNotificationListenerMap = new HashMap<>();
            onRPCNotificationListenerMap.put(FunctionID.ON_HMI_STATUS, new OnRPCNotificationListener() {
                @Override
                public void onNotified(RPCNotification notification) {
                    OnHMIStatus onHMIStatus = (OnHMIStatus) notification;

                    currentHMILevel = onHMIStatus.getHmiLevel();

                    switch (onHMIStatus.getSystemContext()) {
                        case SYSCTXT_MAIN:
                            break;
                        case SYSCTXT_VRSESSION:
                            break;
                        case SYSCTXT_MENU:
                            break;
                        default:
                            return;
                    }

                    switch (onHMIStatus.getAudioStreamingState()) {
                        case AUDIBLE:
                            // play audio if applicable
                            break;
                        case NOT_AUDIBLE:
                            // pause/stop/mute audio if applicable
                            break;
                        default:
                            return;
                    }

                    switch (currentHMILevel) {
                        case HMI_FULL:
                            Log.i(SdlApplication.TAG, "HMI_FULL");
                            mLocalizationUtil.changeLocale(mLocalizationUtil.getAdjustedLocaleLanguage(), mLocalizationUtil.getAdjustedLocaleCountry(), getApplicationContext());
                            if (onHMIStatus.getFirstRun()) {
                                // Custom help and timeout messages
                                setGlobalProperties((getResources().getString(R.string.gp_help_prompt)), (getResources().getString(R.string.gp_timeout_prompt)), autoIncCorrId++);

                                // Perform welcome
                                showWelcomeMessage();

                                // Create InteractionChoicedSet for changing units
                                createChangeUnitsInteractionChoiceSet();

                                // Subscribe buttons
                                subscribeButtons();
                            }
                            break;
                        case HMI_LIMITED:
                            Log.i(SdlApplication.TAG, "HMI_LIMITED");
                            break;
                        case HMI_BACKGROUND:
                            Log.i(SdlApplication.TAG, "HMI_BACKGROUND");
                            if (mFirstHmiNone) {
                                getSdlSettings();
                            }
                            break;
                        case HMI_NONE:
                            Log.i(SdlApplication.TAG, "HMI_NONE");
                            if (mFirstHmiNone) {
                                getSdlSettings();
                            } else {
                                // write back the original locales of the app
                                mLocalizationUtil.changeLocale(mLocalizationUtil.getLocaleLanguage(), mLocalizationUtil.getLocaleCountry(), getApplicationContext());
                            }
                            break;
                    }
                }
            });
            builder.setRPCNotificationListeners(onRPCNotificationListenerMap);
            sdlManager = builder.build();
            sdlManager.start();

            mRegisteredAppSdlLanguage = mDesiredAppSdlLanguage;
            mRegisteredAppHmiLanguage = mDesiredAppHmiLanguage;
        }
    }

    public void disposeSyncProxy() {
        if (sdlManager != null) {
            sdlManager.dispose();
            sdlManager = null;
        }
    }

    public void reset() {
        mFirstHmiNone = true;
        mActiveInfoType = InfoType.NONE;
        if (proxy != null) {
//			try {
//				proxy.resetProxy();
//			} catch (SdlException e1) {
//				e1.printStackTrace();
//				// something goes wrong, & the proxy returns as null, stop the service.
//				// do not want a running service with a null proxy
//				if (proxy == null) {
//					stopSelf();
//				}
//			}
        } else {
            startProxy();
        }
    }

    /**
     * Shows and speaks a welcome message
     */
    private void showWelcomeMessage() {

      sdlManager.getScreenManager().beginTransaction();
      sdlManager.getScreenManager().setTextField1("Welcome to");
      sdlManager.getScreenManager().setTextField2("MobileWeather");
      sdlManager.getScreenManager().setTextField3("");
      sdlManager.getScreenManager().setTextField4("");
      sdlManager.getScreenManager().setTextAlignment(TextAlignment.CENTERED);
        sdlManager.getScreenManager().commit(new CompletionListener() {
            @Override
            public void onComplete(boolean success) {
                Log.i(TAG, "ScreenManager update complete: " + success);
                createMenuCells();

            }
        });
        Speak msg = new Speak(TTSChunkFactory.createSimpleTTSChunks((getResources().getString(R.string.welcome_speak))));
        msg.setCorrelationID(mWelcomeCorrId);
        msg.setOnRPCResponseListener(new OnRPCResponseListener() {
            @Override
            public void onResponse(int correlationId, RPCResponse response) {
                if (response.getCorrelationID() == mWelcomeCorrId) {
                    if (mActiveInfoType == InfoType.NONE && mConditionsRdy && mLocationRdy) {
                        if (mAlerts != null) {
                            if (checkNewAlerts()) {
                                performWeatherAlert(mAlertQueue.pop());
                            } else {
                                mActiveInfoType = InfoType.WEATHER_CONDITIONS;
                                updateHmi(true);
                            }
                        } else {
                            mActiveInfoType = InfoType.WEATHER_CONDITIONS;
                            updateHmi(true);
                        }
                    }
                }
            }
        });
        sdlManager.sendRPC(msg);
    }

    private void getSdlSettings() {
        mActiveInfoType = InfoType.NONE;
        getSdlFiles();
        // Change registration to match the language of the head unit if needed
        mCurrentHmiLanguage = sdlManager.getRegisterAppInterfaceResponse().getHmiDisplayLanguage();
        mCurrentSdlLanguage = sdlManager.getRegisterAppInterfaceResponse().getLanguage();

        if (mCurrentHmiLanguage != null && mCurrentSdlLanguage != null) {
            if ((mCurrentHmiLanguage.compareTo(mRegisteredAppHmiLanguage) != 0) ||
                    (mCurrentSdlLanguage.compareTo(mRegisteredAppSdlLanguage) != 0)) {
                // determine to which locale the phone should be switched, register on Sync
                mLocalizationUtil.determineLocale(mCurrentSdlLanguage);

                ChangeRegistration msg = new ChangeRegistration(mCurrentSdlLanguage, mCurrentHmiLanguage);
                msg.setCorrelationID(autoIncCorrId++);
                msg.setOnRPCResponseListener(new OnRPCResponseListener() {
                    @Override
                    public void onResponse(int correlationId, RPCResponse response) {
                        if ((response.getResultCode().equals(Result.SUCCESS)) && (response.getSuccess())) {
                            //store the registered language if ChangeRegistration has been successful
                            Log.i(SdlApplication.TAG, "ChangeRegistrationResponse: SUCCESS");
                            mRegisteredAppSdlLanguage = mCurrentSdlLanguage;
                            mRegisteredAppHmiLanguage = mCurrentHmiLanguage;
                        }
                    }
                });
                sdlManager.sendRPC(msg);


            }
        }

        // TODO: Save units in preferences
        String units = null;
        //
        if (mCurrentHmiLanguage != null) {
            if (mCurrentHmiLanguage.compareTo(Language.EN_US) == 0 ||
                    mCurrentHmiLanguage.compareTo(Language.EN_GB) == 0) {
                units = setUnitsImp();
            } else {
                units = setUnitsMetric();
            }
        }
        Intent intent = new Intent("com.ford.mobileweather.Units");
        if (mDataManager != null) {
            mDataManager.setUnits(units);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }

        // Get the display capabilities
        DisplayCapabilities displayCapabilities = (DisplayCapabilities) sdlManager.getSystemCapabilityManager().getCapability(SystemCapabilityType.DISPLAY);
        if (displayCapabilities != null) {
            mDisplayType = displayCapabilities.getDisplayType();

            Boolean gSupport = displayCapabilities.getGraphicSupported();
            // Gen 1.0 will return NULL instead of not supported.
            if (gSupport != null) {
                mGraphicsSupported = gSupport.booleanValue();
            } else {
                mGraphicsSupported = false;
            }

            if (displayCapabilities.getTextFields() != null)
                mTextFields = new ArrayList<TextField>(displayCapabilities.getTextFields());

            ArrayList<String> templates = null;
            if (displayCapabilities.getTemplatesAvailable() != null)
                templates = new ArrayList<String>(displayCapabilities.getTemplatesAvailable());


            mDisplayLayoutSupported = false;
            if (templates != null && templates.contains("NON-MEDIA")) {
                mDisplayLayoutSupported = true;
            }

            if (mDisplayType == DisplayType.CID) {
                mNumberOfTextFields = 2;
            } else if (mDisplayType == DisplayType.GEN3_8_INCH) {
                mNumberOfTextFields = 3;
            } else if (mDisplayType == DisplayType.MFD3 ||
                    mDisplayType == DisplayType.MFD4 ||
                    mDisplayType == DisplayType.MFD5) {
                mNumberOfTextFields = 2;
            } else if (mDisplayType == DisplayType.NGN) {
                mNumberOfTextFields = 1;
            } else {
                mNumberOfTextFields = 1;
            }

            if (mTextFields != null && mTextFields.size() > 0) {
                for (TextField field : mTextFields) {
                    if (field.getName() == TextFieldName.mainField1) {
                        // TODO: Workaround needed for GEN3 show issues
                        if (mDisplayType == DisplayType.GEN3_8_INCH) {
                            mLengthOfTextFields = 42;
                        } else {
                            mLengthOfTextFields = field.getWidth();
                        }
                        Log.i(SdlApplication.TAG, String.format(Locale.getDefault(), "MainField Length: %d", mLengthOfTextFields));
                    }
                }
            }
        }

        if (mDisplayLayoutSupported) {
            SetDisplayLayout layoutRequest = new SetDisplayLayout();
            layoutRequest.setDisplayLayout("NON-MEDIA");
            layoutRequest.setCorrelationID(autoIncCorrId++);
            sdlManager.sendRPC(layoutRequest);
        }


        mFirstHmiNone = false;
    }

    private String setUnitsImp() {
        String units;
        unitsInMetric = false;
        tempUnitsShort = getResources().getString(R.string.units_imp_temp_short);
        speedUnitsShort = getResources().getString(R.string.units_imp_speed_short);
        speedUnitsFull = getResources().getString(R.string.units_imp_speed_full);
        lengthUnitsFull = getResources().getString(R.string.units_imp_length_full);
        units = getResources().getString(R.string.units_imperial);
        return units;
    }

    private String setUnitsMetric() {
        String units;
        unitsInMetric = true;
        tempUnitsShort = getResources().getString(R.string.units_metric_temp_short);
        speedUnitsShort = getResources().getString(R.string.units_metric_speed_short);
        speedUnitsFull = getResources().getString(R.string.units_metric_speed_full);
        lengthUnitsFull = getResources().getString(R.string.units_metric_length_full);
        units = getResources().getString(R.string.units_metric);
        return units;
    }

    /**
     * Creates and displays the menu
     */
    private void createMenuCells() {
        menuCells = null;
        Vector<String> vrCommands = null;

        vrCommands = new Vector<>(Arrays.asList(getResources().getString(R.string.vr_current), getResources().getString(R.string.vr_current_cond)));

        mainCell1 = new MenuCell(getResources().getString(R.string.cmd_current_cond), null, vrCommands, new MenuSelectionListener() {
            @Override
            public void onTriggered(TriggerSource trigger) {
                mActiveInfoType = InfoType.WEATHER_CONDITIONS;
                updateHmi(true);
            }
        });

        vrCommands = new Vector<>(Arrays.asList(getResources().getString(R.string.vr_daily),
                getResources().getString(R.string.vr_daily_forecast)));

        mainCell2 = new MenuCell(getResources().getString(R.string.cmd_daily_forecast), null, vrCommands, new MenuSelectionListener() {
            @Override
            public void onTriggered(TriggerSource trigger) {
                mActiveInfoType = InfoType.DAILY_FORECAST;
                updateHmi(true);
            }
        });

        vrCommands = new Vector<>(Arrays.asList(getResources().getString(R.string.vr_hourly),
                getResources().getString(R.string.vr_hourly_forecast)));

        mainCell3 = new MenuCell(getResources().getString(R.string.cmd_hourly_forecast), null, vrCommands, new MenuSelectionListener() {
            @Override
            public void onTriggered(TriggerSource trigger) {
                mActiveInfoType = InfoType.HOURLY_FORECAST;
                updateHmi(true);
            }
        });

        vrCommands = new Vector<>(Arrays.asList(getResources().getString(R.string.vr_change_units),
                getResources().getString(R.string.vr_units)));

        mainCell4 = new MenuCell(getResources().getString(R.string.cmd_change_units), null, vrCommands, new MenuSelectionListener() {
            @Override
            public void onTriggered(TriggerSource trigger) {
                ChoiceSet changeUnitChoiceSet = new ChoiceSet("Units:", changeUnitCellList, new ChoiceSetSelectionListener() {
                    @Override
                    public void onChoiceSelected(ChoiceCell choiceCell, TriggerSource triggerSource, int rowIndex) {
                        if (choiceCell.getText().equals("Imperial")) {
                            setUnitsImp();
                        } else {
                            setUnitsMetric();
                        }
                        updateHmi(true);
                    }

                    @Override
                    public void onError(String error) {

                    }
                });
                changeUnitChoiceSet.setLayout(ChoiceSetLayout.CHOICE_SET_LAYOUT_TILES);
                sdlManager.getScreenManager().presentChoiceSet(changeUnitChoiceSet, InteractionMode.MANUAL_ONLY);
            }
        });
        vrCommands = new Vector<>(Arrays.asList(getResources().getString(R.string.vr_alerts)));
        mainCell5 = new MenuCell(getResources().getString(R.string.cmd_alerts), null, vrCommands, new MenuSelectionListener() {
            @Override
            public void onTriggered(TriggerSource trigger) {
                Log.i("Julian", "onTriggered: Alerts");
                mActiveInfoType = InfoType.ALERTS;
                updateHmi(true);
            }
        });
        menuCells = Arrays.asList(mainCell1, mainCell2, mainCell3, mainCell4, mainCell5);
        sdlManager.getScreenManager().setMenu(menuCells);
    }

    /**
     * pre loads  the choiceset for changing units
     */
    private void createChangeUnitsInteractionChoiceSet() {
        changeUnitCellList = null;
        ChoiceCell changUnitCell1 = new ChoiceCell("Metric", new Vector<>(Arrays.asList(getResources().getString(R.string.choice_metric_vr))), null);
        ChoiceCell changUnitCell2 = new ChoiceCell("Imperial", new Vector<>(Arrays.asList(getResources().getString(R.string.choice_imperial_vr))), null);
        changeUnitCellList = Arrays.asList(changUnitCell1, changUnitCell2);
        sdlManager.getScreenManager().preloadChoices(changeUnitCellList, null);
    }

    //TODO why is this here
    private void subscribeButtons() {
        SubscribeButton msg = new SubscribeButton(ButtonName.PRESET_1);
        msg.setCorrelationID(autoIncCorrId++);
        sdlManager.sendRPC(msg);
    }

    //TODO why
    private void getSdlFiles() {
        final ListFiles request = new ListFiles();
        request.setCorrelationID(autoIncCorrId++);
        request.setOnRPCResponseListener(new OnRPCResponseListener() {
            @Override
            public void onResponse(int correlationId, RPCResponse response) {
                ListFilesResponse listFilesResponse = (ListFilesResponse) response;
                if (listFilesResponse != null && listFilesResponse.getFilenames() != null) {
                    mUploadedFiles = new ArrayList<String>(listFilesResponse.getFilenames());
                }
            }
        });
        sdlManager.sendRPC(request);
    }

    /**
     * Display the current weather conditions.
     */
    private void showWeatherConditions(boolean includeSpeak) {
        mTimedShowHandler.removeCallbacks(mTimedShowRunnable);
        if (mDataManager.isInErrorState()) {
            showWeatherError();
        } else if (mWeatherConditions != null) {
            // Weather conditions are available and we are not in an error state
            // so reset the error boolean flags
            resetFirstErrorFlags();

            float temperature = mWeatherConditions.temperature;
            float windSpeed = mWeatherConditions.windSpeed;
            float precipitation = mWeatherConditions.precipitation;
            float humidity = mWeatherConditions.humidity;
            String title = mWeatherConditions.conditionTitle;
            String locationString = "";
            String mappedName = null;
            conditionsID = 0;
            if (mCurrentLocation != null) {
                if (mCurrentLocation.city != null) {
                    locationString = mCurrentLocation.city;
                }
                if (mCurrentLocation.state != null) {
                    if (mCurrentLocation.city != null) {
                        locationString += ", " + mCurrentLocation.state;
                    } else {
                        locationString = mCurrentLocation.state;
                    }
                }
            }

            if (!unitsInMetric) {
                temperature = UnitConverter.convertTemperatureToImperial(temperature);
                windSpeed = UnitConverter.convertSpeedToImperial(windSpeed);
                precipitation = UnitConverter.convertLengthToImperial(precipitation);
            }
            if (mGraphicsSupported && mWeatherConditions.conditionIcon != null) {
                String imageName = ImageProcessor.getFileFromURL(mWeatherConditions.conditionIcon);
                mappedName = ImageProcessor.getMappedConditionsImageName(imageName, false);

                if (mappedName != null) {
                    mConditionIconFileName = mappedName + ".png";
                    conditionsID = getResources().getIdentifier(mappedName, "drawable", getPackageName());
                    Log.i(SdlApplication.TAG, "Conditions file: " + mConditionIconFileName);
                }
            }

            String field1;
            String field2 = "";
            String field3 = "";
            String field4 = "";
            String mediatrack = ((int) precipitation) + "%";

            if (mNumberOfTextFields < 2) {
                field1 = String.format(Locale.getDefault(), "%s", abbreviate(title)) +
                        String.format(Locale.getDefault(), "%.0f %s", temperature, tempUnitsShort);
            } else if (mNumberOfTextFields < 3) {
                field1 = String.format(Locale.getDefault(), "%s", title);
                if (mDisplayType == DisplayType.CID || mDisplayType == DisplayType.NGN) {
                    field2 = String.format(Locale.getDefault(), "%.0f %s", temperature, tempUnitsShort);
                } else {
                    field2 = String.format(Locale.getDefault(), "%.0f\u00B0%s", temperature, tempUnitsShort);
                }
            } else {
                field1 = String.format(Locale.getDefault(), "%s", locationString);
                field2 = String.format(Locale.getDefault(), "%s", title);
                field3 = String.format(Locale.getDefault(), "%.0f\u00B0%s, %.0f%%, %.0f %s", temperature, tempUnitsShort, humidity, windSpeed, speedUnitsShort);
            }

            sdlManager.getScreenManager().beginTransaction();
            sdlManager.getScreenManager().setTextField1(field1);
            sdlManager.getScreenManager().setTextField2(field2);
            sdlManager.getScreenManager().setTextField3(field3);
            sdlManager.getScreenManager().setTextField4(field4);
            sdlManager.getScreenManager().setMediaTrackTextField(mediatrack);
            sdlManager.getScreenManager().setTextAlignment(TextAlignment.LEFT_ALIGNED);
            sdlManager.getScreenManager().setPrimaryGraphic(new SdlArtwork(mConditionIconFileName, FileType.GRAPHIC_PNG, conditionsID, true));

            if (mDisplayType != DisplayType.CID && mDisplayType != DisplayType.NGN) {
                Log.d(SdlApplication.TAG, "Sending soft buttons");
                List<SoftButtonObject> softButtonObjects = new ArrayList<>();
                softButtonObjects.add(mShowConditions);
                softButtonObjects.add(mShowDailyForecast);
                softButtonObjects.add(mShowHourlyForecast);
                sdlManager.getScreenManager().setSoftButtonObjects(softButtonObjects);
            }
            sdlManager.getScreenManager().commit(new CompletionListener() {
                @Override
                public void onComplete(boolean success) {
                    Log.i(TAG, "ScreenManager update complete: " + success);

                }
            });
            if (includeSpeak) {
                String speakString;
                Vector<TTSChunk> chunks = new Vector<TTSChunk>();
                TTSChunk chunk = new TTSChunk();
                if (temperature <= -1) {
                    speakString = String.format(Locale.getDefault(),
                            getResources().getString(R.string.weather_conditions_neg_temp_speak),
                            title, temperature * -1, humidity, windSpeed, speedUnitsFull);
                } else {
                    speakString = String.format(Locale.getDefault(),
                            getResources().getString(R.string.weather_conditions_speak),
                            title, temperature, humidity, windSpeed, speedUnitsFull);
                }
                chunk.setText(speakString);
                chunk.setType(SpeechCapabilities.TEXT);
                chunks.add(chunk);
                Speak speakRequest = new Speak();
                speakRequest.setTtsChunks(chunks);
                speakRequest.setCorrelationID(autoIncCorrId++);
                sdlManager.sendRPC(speakRequest);
            }
        } else {
            showNoConditionsAvail();
        }
    }

    private void resetFirstErrorFlags() {
        mFirstConnectionError = true;
        mFirstLocationError = true;
        mFirstAPIError = true;
        mFirstUnknownError = true;
    }

    private void showNoConditionsAvail() {
        sdlManager.getScreenManager().beginTransaction();
        sdlManager.getScreenManager().setTextField1(getResources().getString(R.string.conditions_txt_field1));
        sdlManager.getScreenManager().setTextField2(getResources().getString(R.string.conditions_txt_field2));
        sdlManager.getScreenManager().setTextField3(getResources().getString(R.string.conditions_txt_field3));
        sdlManager.getScreenManager().setTextField4(getResources().getString(R.string.conditions_txt_field4));
        sdlManager.getScreenManager().setTextAlignment(TextAlignment.CENTERED);
        sdlManager.getScreenManager().commit(new CompletionListener() {
            @Override
            public void onComplete(boolean success) {
                Log.i(TAG, "ScreenManager update complete: " + success);

            }
        });
        if (mFirstUnknownError) {
            speak(getResources().getString(R.string.conditions_speak), autoIncCorrId++);
            mFirstUnknownError = false;
        }
    }

    private void showWeatherError() {
        sdlManager.getScreenManager().beginTransaction();
        sdlManager.getScreenManager().setTextField3(getResources().getString(R.string.network_txt_field3));
        sdlManager.getScreenManager().setTextField4(getResources().getString(R.string.network_txt_field4));
        sdlManager.getScreenManager().setTextAlignment(TextAlignment.CENTERED);
        String errorTTSStr = null;
        String field3 = "";
        String field4 = "";

        if (!mDataManager.isNetworkAvailable()) {
            sdlManager.getScreenManager().setTextField1(getResources().getString(R.string.network_txt_field1));
            sdlManager.getScreenManager().setTextField2(getResources().getString(R.string.network_txt_field2));
            if (mFirstConnectionError) {
                errorTTSStr = getResources().getString(R.string.network_speak);
                mFirstConnectionError = false;
            }
        } else if (!mDataManager.isLocationAvailable()) {
            sdlManager.getScreenManager().setTextField1(getResources().getString(R.string.location_txt_field1));
            sdlManager.getScreenManager().setTextField2(getResources().getString(R.string.location_txt_field2));
            if (mFirstLocationError) {
                errorTTSStr = getResources().getString(R.string.location_speak);
                mFirstLocationError = false;
            }
        } else if (!mDataManager.isAPIAvailable()) {
            sdlManager.getScreenManager().setTextField1(getResources().getString(R.string.weather_api_txt_field1));
            sdlManager.getScreenManager().setTextField2(getResources().getString(R.string.weather_api_txt_field2));
            if (mFirstAPIError) {
                errorTTSStr = getResources().getString(R.string.weather_api_speak);
            }
        } else {
            // No error state detected. Return to cancel error state
            // show and speak.
            return;
        }
        sdlManager.getScreenManager().setTextField3(field3);
        sdlManager.getScreenManager().setTextField4(field4);
        sdlManager.getScreenManager().commit(new CompletionListener() {
            @Override
            public void onComplete(boolean success) {
                Log.i(TAG, "ScreenManager update complete: " + success);

            }
        });
        speak(errorTTSStr, autoIncCorrId++);
    }

    private void writeDisplay(boolean includeSpeak) {
        String field1 = "";
        String field2 = "";
        String field3 = "";
        String field4 = "";
        String mediatrack = "";

        if (forecast_item_counter == 0) {
            mShowPrevItem.transitionToStateByName(mShowPrevItemState2Name);
        } else {
            mShowPrevItem.transitionToStateByName(mShowPrevItemState1Name);
        }
        if (((mActiveInfoType == InfoType.DAILY_FORECAST) && (forecast_item_counter == DAILY_FORECAST_DAYS - 1)) ||
                ((mActiveInfoType == InfoType.HOURLY_FORECAST) && (forecast_item_counter == HOURLY_FORECAST_HOURS - 1))) {
            mShowNextItem.transitionToStateByName(mShowNextItemState2Name);
        } else {
            mShowNextItem.transitionToStateByName(mShowNextItemState1Name);
        }

        sdlManager.getScreenManager().beginTransaction();
        List<SoftButtonObject> softButtonObjects = new ArrayList<>();
        softButtonObjects.add(mShowPrevItem);
        softButtonObjects.add(mShowNextItem);
        softButtonObjects.add(mShowBack);
        softButtonObjects.add(mShowListItems);

        if (forecast_items != null) {
            field1 = forecast_items[forecast_item_counter].showString_field1;
            field2 = forecast_items[forecast_item_counter].showString_field2;
            mediatrack = forecast_items[forecast_item_counter].precipitationChance.toString() + "%";
        }

        sdlManager.getScreenManager().setTextField1(field1);
        sdlManager.getScreenManager().setTextField2(field2);
        sdlManager.getScreenManager().setTextField3(field3);
        sdlManager.getScreenManager().setTextField4(field4);
        sdlManager.getScreenManager().setMediaTrackTextField(mediatrack);

        String mappedName = null;
        conditionsID = 0;
        if (mGraphicsSupported && mWeatherConditions.conditionIcon != null && forecast_items != null) {
            String imageName = ImageProcessor.getFileFromURL(forecast_items[forecast_item_counter].conditionIcon);
            mappedName = ImageProcessor.getMappedConditionsImageName(imageName, false);
            if (mappedName != null) {
                mConditionIconFileName = mappedName + ".png";
                conditionsID = getResources().getIdentifier(mappedName, "drawable", getPackageName());
            }
        }

        sdlManager.getScreenManager().setTextAlignment(TextAlignment.LEFT_ALIGNED);
        sdlManager.getScreenManager().setPrimaryGraphic(new SdlArtwork(mConditionIconFileName, FileType.GRAPHIC_PNG, conditionsID, true));

        if (mDisplayType != DisplayType.CID && mDisplayType != DisplayType.NGN) {
            sdlManager.getScreenManager().setSoftButtonObjects(softButtonObjects);
        }

        sdlManager.getScreenManager().commit(new CompletionListener() {
            @Override
            public void onComplete(boolean success) {
                Log.i(TAG, "ScreenManager update complete: " + success);
            }
        });

        if (includeSpeak) {
            Vector<TTSChunk> chunks = new Vector<TTSChunk>();
            TTSChunk chunk = new TTSChunk();
            chunk.setText(forecast_items[forecast_item_counter].speakString);
            chunk.setType(SpeechCapabilities.TEXT);
            chunks.add(chunk);

            Speak speakRequest = new Speak();
            speakRequest.setTtsChunks(chunks);
            speakRequest.setCorrelationID(autoIncCorrId++);
            sdlManager.sendRPC(speakRequest);
        }
        return;
    }

    private void showForecast(boolean includeSpeak, int numberOfForecasts) {
        mTimedShowHandler.removeCallbacks(mTimedShowRunnable);
        Forecast[] forecast;

        if (mActiveInfoType == InfoType.HOURLY_FORECAST) {
            forecast = mHourlyForecast;

        } else {
            forecast = mForecast;
        }

        if (mDataManager.isInErrorState()) {
            showWeatherError();
        } else if (forecast != null) {
            // We have a forecast we are not in an error state so reset error flags
            resetFirstErrorFlags();

            String speakStrings = "";
            String showStrings = "";
            SimpleDateFormat dateFormat = new SimpleDateFormat(getResources().getString(R.string.weather_forecast_simpleDateFormat), Locale.getDefault());
            SimpleDateFormat fullDayOfWeekFormat = new SimpleDateFormat(getResources().getString(R.string.weather_forecast_fullDayOfWeekFormat), Locale.getDefault());
            SimpleDateFormat shortDayOfWeekFormat = new SimpleDateFormat(getResources().getString(R.string.weather_forecast_shortDayOfWeekFormat), Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat(getResources().getString(R.string.weather_forecast_simpleTimeFormat), Locale.getDefault());

            int actualForecasts = forecast.length;

            if (numberOfForecasts > actualForecasts) {
                numberOfForecasts = actualForecasts;
            }
            forecast_items = new ForecastItem[numberOfForecasts];

            for (int forecastCounter = 0; forecastCounter < numberOfForecasts; forecastCounter++) {
                Forecast currentForecast = forecast[forecastCounter];
                forecast_items[forecastCounter] = new ForecastItem();

                if (currentForecast != null) {
                    String fullDateString = fullDayOfWeekFormat.format(currentForecast.date.getTime().getTime() * 1000);
                    String shortDateString = shortDayOfWeekFormat.format(currentForecast.date.getTime().getTime() * 1000);
                    String dateString = dateFormat.format(currentForecast.date.getTime().getTime() * 1000);
                    String timeString = timeFormat.format(currentForecast.date.getTime().getTime() * 1000);

                    forecast_items[forecastCounter].fullDateString = fullDateString;
                    forecast_items[forecastCounter].shortDateString = shortDateString;
                    forecast_items[forecastCounter].dateString = dateString;
                    forecast_items[forecastCounter].timeString = timeString;

                    float high = 0;
                    float low = 0;
                    float temp = 0;

                    String highString = "";
                    String tempString = "";

                    if (mActiveInfoType == InfoType.HOURLY_FORECAST) {
                        temp = currentForecast.temperature;
                        if (!unitsInMetric) {
                            temp = UnitConverter.convertTemperatureToImperial(temp);
                        }

                        if (temp <= -1) {
                            tempString = String.format(Locale.getDefault(),
                                    getResources().getString(R.string.weather_forecast_neg_temp_speak), temp * -1);
                        } else {
                            tempString = String.format(Locale.getDefault(),
                                    getResources().getString(R.string.weather_forecast_pos_temp_speak), temp);
                        }
                    } else {
                        high = currentForecast.highTemperature;
                        low = currentForecast.lowTemperature;
                        if (!unitsInMetric) {
                            high = UnitConverter.convertTemperatureToImperial(high);
                            low = UnitConverter.convertTemperatureToImperial(low);
                        }

                        if (high <= -1) {
                            highString = String.format(Locale.getDefault(),
                                    getResources().getString(R.string.weather_forecast_neg_temp_high_speak), high * -1);
                        } else {
                            highString = String.format(Locale.getDefault(),
                                    getResources().getString(R.string.weather_forecast_pos_temp_high_speak), high);
                        }
                    }

                    forecast_items[forecastCounter].numberOfForecasts = numberOfForecasts;
                    forecast_items[forecastCounter].highTemperature = high;
                    forecast_items[forecastCounter].lowTemperature = low;
                    forecast_items[forecastCounter].conditionIcon = currentForecast.conditionIcon;

                    String[] titleWords = currentForecast.conditionTitle.split("[\\s]");
                    String title;
                    if (titleWords.length > 0) {
                        if (currentForecast.conditionTitle.startsWith(getResources().getString(R.string.weather_forecast_title_partly)) ||
                                currentForecast.conditionTitle.startsWith(getResources().getString(R.string.weather_forecast_title_mostly)) ||
                                currentForecast.conditionTitle.startsWith(getResources().getString(R.string.weather_forecast_title_light)) ||
                                currentForecast.conditionTitle.startsWith(getResources().getString(R.string.weather_forecast_title_heavy))) {
                            title = titleWords[0] + " " + titleWords[1];
                        } else {
                            title = titleWords[0];
                        }
                    } else {
                        title = "";
                    }

                    if (mLengthOfTextFields < 40) {
                        if (AbbreviationDictionary.isPrepared())
                            title = abbreviate(title);
                    }

                    forecast_items[forecastCounter].title = title;
                    int precipChance = currentForecast.precipitationChance;

                    String precipChanceStringFull;
                    String precipChanceStringShort;
                    if (precipChance > 0) {
                        precipChanceStringFull = String.format(Locale.getDefault(),
                                getResources().getString(R.string.weather_forecast_percip_speak), precipChance);
                        precipChanceStringShort = String.format(Locale.getDefault(), "%d%% ", precipChance);
                    } else {
                        precipChanceStringFull = "";
                        precipChanceStringShort = "";
                    }

                    forecast_items[forecastCounter].precipitationChance = precipChance;
                    forecast_items[forecastCounter].precipChanceStringFull = precipChanceStringFull;
                    forecast_items[forecastCounter].precipChanceStringShort = precipChanceStringShort;

                    if (mActiveInfoType == InfoType.HOURLY_FORECAST) {
                        speakStrings = (String.format(Locale.getDefault(),
                                getResources().getString(R.string.weather_forecast_hourly_speak),
                                timeString, currentForecast.conditionTitle, precipChanceStringFull, tempString));

                    } else {
                        speakStrings = (String.format(Locale.getDefault(),
                                getResources().getString(R.string.weather_forecast_more3_speak),
                                fullDateString, currentForecast.conditionTitle, precipChanceStringFull, highString));
                    }
                    forecast_items[forecastCounter].speakString = speakStrings;

                    if (mLengthOfTextFields < 40) {
                        if (mActiveInfoType == InfoType.HOURLY_FORECAST) {
                            showStrings = (String.format(Locale.getDefault(),
                                    "%s %s: %s %s %.0f",
                                    shortDateString, timeString, title, precipChanceStringShort, temp));
                            forecast_items[forecastCounter].showString_field1 = (String.format(Locale.getDefault(),
                                    "%s %s: %s",
                                    shortDateString, timeString, title));
                            forecast_items[forecastCounter].showString_field2 = (String.format(Locale.getDefault(),
                                    "%s %.0f",
                                    precipChanceStringShort, temp));

                        } else {
                            showStrings = (String.format(Locale.getDefault(), "%s %s: %s %s(" +
                                            getResources().getString(R.string.weather_forecast_high_temp) + "%.0f / " +
                                            getResources().getString(R.string.weather_forecast_low_temp) + "%.0f)",
                                    shortDateString, dateString, title, precipChanceStringShort, high, low));
                            forecast_items[forecastCounter].showString_field1 = (String.format(Locale.getDefault(), "%s %s: %s",
                                    shortDateString, dateString, title));
                            forecast_items[forecastCounter].showString_field2 = (String.format(Locale.getDefault(), "%s(" +
                                            getResources().getString(R.string.weather_forecast_high_temp) + "%.0f / " +
                                            getResources().getString(R.string.weather_forecast_low_temp) + "%.0f)",
                                    precipChanceStringShort, high, low));
                        }
                    } else {
                        if (mActiveInfoType == InfoType.HOURLY_FORECAST) {
                            showStrings = (String.format(Locale.getDefault(),
                                    "%s: %s %.0f %s",
                                    timeString, title, temp, tempUnitsShort));
                            forecast_items[forecastCounter].showString_field1 = (String.format(Locale.getDefault(),
                                    "%s: %s",
                                    timeString, title));
                            forecast_items[forecastCounter].showString_field2 = (String.format(Locale.getDefault(),
                                    "%.0f %s",
                                    temp, tempUnitsShort));
                        } else {
                            showStrings = (String.format(Locale.getDefault(),
                                    "%s: %s %.0f %s",
                                    shortDateString, title, high, tempUnitsShort));
                            forecast_items[forecastCounter].showString_field1 = (String.format(Locale.getDefault(),
                                    "%s: %s",
                                    shortDateString, title));
                            forecast_items[forecastCounter].showString_field2 = (String.format(Locale.getDefault(),
                                    "%.0f %s",
                                    high, tempUnitsShort));
                        }
                    }
                    forecast_items[forecastCounter].showString = showStrings;
                }
            }
            if ((mActiveInfoType == InfoType.HOURLY_FORECAST) || (mActiveInfoType == InfoType.DAILY_FORECAST)) {
                createForecastChoiceSet();
            }
            writeDisplay(includeSpeak);
        } else {
            showNoConditionsAvail();
        }
    }

    /**
     * @param icon_url
     * @return sdlArtwork for Forecast choiceset
     */
    private SdlArtwork getArtWork(URL icon_url){
        String mappedName = null;
        int conditionID = 0;
        if (mGraphicsSupported && mWeatherConditions.conditionIcon != null) {
            String imageName = ImageProcessor.getFileFromURL(icon_url);
            mappedName = ImageProcessor.getMappedConditionsImageName(imageName, false);

            if (mappedName != null) {
                mConditionIconFileName = mappedName + ".png";
                conditionID = getResources().getIdentifier(mappedName, "drawable", getPackageName());
            }
        }
        SdlArtwork tempArtworkName = new SdlArtwork(mConditionIconFileName,FileType.GRAPHIC_PNG,conditionID,true);
        return tempArtworkName;
    }

    /**
     * pre loads choiceset for Hourly and Daily forecast
     */
    private void createForecastChoiceSet() {
        /* Choices for Hourly Forecast to be created */
        if (choiceCellList != null) {
            sdlManager.getScreenManager().deleteChoices(choiceCellList);
        }
        choiceCellList = null;

        if (mActiveInfoType == InfoType.HOURLY_FORECAST) {
            ChoiceCell cell1 = new ChoiceCell(forecast_items[0].timeString, new Vector<>(Arrays.asList(new String[]{forecast_items[0].timeString})), getArtWork(forecast_items[0].conditionIcon));
            ChoiceCell cell2 = new ChoiceCell(forecast_items[1].timeString, new Vector<>(Arrays.asList(new String[]{forecast_items[1].timeString})), getArtWork(forecast_items[1].conditionIcon));
            ChoiceCell cell3 = new ChoiceCell(forecast_items[2].timeString, new Vector<>(Arrays.asList(new String[]{forecast_items[2].timeString})), getArtWork(forecast_items[2].conditionIcon));
            ChoiceCell cell4 = new ChoiceCell(forecast_items[3].timeString, new Vector<>(Arrays.asList(new String[]{forecast_items[3].timeString})), getArtWork(forecast_items[3].conditionIcon));
            ChoiceCell cell5 = new ChoiceCell(forecast_items[4].timeString, new Vector<>(Arrays.asList(new String[]{forecast_items[4].timeString})), getArtWork(forecast_items[4].conditionIcon));
            ChoiceCell cell6 = new ChoiceCell(forecast_items[5].timeString, new Vector<>(Arrays.asList(new String[]{forecast_items[5].timeString})), getArtWork(forecast_items[5].conditionIcon));
            ChoiceCell cell7 = new ChoiceCell(forecast_items[6].timeString, new Vector<>(Arrays.asList(new String[]{forecast_items[6].timeString})), getArtWork(forecast_items[6].conditionIcon));
            ChoiceCell cell8 = new ChoiceCell(forecast_items[7].timeString, new Vector<>(Arrays.asList(new String[]{forecast_items[7].timeString})), getArtWork(forecast_items[7].conditionIcon));
            ChoiceCell cell9 = new ChoiceCell(forecast_items[8].timeString, new Vector<>(Arrays.asList(new String[]{forecast_items[8].timeString})), getArtWork(forecast_items[8].conditionIcon));
            ChoiceCell cell10 = new ChoiceCell(forecast_items[9].timeString, new Vector<>(Arrays.asList(new String[]{forecast_items[9].timeString})), getArtWork(forecast_items[9].conditionIcon));
            ChoiceCell cell11 = new ChoiceCell(forecast_items[10].timeString, new Vector<>(Arrays.asList(new String[]{forecast_items[10].timeString})), getArtWork(forecast_items[10].conditionIcon));
            ChoiceCell cell12 = new ChoiceCell(forecast_items[11].timeString, new Vector<>(Arrays.asList(new String[]{forecast_items[11].timeString})), getArtWork(forecast_items[11].conditionIcon));
            forecast_item_counter = 0;
            choiceCellList = Arrays.asList(cell1, cell2, cell3, cell4, cell5, cell6, cell7, cell8, cell9, cell10, cell11, cell12);
            sdlManager.getScreenManager().preloadChoices(choiceCellList, null);
        }

        /* Choices for Daily Forecast to be created */
        else if (mActiveInfoType == InfoType.DAILY_FORECAST) {
            ChoiceCell cell1 = new ChoiceCell(getResources().getString(R.string.cmd_today), new Vector<>(Arrays.asList(new String[]{getResources().getString(R.string.cmd_today)})), getArtWork(forecast_items[0].conditionIcon));
            ChoiceCell cell2 = new ChoiceCell(getResources().getString(R.string.cmd_tomorrow), new Vector<>(Arrays.asList(new String[]{getResources().getString(R.string.cmd_tomorrow)})), getArtWork(forecast_items[1].conditionIcon));
            ChoiceCell cell3 = new ChoiceCell(forecast_items[2].fullDateString, new Vector<>(Arrays.asList(new String[]{forecast_items[2].fullDateString})), getArtWork(forecast_items[2].conditionIcon));
            ChoiceCell cell4 = new ChoiceCell(forecast_items[3].fullDateString, new Vector<>(Arrays.asList(new String[]{forecast_items[3].fullDateString})), getArtWork(forecast_items[3].conditionIcon));
            ChoiceCell cell5 = new ChoiceCell(forecast_items[4].fullDateString, new Vector<>(Arrays.asList(new String[]{forecast_items[4].fullDateString})), getArtWork(forecast_items[4].conditionIcon));
            ChoiceCell cell6 = new ChoiceCell(forecast_items[5].fullDateString, new Vector<>(Arrays.asList(new String[]{forecast_items[5].fullDateString})), getArtWork(forecast_items[5].conditionIcon));
            ChoiceCell cell7 = new ChoiceCell(forecast_items[6].fullDateString, new Vector<>(Arrays.asList(new String[]{forecast_items[6].fullDateString})), getArtWork(forecast_items[6].conditionIcon));
            ChoiceCell cell8 = new ChoiceCell(forecast_items[7].fullDateString, new Vector<>(Arrays.asList(new String[]{forecast_items[7].fullDateString})), getArtWork(forecast_items[7].conditionIcon));
            forecast_item_counter = 0;
            choiceCellList = Arrays.asList(cell1, cell2, cell3, cell4, cell5, cell6, cell7, cell8);
            sdlManager.getScreenManager().preloadChoices(choiceCellList, null);
        } else {
            Log.d(SdlApplication.TAG, "CreateInteractioinChoiceSet requested for something else than hourly or daily forecast");
        }
    }

    private void showStandardForecast(boolean includeSpeak) {
        showForecast(includeSpeak, STANDARD_FORECAST_DAYS);
    }

    private void showDailyForecast(boolean includeSpeak) {
        showForecast(includeSpeak, DAILY_FORECAST_DAYS);
    }

    private void showAlerts(boolean includeSpeak) {
        mTimedShowHandler.removeCallbacks(mTimedShowRunnable);

        if (mDataManager.isInErrorState()) {
            showWeatherError();
        } else if (mAlerts != null) {
            // We have alerts and we are not in error state reset error flags
            resetFirstErrorFlags();

            Vector<String> speakStrings = new Vector<String>();
            Vector<String> showStrings = new Vector<String>();
            String showStr = "";
            SimpleDateFormat timeFormat = new SimpleDateFormat(getResources().getString(R.string.weather_alerts_simpleDateFormat), Locale.getDefault());

            for (int alertCounter = 0; alertCounter < mAlerts.length; alertCounter++) {
                WeatherAlert currentAlert = mAlerts[alertCounter];
                if (currentAlert != null) {
                    String timeString = timeFormat.format(currentAlert.dateExpires.getTime().getTime() * 1000);

                    if (alertCounter < 3) {
                        speakStrings.add(String.format(Locale.getDefault(), getResources().getString(R.string.weather_alerts_expires_at),
                                currentAlert.message, timeString.replace(':', ' ').replace("00", "")));
                    }

                    showStr = String.format(Locale.getDefault(), "%s :" +
                                    getResources().getString(R.string.weather_alerts_expires),
                            currentAlert.message, timeString);

                    if (mLengthOfTextFields > showStr.length()) {
                        showStrings.add(showStr);
                    } else {
                        if (AbbreviationDictionary.isPrepared())
                            showStrings.add(abbreviate(showStr));
                    }
                }
            }

            List<SoftButtonObject> softButtonObjects = new ArrayList<>();
            softButtonObjects.add(mShowConditions);
            softButtonObjects.add(mShowDailyForecast);
            softButtonObjects.add(mShowHourlyForecast);

            mTimedShowRunnable = new TimedShowRunnable(showStrings, softButtonObjects, 0, TIMED_SHOW_DELAY);
            mTimedShowHandler.post(mTimedShowRunnable);

            if (includeSpeak) {
                Vector<TTSChunk> chunks = new Vector<TTSChunk>();
                for (String speakString : speakStrings) {
                    TTSChunk chunk = new TTSChunk();
                    chunk.setText(speakString);
                    chunk.setType(SpeechCapabilities.TEXT);
                    chunks.add(chunk);
                }
                Speak speakRequest = new Speak();
                speakRequest.setTtsChunks(chunks);
                speakRequest.setCorrelationID(autoIncCorrId++);
                sdlManager.sendRPC(speakRequest); //TODO here
            }
        } else {
            sdlManager.getScreenManager().beginTransaction();
            sdlManager.getScreenManager().setTextField1(getResources().getString(R.string.weather_alerts_txt_field1));
            sdlManager.getScreenManager().setTextField2(getResources().getString(R.string.weather_alerts_txt_field2));
            sdlManager.getScreenManager().setTextField3(getResources().getString(R.string.weather_alerts_txt_field3));
            sdlManager.getScreenManager().setTextField4(getResources().getString(R.string.weather_alerts_txt_field4));
            sdlManager.getScreenManager().setMediaTrackTextField(getResources().getString(R.string.weather_alerts_txt_mediatrack));
            sdlManager.getScreenManager().setTextAlignment(TextAlignment.CENTERED);
            sdlManager.getScreenManager().commit(new CompletionListener() {
                @Override
                public void onComplete(boolean success) {
                    Log.i(TAG, "ScreenManager update complete: " + success);
                }
            });

            if (includeSpeak) {
                speak(getResources().getString(R.string.weather_alerts_speak), autoIncCorrId++);
            }
        }
    }

    private void performWeatherAlert(WeatherAlert alert) {
        SimpleDateFormat timeFormat = new SimpleDateFormat(getResources().getString(R.string.weather_alerts_simpleDateFormat), Locale.getDefault());
        String timeString = timeFormat.format(alert.dateExpires.getTime().getTime() * 1000);

        String speakString = String.format(Locale.getDefault(), getResources().getString(R.string.weather_alerts_expires_at),
                alert.message, timeString.replace(':', ' ').replace("00", ""));
        Log.d(SdlApplication.TAG, "performWeatherAlert: speak string - " + speakString);

        Vector<TTSChunk> chunks = new Vector<TTSChunk>();
        TTSChunk chunk = new TTSChunk();
        chunk.setText(speakString);
        chunk.setType(SpeechCapabilities.TEXT);
        chunks.add(chunk);

        Alert alertRequest = new Alert();
        alertRequest.setTtsChunks(chunks);
        alertRequest.setAlertText1(alert.message);
        alertRequest.setDuration(7000);
        int coId = autoIncCorrId++;
        mLastAlertId = coId;
        alertRequest.setCorrelationID(coId);
        alertRequest.setOnRPCResponseListener(new OnRPCResponseListener() {
            @Override
            public void onResponse(int correlationId, RPCResponse response) {
                if (response.getCorrelationID() == mLastAlertId) {
                    if (mAlertQueue.size() > 0) {
                        performWeatherAlert(mAlertQueue.pop());
                    } else if (mActiveInfoType == InfoType.NONE && mConditionsRdy && mLocationRdy) {
                        mWelcomeComplete = true;
                        mActiveInfoType = InfoType.WEATHER_CONDITIONS;
                        updateHmi(true);
                    }
                }
            }
        });
        sdlManager.sendRPC(alertRequest);
    }

    private void showHourlyForecast(boolean includeSpeak) {
        showForecast(includeSpeak, HOURLY_FORECAST_HOURS);
    }

    private void updateHmi(boolean includeSpeaks) {
        switch (mActiveInfoType) {
            case WEATHER_CONDITIONS:
                showWeatherConditions(includeSpeaks);
                break;
            case DAILY_FORECAST:
                showDailyForecast(includeSpeaks);
                break;
            case STANDARD_FORECAST:
                showStandardForecast(includeSpeaks);
                break;
            case HOURLY_FORECAST:
                showHourlyForecast(includeSpeaks);
                break;
            case ALERTS:
                showAlerts(includeSpeaks);
                break;
            default:
                break;
        }
    }

    private String abbreviate(String iAbrv) {
        String[] tokens = iAbrv.split(" ");
        String replacement = null;
        String oAbrv = "";
        for (int i = 0; i < tokens.length; i++) {
            replacement = AbbreviationDictionary.lookUp(tokens[i].toLowerCase(Locale.ENGLISH));
            if (replacement != null) {
                oAbrv += replacement + " ";
            } else {
                oAbrv += tokens[i] + " ";
            }
        }
        return oAbrv;
    }

    private boolean checkNewAlerts() {
        // Iterate through the alerts to find if any of them are new
        boolean haveNewAlerts = false;
        if (mAlerts != null) {
            boolean isNew;
            for (WeatherAlert alert : mAlerts) {
                isNew = true;
                if (mPreviousAlerts != null) {
                    for (WeatherAlert previousAlert : mPreviousAlerts) {
                        if (previousAlert.message.equals(alert.message) &&
                                !(mActiveInfoType == InfoType.NONE)) {
                            isNew = false;
                        }
                    }
                }
                if (isNew) {
                    haveNewAlerts = true;
                    mAlertQueue.add(alert);
                } else {
                    Log.v(SdlApplication.TAG, "Ignored alert as old: " + alert.message);
                }
            }
        }
        return haveNewAlerts;
    }

    public void speak(@NonNull String ttsText, Integer correlationID) {
        Speak msg = new Speak(TTSChunkFactory.createSimpleTTSChunks(ttsText));
        msg.setCorrelationID(correlationID);

        sdlManager.sendRPC(msg); //TODO here
    }

    public void setGlobalProperties(String helpPrompt, String timeoutPrompt, Integer correlationID) {
        SetGlobalProperties req = new SetGlobalProperties();
        req.setCorrelationID(correlationID);
        req.setHelpPrompt(TTSChunkFactory.createSimpleTTSChunks(helpPrompt));
        req.setTimeoutPrompt(TTSChunkFactory.createSimpleTTSChunks(timeoutPrompt));
        sdlManager.sendRPC(req);
    }
}
