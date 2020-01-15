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
import com.smartdevicelink.exception.SdlException;
import com.smartdevicelink.managers.BaseSubManager;
import com.smartdevicelink.managers.CompletionListener;
import com.smartdevicelink.managers.SdlManager;
import com.smartdevicelink.managers.SdlManagerListener;
import com.smartdevicelink.managers.file.filetypes.SdlArtwork;
import com.smartdevicelink.managers.lifecycle.LifecycleConfigurationUpdate;
import com.smartdevicelink.managers.screen.SoftButtonObject;
import com.smartdevicelink.managers.screen.SoftButtonState;
import com.smartdevicelink.protocol.enums.FunctionID;
import com.smartdevicelink.proxy.RPCNotification;
import com.smartdevicelink.proxy.RPCResponse;
import com.smartdevicelink.proxy.TTSChunkFactory;
import com.smartdevicelink.proxy.rpc.AddCommand;
import com.smartdevicelink.proxy.rpc.Alert;
import com.smartdevicelink.proxy.rpc.ChangeRegistration;
import com.smartdevicelink.proxy.rpc.Choice;
import com.smartdevicelink.proxy.rpc.CreateInteractionChoiceSet;
import com.smartdevicelink.proxy.rpc.DeleteCommand;
import com.smartdevicelink.proxy.rpc.DeleteInteractionChoiceSet;
import com.smartdevicelink.proxy.rpc.DeviceStatus;
import com.smartdevicelink.proxy.rpc.DisplayCapabilities;
import com.smartdevicelink.proxy.rpc.Image;
import com.smartdevicelink.proxy.rpc.ListFiles;
import com.smartdevicelink.proxy.rpc.ListFilesResponse;
import com.smartdevicelink.proxy.rpc.MenuParams;
import com.smartdevicelink.proxy.rpc.OnButtonEvent;
import com.smartdevicelink.proxy.rpc.OnButtonPress;
import com.smartdevicelink.proxy.rpc.OnCommand;
import com.smartdevicelink.proxy.rpc.OnHMIStatus;
import com.smartdevicelink.proxy.rpc.OnLanguageChange;
import com.smartdevicelink.proxy.rpc.OnVehicleData;
import com.smartdevicelink.proxy.rpc.PerformInteraction;
import com.smartdevicelink.proxy.rpc.PerformInteractionResponse;
import com.smartdevicelink.proxy.rpc.PutFile;
import com.smartdevicelink.proxy.rpc.SetDisplayLayout;
import com.smartdevicelink.proxy.rpc.SetGlobalProperties;
import com.smartdevicelink.proxy.rpc.Show;
import com.smartdevicelink.proxy.rpc.SoftButton;
import com.smartdevicelink.proxy.rpc.Speak;
import com.smartdevicelink.proxy.rpc.SubscribeButton;
import com.smartdevicelink.proxy.rpc.TTSChunk;
import com.smartdevicelink.proxy.rpc.TextField;
import com.smartdevicelink.proxy.rpc.enums.ButtonName;
import com.smartdevicelink.proxy.rpc.enums.DisplayType;
import com.smartdevicelink.proxy.rpc.enums.DriverDistractionState;
import com.smartdevicelink.proxy.rpc.enums.FileType;
import com.smartdevicelink.proxy.rpc.enums.HMILevel;
import com.smartdevicelink.proxy.rpc.enums.ImageType;
import com.smartdevicelink.proxy.rpc.enums.InteractionMode;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.smartdevicelink.proxy.rpc.enums.Result;
import com.smartdevicelink.proxy.rpc.enums.SoftButtonType;
import com.smartdevicelink.proxy.rpc.enums.SpeechCapabilities;
import com.smartdevicelink.proxy.rpc.enums.SystemAction;
import com.smartdevicelink.proxy.rpc.enums.SystemCapabilityType;
import com.smartdevicelink.proxy.rpc.enums.TextAlignment;
import com.smartdevicelink.proxy.rpc.enums.TextFieldName;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener;
import com.smartdevicelink.proxy.rpc.listeners.OnRPCResponseListener;
import com.smartdevicelink.transport.BaseTransportConfig;
import com.smartdevicelink.transport.MultiplexTransportConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    private static final int VIEW_CURRENT_CONDITIONS = 1;
    private static final int VIEW_STANDARD_FORECAST = 2;
    private static final int VIEW_DAILY_FORECAST = 3;
    private static final int CHANGE_UNITS = 4;
    private static final int CHANGE_UNITS_CHOICESET = 1;
    private static final int METRIC_CHOICE = 1;
    private static final int IMPERIAL_CHOICE = 2;
    private static final int VIEW_HOURLY_FORECAST = 5;
    private static final int VIEW_ALERTS = 6;
    private static final int PREVIOUS = 7;
    private static final int NEXT = 8;
    private static final int LIST = 9;
    private static final int BACK = 10;
    private static final int TODAY = 11;
    private static final int TOMORROW = 12;
    private static final int NOW = 13;
    private static Boolean DailyForecast_ChoiceSet_created = false;
    private static Boolean HourlyForecast_ChoiceSet_created = false;
    private static int mDailyForecast_ChoiceSetID = 400;
    private static int mHourlyForecast_ChoiceSetID = 600;
    private int delete_DailyForecast_ChoiceSet_corrId = 0;
    private int delete_HourlyForecast_ChoiceSet_corrId = 0;
    private int create_DailyForecast_ChoiceSet_corrId = 0;
    private int create_HourlyForecast_ChoiceSet_corrId = 0;
    private static final int CHOICE_ITEM1_ID = 200;
    private static final int CHOICE_ITEM2_ID = 201;
    private static final int CHOICE_ITEM3_ID = 202;
    private static final int CHOICE_ITEM4_ID = 203;
    private static final int CHOICE_ITEM5_ID = 204;
    private static final int CHOICE_ITEM6_ID = 205;
    private static final int CHOICE_ITEM7_ID = 206;
    private static final int CHOICE_ITEM8_ID = 207;
    private static final int CHOICE_ITEM9_ID = 208;
    private static final int CHOICE_ITEM10_ID = 209;
    private static final int CHOICE_ITEM11_ID = 210;
    private static final int CHOICE_ITEM12_ID = 211;
    private static final int CHOICE_ITEM13_ID = 212;
    private static final int CHOICE_ITEM14_ID = 213;
    private static final int CHOICE_ITEM15_ID = 214;
    private static final int CHOICE_ITEM16_ID = 215;
    private static final int CHOICE_ITEM17_ID = 216;
    private static final int CHOICE_ITEM18_ID = 217;
    private static final int CHOICE_ITEM19_ID = 218;
    private static final int CHOICE_ITEM20_ID = 219;
    private static final int CHOICE_ITEM21_ID = 220;
    private static final int CHOICE_ITEM22_ID = 221;
    private static final int CHOICE_ITEM23_ID = 222;
    private static final int CHOICE_ITEM24_ID = 223;
    private static final int TIMED_SHOW_DELAY = 8000;
    private static final String APP_ICON_NAME = "icon";
    private static final String APP_ICON = APP_ICON_NAME + ".png";
    private static final String CLEAR_ICON = "";
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
    private Boolean next_cmd_added = false;
    private Boolean previous_cmd_added = false;
    private Boolean now_cmd_added = false;
    private Boolean today_cmd_added = false;
    private Boolean tomorrow_cmd_added = false;
    private Boolean list_cmd_added = false;
    private Boolean daily_forecast_cmd_added = false;
    private Boolean hourly_forecast_cmd_added = false;
    private int daily_forecast_cmd_added_corrId = 0;
    private int daily_forecast_cmd_deleted_corrId = 0;
    private int hourly_forecast_cmd_added_corrId = 0;
    private int hourly_forecast_cmd_deleted_corrId = 0;
    private int next_cmd_added_corrId = 0;
    private int next_cmd_deleted_corrId = 0;
    private int previous_cmd_added_corrId = 0;
    private int previous_cmd_deleted_corrId = 0;
    private int now_cmd_added_corrId = 0;
    private int now_cmd_deleted_corrId = 0;
    private int today_cmd_added_corrId = 0;
    private int today_cmd_deleted_corrId = 0;
    private int tomorrow_cmd_added_corrId = 0;
    private int tomorrow_cmd_deleted_corrId = 0;
    private int list_cmd_added_corrId = 0;
    private int list_cmd_deleted_corrId = 0;
    private Handler mTimedShowHandler = null;
    private ArrayList<String> mUploadedFiles = null;
    private SparseArray<String> mPutFileMap = null;
    private Show mShowPendingPutFile = null;
    private String mConditionIconFileName = null;
    private WeatherDataManager mDataManager = null;
    private Boolean unitsInMetric = true;
    private String tempUnitsShort = "C";
    private String speedUnitsShort = "KPH";
    private String speedUnitsFull = "kilometers per hour";
    private String lengthUnitsFull = "millimeters";
    private Handler mHandler = null;
    private HMILevel currentHMILevel = HMILevel.HMI_NONE;
    private DriverDistractionState currentDDState = DriverDistractionState.DD_OFF;
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

    private OnRPCResponseListener onPerformInteractionResponseListener = new OnRPCResponseListener() {
        @Override
        public void onResponse(int correlationId, RPCResponse response) {
            PerformInteractionResponse performInteractionResponse = (PerformInteractionResponse) response;
            if (response.getSuccess()) {
                Integer choiceID = performInteractionResponse.getChoiceID();
                switch (choiceID) {
                    case METRIC_CHOICE:
                        setUnitsMetric();
                        break;
                    case IMPERIAL_CHOICE:
                        setUnitsImp();
                        break;
                    case CHOICE_ITEM1_ID:
                        forecast_item_counter = 0;
                        break;
                    case CHOICE_ITEM2_ID:
                        forecast_item_counter = 1;
                        break;
                    case CHOICE_ITEM3_ID:
                        forecast_item_counter = 2;
                        break;
                    case CHOICE_ITEM4_ID:
                        forecast_item_counter = 3;
                        break;
                    case CHOICE_ITEM5_ID:
                        forecast_item_counter = 4;
                        break;
                    case CHOICE_ITEM6_ID:
                        forecast_item_counter = 5;
                        break;
                    case CHOICE_ITEM7_ID:
                        forecast_item_counter = 6;
                        break;
                    case CHOICE_ITEM8_ID:
                        forecast_item_counter = 7;
                        break;
                    case CHOICE_ITEM9_ID:
                        forecast_item_counter = 8;
                        break;
                    case CHOICE_ITEM10_ID:
                        forecast_item_counter = 9;
                        break;
                    case CHOICE_ITEM11_ID:
                        forecast_item_counter = 10;
                        break;
                    case CHOICE_ITEM12_ID:
                        forecast_item_counter = 11;
                        break;
                    case CHOICE_ITEM13_ID:
                        forecast_item_counter = 12;
                        break;
                    case CHOICE_ITEM14_ID:
                        forecast_item_counter = 13;
                        break;
                    case CHOICE_ITEM15_ID:
                        forecast_item_counter = 14;
                        break;
                    case CHOICE_ITEM16_ID:
                        forecast_item_counter = 15;
                        break;
                    case CHOICE_ITEM17_ID:
                        forecast_item_counter = 16;
                        break;
                    case CHOICE_ITEM18_ID:
                        forecast_item_counter = 17;
                        break;
                    case CHOICE_ITEM19_ID:
                        forecast_item_counter = 18;
                        break;
                    case CHOICE_ITEM20_ID:
                        forecast_item_counter = 19;
                        break;
                    case CHOICE_ITEM21_ID:
                        forecast_item_counter = 20;
                        break;
                    case CHOICE_ITEM22_ID:
                        forecast_item_counter = 21;
                        break;
                    case CHOICE_ITEM23_ID:
                        forecast_item_counter = 22;
                        break;
                    case CHOICE_ITEM24_ID:
                        forecast_item_counter = 23;
                        break;
                }
                writeDisplay(true);
            }
        }
    };

    private OnRPCResponseListener addCommandResponseListener = new OnRPCResponseListener() {
        @Override
        public void onResponse(int correlationId, RPCResponse response) {
            if (response.getSuccess()) {
                String help_prompt = (getResources().getString(R.string.gp_help_prompt_start));
                if (response.getCorrelationID() == next_cmd_added_corrId) {
                    next_cmd_added = true;
                    help_prompt += (getResources().getString(R.string.vr_next));
                }
                if (response.getCorrelationID() == previous_cmd_added_corrId) {
                    previous_cmd_added = true;
                    help_prompt += (getResources().getString(R.string.vr_prev));
                }
                if (response.getCorrelationID() == now_cmd_added_corrId) {
                    now_cmd_added = true;
                    help_prompt += (getResources().getString(R.string.vr_now));
                }
                if (response.getCorrelationID() == today_cmd_added_corrId) {
                    today_cmd_added = true;
                    help_prompt += (getResources().getString(R.string.vr_today));
                }
                if (response.getCorrelationID() == tomorrow_cmd_added_corrId) {
                    tomorrow_cmd_added = true;
                    help_prompt += (getResources().getString(R.string.vr_tomorrow));
                }
                if (response.getCorrelationID() == list_cmd_added_corrId) {
                    list_cmd_added = true;
                    help_prompt += (getResources().getString(R.string.vr_list));
                }
                if (response.getCorrelationID() == daily_forecast_cmd_added_corrId) {
                    daily_forecast_cmd_added = true;
                    help_prompt += (getResources().getString(R.string.vr_daily_forecast));
                }
                if (response.getCorrelationID() == hourly_forecast_cmd_added_corrId) {
                    hourly_forecast_cmd_added = true;
                    help_prompt += (getResources().getString(R.string.vr_hourly_forecast));
                }

                help_prompt += (getResources().getString(R.string.gp_help_prompt_end));

                setGlobalProperties(help_prompt, (getResources().getString(R.string.gp_timeout_prompt)), autoIncCorrId++);
            }
        }
    };

    private OnRPCResponseListener createInteractionChoiceSetListener = new OnRPCResponseListener() {
        @Override
        public void onResponse(int correlationId, RPCResponse response) {
            if (response.getCorrelationID() == create_DailyForecast_ChoiceSet_corrId) {
                if (response.getSuccess()) {
                    DailyForecast_ChoiceSet_created = true;
                    HourlyForecast_ChoiceSet_created = false;
                }
            }
            if (response.getCorrelationID() == create_HourlyForecast_ChoiceSet_corrId) {
                if (response.getSuccess()) {
                    HourlyForecast_ChoiceSet_created = true;
                    DailyForecast_ChoiceSet_created = false;
                }
            }
        }
    };

    public SdlManager getSdlManager() {
        return sdlManager;
    }

    private class ForecastItem extends Forecast {
        private Integer numberOfForecasts;
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
                Image appImage = null;
                if (mGraphicsSupported) {
                    appImage = new Image();
                    appImage.setImageType(ImageType.DYNAMIC);
                    appImage.setValue(CLEAR_ICON);
                }

                sdlManager.getScreenManager().setSoftButtonObjects(mSoftButtonObjects);


                Show showRequest = new Show();
                showRequest.setMainField1(field1);
                showRequest.setMainField2(field2);
                showRequest.setMainField3(field3);
                showRequest.setMainField4(field4);
                showRequest.setAlignment(TextAlignment.LEFT_ALIGNED);
                showRequest.setGraphic(appImage);
                showRequest.setCorrelationID(autoIncCorrId++);
                sdlManager.sendRPC(showRequest);
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
                /* add cmds relevant for Current Weather Conditions, remove cmds not needed */
                prepareCurrentCondCmds();
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
                /* add cmds relevant for Daily Forecast, remove cmds not needed */
                prepareDailyForecastCmds();
                mtemp_counter = 0;
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
                /* add cmds relevant for Hourly Forecast, remove cmds not needed */
                prepareHourlyForecastCmds();
                mtemp_counter = 0;
                forecast_item_counter = mtemp_counter;
                updateHmi(true);            }

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
                updateHmi(true);            }

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
                    if (mtemp_counter == 0) {
                        previous_cmd_deleted_corrId = autoIncCorrId;
                        deleteCommand(PREVIOUS, autoIncCorrId++);
                    }
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
                    next_cmd_deleted_corrId = autoIncCorrId;
                    deleteCommand(NEXT, autoIncCorrId++);

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
                mTimedShowHandler.removeCallbacks(mTimedShowRunnable);
                prepareListItemsCmds();
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
                prepareBackCmds();
                forecast_item_counter = 0;
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
        // mRoadConditions = mDataManager.getRoadConditions();
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


                    sdlManager.addOnRPCNotificationListener(FunctionID.ON_COMMAND, new OnRPCNotificationListener() {
                        @Override
                        public void onNotified(RPCNotification notification) {
                            OnCommand onCommand = (OnCommand) notification;
                            int mtemp_counter = forecast_item_counter;

                            if (notification != null) {
                                int command = onCommand.getCmdID();
                                switch (command) {
                                    case VIEW_CURRENT_CONDITIONS:
                                        mActiveInfoType = InfoType.WEATHER_CONDITIONS;
                                        /* add cmds relevant for Current Weather Conditions, remove cmds not needed */
                                        prepareCurrentCondCmds();
                                        break;
                                    case VIEW_STANDARD_FORECAST:
                                        mActiveInfoType = InfoType.STANDARD_FORECAST;
                                        break;
                                    case VIEW_DAILY_FORECAST:
                                        mActiveInfoType = InfoType.DAILY_FORECAST;
                                        /* add cmds relevant for Daily Forecast, remove cmds not needed */
                                        prepareDailyForecastCmds();
                                        mtemp_counter = 0;
                                        break;
                                    case VIEW_HOURLY_FORECAST:
                                        mActiveInfoType = InfoType.HOURLY_FORECAST;
                                        /* add cmds relevant for Hourly Forecast, remove cmds not needed */
                                        prepareHourlyForecastCmds();
                                        mtemp_counter = 0;
                                        break;
                                    case VIEW_ALERTS:
                                        mActiveInfoType = InfoType.ALERTS;
                                        break;
                                    case NEXT:
                                        mtemp_counter++;
                                        if (mtemp_counter < forecast_items.length) {
                                            forecast_item_counter = mtemp_counter;
                                            writeDisplay(true);
                                        }
                                        if (mtemp_counter >= forecast_items.length) {
                                            next_cmd_deleted_corrId = autoIncCorrId;
                                            deleteCommand(NEXT, autoIncCorrId++);

                                            speak("You have reached the end of the forecast list", autoIncCorrId++);
                                        }
                                        return;
                                    case PREVIOUS:
                                        mtemp_counter--;
                                        if (mtemp_counter >= 0) {
                                            if (mtemp_counter == 0) {
                                                previous_cmd_deleted_corrId = autoIncCorrId;
                                                deleteCommand(PREVIOUS, autoIncCorrId++);
                                            }
                                            forecast_item_counter = mtemp_counter;
                                            writeDisplay(true);
                                        } else {
                                            speak("You have reached the beginning of the forecast list", autoIncCorrId++);
                                        }
                                        return;

                                    case LIST:
                                        mTimedShowHandler.removeCallbacks(mTimedShowRunnable);
                                        forecast_item_counter = 0;
                                        prepareListItemsCmds();
                                        return;

                                    case BACK:
                                        mActiveInfoType = InfoType.WEATHER_CONDITIONS;
                                        prepareBackCmds();
                                        forecast_item_counter = 0;
                                        updateHmi(false);
                                        return;

                                    case TODAY:
                                        mtemp_counter = 0;
                                        break;
                                    case TOMORROW:
                                        if (mtemp_counter < forecast_items.length) {
                                            mActiveInfoType = InfoType.DAILY_FORECAST;
                                            mtemp_counter = 1;
                                        } else {
                                            speak("You have reached the end of the forecast list", autoIncCorrId++);
                                            return;
                                        }
                                        break;

                                    case NOW:
                                        mtemp_counter = 0;
                                        break;

                                    case CHANGE_UNITS:
                                        mTimedShowHandler.removeCallbacks(mTimedShowRunnable);

                                        Vector<Integer> interactionChoiceSetIDs = new Vector<Integer>();
                                        interactionChoiceSetIDs.add(CHANGE_UNITS_CHOICESET);
                                        Vector<TTSChunk> initChunks = TTSChunkFactory.createSimpleTTSChunks(getResources().getString(R.string.interaction_units_prompt));
                                        Vector<TTSChunk> helpChunks = TTSChunkFactory.createSimpleTTSChunks(getResources().getString(R.string.interaction_units_help_prompt));
                                        Vector<TTSChunk> timeoutChunks = TTSChunkFactory.createSimpleTTSChunks(getResources().getString(R.string.interaction_units_timeout_prompt));
                                        PerformInteraction msg = new PerformInteraction(getResources().getString(R.string.interaction_units_displaytext), InteractionMode.BOTH, interactionChoiceSetIDs);
                                        msg.setInitialPrompt(initChunks);
                                        msg.setTimeout(100000);
                                        msg.setHelpPrompt(helpChunks);
                                        msg.setTimeoutPrompt(timeoutChunks);
                                        msg.setCorrelationID(autoIncCorrId++);
                                        msg.setOnRPCResponseListener(onPerformInteractionResponseListener);
                                        sdlManager.sendRPC(msg);

                                        // Fall through to default to avoid showing prematurely
                                        // onPerformInteractionResponse() will perform the show once the user selects units
                                    default:
                                        // Return to avoid showing early for CHANGE_UNITS or unknown commands.
                                        return;
                                }
                                forecast_item_counter = mtemp_counter;
                                updateHmi(true);

                            }
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
            SdlArtwork appIcon = new SdlArtwork(APP_ICON, FileType.GRAPHIC_PNG, R.mipmap.ic_launcher, true);

            // The manager builder sets options for your session
            SdlManager.Builder builder = new SdlManager.Builder(this, APP_ID, "MobileWeather", listener);
            builder.setTransportType(transport);
            builder.setLanguage(mDesiredAppSdlLanguage);
            builder.setAppIcon(appIcon);


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

                                // Add commands
                                addCommands();

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
        Show showRequest = new Show();
        showRequest.setMainField1(getResources().getString(R.string.welcome_textfield1));
        showRequest.setMainField2(getResources().getString(R.string.welcome_textfield2));
        showRequest.setMainField3(getResources().getString(R.string.welcome_textfield3));
        showRequest.setMainField4(getResources().getString(R.string.welcome_textfield4));
        showRequest.setAlignment(TextAlignment.CENTERED);
        showRequest.setCorrelationID(autoIncCorrId++);
        sdlManager.sendRPC(showRequest);
        mWelcomeCorrId = autoIncCorrId++;
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
                            /*store the registered language if ChangeRegistration has been successful */
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

        // Upload files if graphics supported
        if (mGraphicsSupported) {
            uploadFiles();
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


    private void addCommands() {
        Vector<String> vrCommands = null;

        vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_current), getResources().getString(R.string.vr_current_cond)));
        addCommand(VIEW_CURRENT_CONDITIONS, getResources().getString(R.string.cmd_current_cond), vrCommands, autoIncCorrId++);

        vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_daily),
                getResources().getString(R.string.vr_daily_forecast)));
        daily_forecast_cmd_added_corrId = autoIncCorrId;
        addCommand(VIEW_DAILY_FORECAST, getResources().getString(R.string.cmd_daily_forecast), vrCommands, autoIncCorrId++);

        vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_change_units),
                getResources().getString(R.string.vr_units)));
        addCommand(CHANGE_UNITS, getResources().getString(R.string.cmd_change_units), vrCommands, autoIncCorrId++);

        vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_hourly),
                getResources().getString(R.string.vr_hourly_forecast)));
        hourly_forecast_cmd_added_corrId = autoIncCorrId;
        addCommand(VIEW_HOURLY_FORECAST, getResources().getString(R.string.cmd_hourly_forecast), vrCommands, autoIncCorrId++);

        vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_alerts)));
        addCommand(VIEW_ALERTS, getResources().getString(R.string.cmd_alerts), vrCommands, autoIncCorrId++);

    }

    private void createChangeUnitsInteractionChoiceSet() {
        Choice metricChoice = null;
        metricChoice = new Choice();
        metricChoice.setChoiceID(METRIC_CHOICE);
        metricChoice.setMenuName(getResources().getString(R.string.choice_metric_menue));
        metricChoice.setVrCommands(new Vector<String>(Arrays.asList(getResources().getString(R.string.choice_metric_vr))));
        Choice imperialChoice = new Choice();
        imperialChoice.setChoiceID(IMPERIAL_CHOICE);
        imperialChoice.setMenuName(getResources().getString(R.string.choice_imperial_menue));
        imperialChoice.setVrCommands(new Vector<String>(Arrays.asList(getResources().getString(R.string.choice_imperial_vr))));


        CreateInteractionChoiceSet msg = new CreateInteractionChoiceSet(CHANGE_UNITS_CHOICESET, new Vector<Choice>(Arrays.asList(metricChoice, imperialChoice)));
        msg.setCorrelationID(autoIncCorrId++);
        msg.setOnRPCResponseListener(createInteractionChoiceSetListener);

        sdlManager.sendRPC(msg);

    }

    private void subscribeButtons() {
        SubscribeButton msg = new SubscribeButton(ButtonName.PRESET_1);
        msg.setCorrelationID(autoIncCorrId++);
        sdlManager.sendRPC(msg);
    }

    private void uploadFiles() {
        if (mUploadedFiles == null ||
                !mUploadedFiles.contains(APP_ICON)) {
            uploadFile(APP_ICON_NAME);
        }
    }

    private void uploadFile(String fileResource) {
        if (fileResource != null) {
            Bitmap bm = ImageProcessor.getBitmapFromResources(fileResource);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
            PutFile putfileRequest = new PutFile();
            putfileRequest.setSdlFileName(fileResource + ".png");
            putfileRequest.setFileType(FileType.GRAPHIC_PNG);
            putfileRequest.setSystemFile(false);
            putfileRequest.setPersistentFile(true);
            putfileRequest.setCorrelationID(autoIncCorrId++);
            putfileRequest.setBulkData(stream.toByteArray());
            putfileRequest.setOnRPCResponseListener(new OnRPCResponseListener() {
                @Override
                public void onResponse(int correlationId, RPCResponse response) {
                    Log.i(SdlApplication.TAG, String.format(Locale.getDefault(), "PutFile response success: %b", response.getSuccess()));
                    Log.i(SdlApplication.TAG, String.format(Locale.getDefault(), "PutFile response corrId: %d", response.getCorrelationID()));
                    Log.i(SdlApplication.TAG, String.format(Locale.getDefault(), "PutFile response info: %s", response.getInfo()));

                    // Add uploaded files to the list if they're not already there
                    String currentFile = mPutFileMap.get(response.getCorrelationID());
                    if (response.getSuccess() && currentFile != null) {
                        if (mUploadedFiles == null) {
                            mUploadedFiles = new ArrayList<String>();
                        }
                        if (!mUploadedFiles.contains(currentFile)) {
                            mUploadedFiles.add(currentFile);
                        }

                        // Set AppIcon
                        if (mGraphicsSupported && APP_ICON.equals(currentFile)) {
//							SetAppIcon request = new SetAppIcon();
//							request.setSdlFileName(APP_ICON);
//							request.setCorrelationID(autoIncCorrId++);
//							try {
//								proxy.sendRPCRequest(request);
//							} catch (SdlException e) {}
                        } else if (mGraphicsSupported && currentFile.equals(mConditionIconFileName) && (mShowPendingPutFile != null)) {
                            sdlManager.sendRPC(mShowPendingPutFile);
                        }
                    }
                }
            });
            mPutFileMap.put(putfileRequest.getCorrelationID(), putfileRequest.getSdlFileName());
            try {
                stream.close();
                sdlManager.sendRPC(putfileRequest);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

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

            String field1 = "";
            String field2 = "";
            String field3 = "";
            String field4 = "";
            String mediatrack = String.valueOf((int) precipitation) + "%";

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
            sdlManager.getScreenManager().setPrimaryGraphic(new SdlArtwork(mConditionIconFileName, FileType.GRAPHIC_PNG, conditionsID, true ));

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

        Image appImage = null;
        if (mGraphicsSupported) {
            appImage = new Image();
            appImage.setImageType(ImageType.DYNAMIC);
            appImage.setValue(CLEAR_ICON);
        }

        Show showRequest = new Show();
        showRequest.setMainField1(field1);
        showRequest.setMainField2(field2);
        showRequest.setMainField3(field3);
        showRequest.setMainField4(field4);
        showRequest.setMediaTrack(mediatrack);

        Image conditionsImage = null;
        boolean putFilePending = false;
        String mappedName = null;

        if (mGraphicsSupported && mWeatherConditions.conditionIcon != null && forecast_items != null) {
            String imageName = ImageProcessor.getFileFromURL(forecast_items[forecast_item_counter].conditionIcon);
            mappedName = ImageProcessor.getMappedConditionsImageName(imageName, false);
            if (mappedName != null) {
                mConditionIconFileName = mappedName + ".png";
                if (!mUploadedFiles.contains(mConditionIconFileName)) {
                    putFilePending = true;
                }
                conditionsImage = new Image();
                conditionsImage.setValue(mConditionIconFileName);
                conditionsImage.setImageType(ImageType.DYNAMIC);
            }
        }

        showRequest.setAlignment(TextAlignment.LEFT_ALIGNED);
        showRequest.setGraphic(conditionsImage);
        showRequest.setCorrelationID(autoIncCorrId++);
        if (mDisplayType != DisplayType.CID && mDisplayType != DisplayType.NGN) {
            sdlManager.getScreenManager().setSoftButtonObjects(softButtonObjects);
        }

        if (putFilePending) {
            mShowPendingPutFile = showRequest;
            uploadFile(mappedName);
        } else {
            if (showRequest.getGraphic() != null) {
                Log.i(SdlApplication.TAG, String.format(Locale.getDefault(), "Show image: %s", showRequest.getGraphic().getValue()));
            }
            sdlManager.sendRPC(showRequest);
        }

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

            if (HourlyForecast_ChoiceSet_created) {
                delete_HourlyForecast_ChoiceSet_corrId = autoIncCorrId;
                deleteInteractionChoiceSet(mHourlyForecast_ChoiceSetID, autoIncCorrId++);
            }
        } else {
            forecast = mForecast;

            if (DailyForecast_ChoiceSet_created) {
                delete_DailyForecast_ChoiceSet_corrId = autoIncCorrId;
                deleteInteractionChoiceSet(mDailyForecast_ChoiceSetID, autoIncCorrId++);
            }
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

    private Image getImageIcon(URL icon_url) {
        Image conditionsImage = null;
        String mappedName = null;
        if (mGraphicsSupported && mWeatherConditions.conditionIcon != null) {
            String imageName = ImageProcessor.getFileFromURL(icon_url);
            mappedName = ImageProcessor.getMappedConditionsImageName(imageName, false);
            if (mappedName != null) {
                mConditionIconFileName = mappedName + ".png";
                if (!mUploadedFiles.contains(mConditionIconFileName)) {
                    uploadFile(mappedName);
                }
                conditionsImage = new Image();
                conditionsImage.setValue(mConditionIconFileName);
                conditionsImage.setImageType(ImageType.DYNAMIC);

            }
        }
        return conditionsImage;
    }


    private void createForecastChoiceSet() {
        /* Choices for Hourly Forecast to be created */
        if (mActiveInfoType == InfoType.HOURLY_FORECAST) {
            Vector<Choice> commands = new Vector<Choice>();

            Choice listChoice1 = new Choice();
            listChoice1.setChoiceID(CHOICE_ITEM1_ID);
            listChoice1.setMenuName(forecast_items[0].timeString);
            listChoice1.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[0].timeString})));
            if (mGraphicsSupported) {
                listChoice1.setImage(getImageIcon(forecast_items[0].conditionIcon));
            }
            commands.add(listChoice1);

            Choice listChoice2 = new Choice();
            listChoice2.setChoiceID(CHOICE_ITEM2_ID);
            listChoice2.setMenuName(forecast_items[1].timeString);
            listChoice2.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[1].timeString})));
            if (mGraphicsSupported) {
                listChoice2.setImage(getImageIcon(forecast_items[1].conditionIcon));
            }
            commands.add(listChoice2);

            Choice listChoice3 = new Choice();
            listChoice3.setChoiceID(CHOICE_ITEM3_ID);
            listChoice3.setMenuName(forecast_items[2].timeString);
            listChoice3.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[2].timeString})));
            if (mGraphicsSupported) {
                listChoice3.setImage(getImageIcon(forecast_items[2].conditionIcon));
            }
            commands.add(listChoice3);

            Choice listChoice4 = new Choice();
            listChoice4.setChoiceID(CHOICE_ITEM4_ID);
            listChoice4.setMenuName(forecast_items[3].timeString);
            listChoice4.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[3].timeString})));
            if (mGraphicsSupported) {
                listChoice4.setImage(getImageIcon(forecast_items[3].conditionIcon));
            }
            commands.add(listChoice4);

            Choice listChoice5 = new Choice();
            listChoice5.setChoiceID(CHOICE_ITEM5_ID);
            listChoice5.setMenuName(forecast_items[4].timeString);
            listChoice5.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[4].timeString})));
            if (mGraphicsSupported) {
                listChoice5.setImage(getImageIcon(forecast_items[4].conditionIcon));
            }
            commands.add(listChoice5);

            Choice listChoice6 = new Choice();
            listChoice6.setChoiceID(CHOICE_ITEM6_ID);
            listChoice6.setMenuName(forecast_items[5].timeString);
            listChoice6.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[5].timeString})));
            if (mGraphicsSupported) {
                listChoice6.setImage(getImageIcon(forecast_items[5].conditionIcon));
            }
            commands.add(listChoice6);

            Choice listChoice7 = new Choice();
            listChoice7.setChoiceID(CHOICE_ITEM7_ID);
            listChoice7.setMenuName(forecast_items[6].timeString);
            listChoice7.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[6].timeString})));
            if (mGraphicsSupported) {
                listChoice7.setImage(getImageIcon(forecast_items[6].conditionIcon));
            }
            commands.add(listChoice7);

            Choice listChoice8 = new Choice();
            listChoice8.setChoiceID(CHOICE_ITEM8_ID);
            listChoice8.setMenuName(forecast_items[7].timeString);
            listChoice8.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[7].timeString})));
            if (mGraphicsSupported) {
                listChoice8.setImage(getImageIcon(forecast_items[7].conditionIcon));
            }
            commands.add(listChoice8);

            Choice listChoice9 = new Choice();
            listChoice9.setChoiceID(CHOICE_ITEM9_ID);
            listChoice9.setMenuName(forecast_items[8].timeString);
            listChoice9.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[8].timeString})));
            if (mGraphicsSupported) {
                listChoice9.setImage(getImageIcon(forecast_items[8].conditionIcon));
            }
            commands.add(listChoice9);

            Choice listChoice10 = new Choice();
            listChoice10.setChoiceID(CHOICE_ITEM10_ID);
            listChoice10.setMenuName(forecast_items[9].timeString);
            listChoice10.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[9].timeString})));
            if (mGraphicsSupported) {
                listChoice10.setImage(getImageIcon(forecast_items[9].conditionIcon));
            }
            commands.add(listChoice10);

            Choice listChoice11 = new Choice();
            listChoice11.setChoiceID(CHOICE_ITEM11_ID);
            listChoice11.setMenuName(forecast_items[10].timeString);
            listChoice11.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[10].timeString})));
            if (mGraphicsSupported) {
                listChoice11.setImage(getImageIcon(forecast_items[10].conditionIcon));
            }
            commands.add(listChoice11);

            Choice listChoice12 = new Choice();
            listChoice12.setChoiceID(CHOICE_ITEM12_ID);
            listChoice12.setMenuName(forecast_items[11].timeString);
            listChoice12.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[11].timeString})));
            if (mGraphicsSupported) {
                listChoice12.setImage(getImageIcon(forecast_items[11].conditionIcon));
            }
            commands.add(listChoice12);

            if (!commands.isEmpty()) {
                Log.d(SdlApplication.TAG, "send HourlyChoiceSet to SDL");
                CreateInteractionChoiceSet choiceset_rpc = new CreateInteractionChoiceSet();
                create_HourlyForecast_ChoiceSet_corrId = autoIncCorrId;
                choiceset_rpc.setCorrelationID(autoIncCorrId++);
                mHourlyForecast_ChoiceSetID++;
                choiceset_rpc.setInteractionChoiceSetID(mHourlyForecast_ChoiceSetID);
                choiceset_rpc.setChoiceSet(commands);
                choiceset_rpc.setOnRPCResponseListener(createInteractionChoiceSetListener);
                sdlManager.sendRPC(choiceset_rpc);
            }
        }

        /* Choices for Daily Forecast to be created */
        else if (mActiveInfoType == InfoType.DAILY_FORECAST) {
            Vector<Choice> commands = new Vector<Choice>();

            Choice listChoice1 = new Choice();
            listChoice1.setChoiceID(CHOICE_ITEM1_ID);
            listChoice1.setMenuName(getResources().getString(R.string.cmd_today)/*forecast_items[0].fullDateString*/);
            listChoice1.setVrCommands(new Vector<String>(Arrays.asList(new String[]{getResources().getString(R.string.cmd_today)})));
            if (mGraphicsSupported) {
                listChoice1.setImage(getImageIcon(forecast_items[0].conditionIcon));
            }
            commands.add(listChoice1);

            Choice listChoice2 = new Choice();
            listChoice2.setChoiceID(CHOICE_ITEM2_ID);
            listChoice2.setMenuName(getResources().getString(R.string.cmd_tomorrow)/*forecast_items[1].fullDateString*/);
            listChoice2.setVrCommands(new Vector<String>(Arrays.asList(new String[]{getResources().getString(R.string.cmd_tomorrow)})));
            if (mGraphicsSupported) {
                listChoice2.setImage(getImageIcon(forecast_items[1].conditionIcon));
            }
            commands.add(listChoice2);

            Choice listChoice3 = new Choice();
            listChoice3.setChoiceID(CHOICE_ITEM3_ID);
            listChoice3.setMenuName(forecast_items[2].fullDateString);
            listChoice3.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[2].fullDateString})));
            if (mGraphicsSupported) {
                listChoice3.setImage(getImageIcon(forecast_items[2].conditionIcon));
            }
            commands.add(listChoice3);

            Choice listChoice4 = new Choice();
            listChoice4.setChoiceID(CHOICE_ITEM4_ID);
            listChoice4.setMenuName(forecast_items[3].fullDateString);
            listChoice4.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[3].fullDateString})));
            if (mGraphicsSupported) {
                listChoice4.setImage(getImageIcon(forecast_items[3].conditionIcon));
            }
            commands.add(listChoice4);

            Choice listChoice5 = new Choice();
            listChoice5.setChoiceID(CHOICE_ITEM5_ID);
            listChoice5.setMenuName(forecast_items[4].fullDateString);
            listChoice5.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[4].fullDateString})));
            if (mGraphicsSupported) {
                listChoice5.setImage(getImageIcon(forecast_items[4].conditionIcon));
            }
            commands.add(listChoice5);

            Choice listChoice6 = new Choice();
            listChoice6.setChoiceID(CHOICE_ITEM6_ID);
            listChoice6.setMenuName(forecast_items[5].fullDateString);
            listChoice6.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[5].fullDateString})));
            if (mGraphicsSupported) {
                listChoice6.setImage(getImageIcon(forecast_items[5].conditionIcon));
            }
            commands.add(listChoice6);

            Choice listChoice7 = new Choice();
            listChoice7.setChoiceID(CHOICE_ITEM7_ID);
            listChoice7.setMenuName(forecast_items[6].fullDateString);
            listChoice7.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[6].fullDateString})));
            if (mGraphicsSupported) {
                listChoice7.setImage(getImageIcon(forecast_items[6].conditionIcon));
            }
            commands.add(listChoice7);

            Choice listChoice8 = new Choice();
            listChoice8.setChoiceID(CHOICE_ITEM8_ID);
            listChoice8.setMenuName(forecast_items[7].fullDateString);
            listChoice8.setVrCommands(new Vector<String>(Arrays.asList(new String[]{forecast_items[7].fullDateString})));
            if (mGraphicsSupported) {
                listChoice8.setImage(getImageIcon(forecast_items[7].conditionIcon));
            }
            commands.add(listChoice8);

            if (!commands.isEmpty()) {
                Log.d(SdlApplication.TAG, "send DayChoiceSet to SDL");
                CreateInteractionChoiceSet choiceset_rpc = new CreateInteractionChoiceSet();
                create_DailyForecast_ChoiceSet_corrId = autoIncCorrId;
                choiceset_rpc.setCorrelationID(autoIncCorrId++);
                mDailyForecast_ChoiceSetID++;
                choiceset_rpc.setInteractionChoiceSetID(mDailyForecast_ChoiceSetID);
                choiceset_rpc.setChoiceSet(commands);
                choiceset_rpc.setOnRPCResponseListener(createInteractionChoiceSetListener);
                sdlManager.sendRPC(choiceset_rpc);
            }
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
                sdlManager.sendRPC(speakRequest);
            }
        } else {
            Show showRequest = new Show();
            showRequest.setMainField1(getResources().getString(R.string.weather_alerts_txt_field1));
            showRequest.setMainField2(getResources().getString(R.string.weather_alerts_txt_field2));
            showRequest.setMainField3(getResources().getString(R.string.weather_alerts_txt_field3));
            showRequest.setMainField4(getResources().getString(R.string.weather_alerts_txt_field4));
            showRequest.setMediaTrack(getResources().getString(R.string.weather_alerts_txt_mediatrack));
            showRequest.setAlignment(TextAlignment.CENTERED);
            showRequest.setCorrelationID(autoIncCorrId++);
            sdlManager.sendRPC(showRequest);

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


    private void prepareDailyForecastCmds() {
        Vector<String> vrCommands = null;
        if (next_cmd_added == false) {
            next_cmd_added_corrId = autoIncCorrId;
            vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_next)));
            addCommand(NEXT, getResources().getString(R.string.cmd_next), vrCommands, autoIncCorrId++);
        }
        vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_back)));
        addCommand(BACK, getResources().getString(R.string.cmd_back), vrCommands, autoIncCorrId++);

        /* add vc "Tomorrow" */
        if (tomorrow_cmd_added == false) {
            tomorrow_cmd_added_corrId = autoIncCorrId;
            vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_tomorrow)));
            addCommand(TOMORROW, getResources().getString(R.string.cmd_tomorrow), vrCommands, autoIncCorrId++);
        }

        /* add vc "Today" */
        if (today_cmd_added == false) {
            today_cmd_added_corrId = autoIncCorrId;
            vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_today)));
            addCommand(TODAY, getResources().getString(R.string.cmd_today), vrCommands, autoIncCorrId++);
        }

        /* add vc "List" */
        if (list_cmd_added == false) {
            list_cmd_added_corrId = autoIncCorrId;
            vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_list)));
            addCommand(LIST, "List", vrCommands, autoIncCorrId++);
        }

        if (now_cmd_added == true) {
            now_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(NOW, autoIncCorrId++);
        }
        /* delete cmd "Daily Forecast" */
        if (daily_forecast_cmd_added == true) {
            daily_forecast_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(VIEW_DAILY_FORECAST, autoIncCorrId++);
        }
        /* add cmd "Hourly Forecast" */
        if (hourly_forecast_cmd_added == false) {

            vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_hourly),
                    getResources().getString(R.string.vr_hourly_forecast)));
            hourly_forecast_cmd_added_corrId = autoIncCorrId;
            addCommand(VIEW_HOURLY_FORECAST, getResources().getString(R.string.cmd_hourly_forecast), vrCommands, autoIncCorrId++);

        }
    }


    private void prepareHourlyForecastCmds() {
        Vector<String> vrCommands = null;
        if (next_cmd_added == false) {
            next_cmd_added_corrId = autoIncCorrId;
            vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_next)));
            addCommand(NEXT, getResources().getString(R.string.cmd_next), vrCommands, autoIncCorrId++);
        }
        vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_back)));
        addCommand(BACK, getResources().getString(R.string.cmd_back), vrCommands, autoIncCorrId++);
        /* add cmd "Now" */
        now_cmd_added_corrId = autoIncCorrId;
        vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_now)));
        addCommand(NOW, getResources().getString(R.string.cmd_now), vrCommands, autoIncCorrId++);
        /* add cmd "List" */
        if (list_cmd_added == false) {
            list_cmd_added_corrId = autoIncCorrId;
            vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_list)));
            addCommand(LIST, "List", vrCommands, autoIncCorrId++);
        }
        /* delete cmd "Today" */
        if (today_cmd_added == true) {
            today_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(TODAY, autoIncCorrId++);
        }
        /* delete cmd "Tomorrow" */
        if (tomorrow_cmd_added == true) {
            tomorrow_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(TOMORROW, autoIncCorrId++);
        }
        /* delete cmd Hourly Forecast */
        if (hourly_forecast_cmd_added == true) {
            hourly_forecast_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(VIEW_HOURLY_FORECAST, autoIncCorrId++);
        }
        /* add cmd "Daily Forecast" */
        if (daily_forecast_cmd_added == false) {

            vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_daily),
                    getResources().getString(R.string.vr_daily_forecast)));
            daily_forecast_cmd_added_corrId = autoIncCorrId;
            addCommand(VIEW_DAILY_FORECAST, getResources().getString(R.string.cmd_daily_forecast), vrCommands, autoIncCorrId++);
        }
    }

    private void prepareCurrentCondCmds() {
        previous_cmd_deleted_corrId = autoIncCorrId;
        deleteCommand(PREVIOUS, autoIncCorrId++);

        next_cmd_deleted_corrId = autoIncCorrId;
        deleteCommand(NEXT, autoIncCorrId++);

        deleteCommand(BACK, autoIncCorrId++);
        if (now_cmd_added == true) {
            now_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(NOW, autoIncCorrId++);
        }
        if (today_cmd_added == true) {
            today_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(TODAY, autoIncCorrId++);
        }
        if (tomorrow_cmd_added == true) {
            tomorrow_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(TOMORROW, autoIncCorrId++);
        }

        if (list_cmd_added == true) {
            list_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(LIST, autoIncCorrId++);
        }
    }


    private void prepareListItemsCmds() {
        String initialPrompt = "";
        String helpPrompt = "";
        String timeoutPrompt = getResources().getString(R.string.interaction_forecastlist_timeoutprompt);
        String initialText = "";

        if (mActiveInfoType == InfoType.HOURLY_FORECAST) {
            initialPrompt = getResources().getString(R.string.interaction_hourly_forecastlist_initprompt);
            helpPrompt = getResources().getString(R.string.interaction_hourly_forecastlist_helpprompt);
            initialText = getResources().getString(R.string.interaction_hourly_forecastlist_inittext);
        }
        if (mActiveInfoType == InfoType.DAILY_FORECAST) {
            initialPrompt = getResources().getString(R.string.interaction_daily_forecastlist_initprompt);
            helpPrompt = getResources().getString(R.string.interaction_daily_forecastlist_helpprompt);
            initialText = getResources().getString(R.string.interaction_daily_forecastlist_inittext);
        }
        Vector<TTSChunk> intitial_prompt = TTSChunkFactory.createSimpleTTSChunks(initialPrompt);
        Vector<TTSChunk> help_prompt = TTSChunkFactory.createSimpleTTSChunks(helpPrompt);
        Vector<TTSChunk> timeout_prompt = TTSChunkFactory.createSimpleTTSChunks(timeoutPrompt);
        Vector<Integer> interactionChoiceSetIDs = new Vector<Integer>();
        if (mActiveInfoType == InfoType.DAILY_FORECAST) {
            interactionChoiceSetIDs.add(mDailyForecast_ChoiceSetID);
        }
        if (mActiveInfoType == InfoType.HOURLY_FORECAST) {
            interactionChoiceSetIDs.add(mHourlyForecast_ChoiceSetID);
        }

        PerformInteraction performInterActionRequest = new PerformInteraction();
        performInterActionRequest.setInitialPrompt(intitial_prompt);
        performInterActionRequest.setHelpPrompt(help_prompt);
        performInterActionRequest.setTimeoutPrompt(timeout_prompt);
        performInterActionRequest.setInitialText(initialText);
        performInterActionRequest.setTimeout(100000);
        performInterActionRequest.setInteractionChoiceSetIDList(interactionChoiceSetIDs);
        performInterActionRequest.setInteractionMode(InteractionMode.MANUAL_ONLY);
        performInterActionRequest.setCorrelationID(autoIncCorrId++);
        performInterActionRequest.setOnRPCResponseListener(onPerformInteractionResponseListener);
        sdlManager.sendRPC(performInterActionRequest);

    }

    private void prepareBackCmds() {
        Vector<String> vrCommands = null;
        previous_cmd_deleted_corrId = autoIncCorrId;
        deleteCommand(PREVIOUS, autoIncCorrId++);

        next_cmd_deleted_corrId = autoIncCorrId;
        deleteCommand(NEXT, autoIncCorrId++);

        deleteCommand(BACK, autoIncCorrId++);
        if (now_cmd_added) {
            now_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(NOW, autoIncCorrId++);
        }
        if (today_cmd_added) {
            today_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(TODAY, autoIncCorrId++);
        }
        if (tomorrow_cmd_added) {
            tomorrow_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(TOMORROW, autoIncCorrId++);
        }

        if (list_cmd_added) {
            list_cmd_deleted_corrId = autoIncCorrId;
            deleteCommand(LIST, autoIncCorrId++);
        }
        if (HourlyForecast_ChoiceSet_created) {
            delete_HourlyForecast_ChoiceSet_corrId = autoIncCorrId;
            deleteInteractionChoiceSet(mHourlyForecast_ChoiceSetID, autoIncCorrId++);
        }
        if (DailyForecast_ChoiceSet_created) {
            delete_DailyForecast_ChoiceSet_corrId = autoIncCorrId;
            deleteInteractionChoiceSet(mDailyForecast_ChoiceSetID, autoIncCorrId++);
        }

        /* add cmd "Daily Forecast" */
        if (!daily_forecast_cmd_added) {
            vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_daily),
                    getResources().getString(R.string.vr_daily_forecast)));
            daily_forecast_cmd_added_corrId = autoIncCorrId;
            addCommand(VIEW_DAILY_FORECAST, getResources().getString(R.string.cmd_daily_forecast), vrCommands, autoIncCorrId++);
        }

        /* add cmd "Hourly Forecast */
        if (!hourly_forecast_cmd_added) {
            vrCommands = new Vector<String>(Arrays.asList(getResources().getString(R.string.vr_hourly),
                    getResources().getString(R.string.vr_hourly_forecast)));
            hourly_forecast_cmd_added_corrId = autoIncCorrId;
            addCommand(VIEW_HOURLY_FORECAST, getResources().getString(R.string.cmd_hourly_forecast), vrCommands, autoIncCorrId++);

        }

    }

    public void deleteCommand(@NonNull Integer commandID, Integer correlationID) {

        DeleteCommand msg = new DeleteCommand(commandID);
        msg.setCorrelationID(correlationID);
        msg.setOnRPCResponseListener(new OnRPCResponseListener() {
            @Override
            public void onResponse(int correlationId, RPCResponse response) {
                if (response.getSuccess()) {
                    if (response.getCorrelationID() == next_cmd_deleted_corrId) {
                        next_cmd_added = false;
                    }
                    if (response.getCorrelationID() == previous_cmd_deleted_corrId) {
                        previous_cmd_added = false;
                    }
                    if (response.getCorrelationID() == now_cmd_deleted_corrId) {
                        now_cmd_added = false;
                    }
                    if (response.getCorrelationID() == today_cmd_deleted_corrId) {
                        today_cmd_added = false;
                    }
                    if (response.getCorrelationID() == tomorrow_cmd_deleted_corrId) {
                        tomorrow_cmd_added = false;
                    }
                    if (response.getCorrelationID() == list_cmd_deleted_corrId) {
                        list_cmd_added = false;
                    }
                    if (response.getCorrelationID() == daily_forecast_cmd_deleted_corrId) {
                        daily_forecast_cmd_added = false;
                    }
                    if (response.getCorrelationID() == hourly_forecast_cmd_deleted_corrId) {
                        hourly_forecast_cmd_added = false;
                    }
                }
            }
        });
        sdlManager.sendRPC(msg);
    }

    public void addCommand(Integer commandID,
                           String menuText, Vector<String> vrCommands, Integer correlationID) {

        AddCommand msg = new AddCommand(commandID);
        msg.setCorrelationID(correlationID);
        msg.setVrCommands(vrCommands);
        if (menuText != null) {
            MenuParams menuParams = new MenuParams();
            menuParams.setMenuName(menuText);
            msg.setMenuParams(menuParams);
        }
        msg.setOnRPCResponseListener(addCommandResponseListener);
        sdlManager.sendRPC(msg);
    }

    public void speak(@NonNull String ttsText, Integer correlationID) {

        Speak msg = new Speak(TTSChunkFactory.createSimpleTTSChunks(ttsText));
        msg.setCorrelationID(correlationID);

        sdlManager.sendRPC(msg);
    }

    public void deleteInteractionChoiceSet(@NonNull Integer interactionChoiceSetID, Integer correlationID) {

        DeleteInteractionChoiceSet msg = new DeleteInteractionChoiceSet(interactionChoiceSetID);
        msg.setCorrelationID(correlationID);
        msg.setOnRPCResponseListener(new OnRPCResponseListener() {
            @Override
            public void onResponse(int correlationId, RPCResponse response) {
                if (response.getCorrelationID() == delete_DailyForecast_ChoiceSet_corrId) {
                    if (response.getSuccess()) {
                        DailyForecast_ChoiceSet_created = false;
                    }
                }
                if (response.getCorrelationID() == delete_HourlyForecast_ChoiceSet_corrId) {
                    if (response.getSuccess()) {
                        HourlyForecast_ChoiceSet_created = false;
                    }
                }
            }
        });
        sdlManager.sendRPC(msg);
    }

    public void setGlobalProperties(
            String helpPrompt, String timeoutPrompt, Integer correlationID) {

        SetGlobalProperties req = new SetGlobalProperties();
        req.setCorrelationID(correlationID);
        req.setHelpPrompt(TTSChunkFactory.createSimpleTTSChunks(helpPrompt));
        req.setTimeoutPrompt(TTSChunkFactory.createSimpleTTSChunks(timeoutPrompt));

        sdlManager.sendRPC(req);
    }
}
