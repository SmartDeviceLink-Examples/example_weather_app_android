package com.ford.mobileweather.localization;

import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration; 

import com.ford.mobileweather.R;
import com.smartdevicelink.proxy.rpc.enums.Language;
import com.ford.mobileweather.forecastio.ForecastIoService;
import com.ford.mobileweather.smartdevicelink.SmartDeviceLinkApplication;
import com.ford.mobileweather.weather.WeatherDataManager;
import com.ford.mobileweather.weather.WeatherUpdateWakefulReceiver;

public class LocalizationUtil {
	private static LocalizationUtil instance;
	// variables to save the original and adjusted Locales
	private String mSavedLocaleCountry = "US";    
	private String mSavedLocaleLanguage = "en";   
	private String mAdjustedLocaleCountry = "US";  
	private String mAdjustedLocaleLanguage = "en";  

	private final Object mSavedLocaleCountryLock = new Object();
	private final Object mSavedLocaleLanguageLock = new Object();
	private final Object mAdjustedLocaleCountryLock = new Object();
	private final Object mAdjustedLocaleLanguageLock = new Object();

	static{
		instance = null;
	}

	private static synchronized void setInstance(LocalizationUtil loc){
		instance = loc;
	}

	public static synchronized LocalizationUtil getInstance(){
		return instance;
	}

	public LocalizationUtil(){
		LocalizationUtil.setInstance(this);
	}


	public String getLocaleCountry(){
		synchronized (mSavedLocaleCountryLock){
			return mSavedLocaleCountry;
		}
	}

	public void setLocaleCountry(String country){
		synchronized (mSavedLocaleCountryLock) {
			this.mSavedLocaleCountry = country;
		}
	}	

	public String getLocaleLanguage(){
		synchronized (mSavedLocaleLanguageLock){
			return mSavedLocaleLanguage;
		}
	}

	public void setLocaleLanguage(String language){
		synchronized (mSavedLocaleLanguageLock){
			this.mSavedLocaleLanguage = language;
		}
	}	

	public String getAdjustedLocaleCountry(){
		synchronized (mAdjustedLocaleCountryLock){
			return mAdjustedLocaleCountry;
		}
	}

	public void setAdjustedLocaleCountry(String country){
		synchronized (mAdjustedLocaleCountryLock){
			this.mAdjustedLocaleCountry = country;
		}
	}	

	public String getAdjustedLocaleLanguage(){
		synchronized (mAdjustedLocaleLanguageLock){
			return mAdjustedLocaleLanguage;
		}
	}

	public void setAdjustedLocaleLanguage(String language){
		synchronized (mAdjustedLocaleLanguageLock){
			this.mAdjustedLocaleLanguage = language;
		}
	}	



	public void determineLocale(Language sync_language) {			
		// derive locales for device, get language (VR+TTS) that is used currently by SYNC voice engine(VR+TTS)
		mSavedLocaleCountry = (Locale.getDefault()).getCountry();
		mSavedLocaleLanguage = (Locale.getDefault()).getLanguage();

		switch(sync_language){
		/*
		 * Map codes for the representation of languages according to ISO 639-1 Code
		 * Map codes for the representation of countries according to ISO 3166-2 Code
		 */	 
				
		case EN_GB:
			mAdjustedLocaleLanguage = "en";
			mAdjustedLocaleCountry = "GB";
			break;
		case EN_US:
			mAdjustedLocaleLanguage ="en";
			mAdjustedLocaleCountry = "US";
			break;
		case DE_DE:
			mAdjustedLocaleLanguage = "de";
			mAdjustedLocaleCountry = "DE";
			break;
		case PT_BR:
			mAdjustedLocaleLanguage = "pt";
			mAdjustedLocaleCountry = "BR";
			break;
		case PT_PT:
			mAdjustedLocaleLanguage = "pt";
			mAdjustedLocaleCountry = "PT";
			break;
		case ES_ES:
			mAdjustedLocaleLanguage = "es";
			mAdjustedLocaleCountry = "ES";
			break;
		case ES_MX:
			mAdjustedLocaleLanguage = "es";
			mAdjustedLocaleCountry = "MX";
			break;			
			/* fall through for the countries localization is not yet available */ 
		case AR_SA:/*
				mAdjustedLocaleLanguage = "ar";
				mAdjustedLocaleCountry = "SA";
				break;*/
		case CS_CZ:/*
				mAdjustedLocaleLanguage = "cs";
				mAdjustedLocaleCountry = "CZ";
				break;*/
		case DA_DK:/*
				mAdjustedLocaleLanguage = "da";
				mAdjustedLocaleCountry = "DK";
				break;*/
		case EN_AU:/*
				mAdjustedLocaleLanguage = "en";
				mAdjustedLocaleCountry = "AU";
				break;*/
		case FR_CA:/*
				mAdjustedLocaleLanguage = "fr";
				mAdjustedLocaleCountry = "CA";
				break;*/
		case FR_FR:/*
				mAdjustedLocaleLanguage = "fr";
				mAdjustedLocaleCountry = "FR";
				break;*/
		case IT_IT:/*
				mAdjustedLocaleLanguage = "it";
				mAdjustedLocaleCountry = "IT";
				break;*/
		case JA_JP:/*
				mAdjustedLocaleLanguage = "ja";
				mAdjustedLocaleCountry = "JP"	;
				break;*/
		case KO_KR:/*
				mAdjustedLocaleLanguage = "ko";
				mAdjustedLocaleCountry = "KR";
				break;*/
		case NL_NL:/*
				mAdjustedLocaleLanguage = "nl";
				mAdjustedLocaleCountry = "NL";
				break;*/
		case NO_NO:/*
				mAdjustedLocaleLanguage = "no";
				mAdjustedLocaleCountry = "NO";
				break;*/
		case PL_PL:/*
				mAdjustedLocaleLanguage = "pl";
				mAdjustedLocaleCountry = "PL";
				break;*/
		case RU_RU:/*
				mAdjustedLocaleLanguage = "ru";
				mAdjustedLocaleCountry = "RU";
				break;*/
		case SV_SE:/*
				mAdjustedLocaleLanguage = "sv";
				mAdjustedLocaleCountry = "SE";
				break;*/
		case TR_TR:/*
				mAdjustedLocaleLanguage = "tr";
				mAdjustedLocaleCountry = "TR";
				break;*/
		case ZH_CN:/*
			    mAdjustedLocaleLanguage = "zh";
				mAdjustedLocaleCountry = "CN";
				break;*/
		case ZH_TW:/*
				mAdjustedLocaleLanguage = "zh";
				mAdjustedLocaleCountry = "TW";
				break;*/
		default:
			mAdjustedLocaleLanguage = "en";
			mAdjustedLocaleCountry = "US";
			break;
		}	
		return;
	}	

	public void changeLocale(String language, String country, Context context){
		// change locale to enforce usage of localization resource
		WeatherDataManager mDataManager;
		Configuration config = context.getResources().getConfiguration();
		Locale locale = new Locale(language, country);
		Locale.setDefault(locale);
		Configuration conf = new Configuration(config);
		conf.locale = locale;
		context.getResources().updateConfiguration(conf, context.getResources().getDisplayMetrics());
		mDataManager = WeatherDataManager.getInstance();
		if(mDataManager != null){
			mDataManager.setUnits(context.getResources().getString(R.string.units_default));			
		}
		Context mAppContext =  SmartDeviceLinkApplication.getInstance().getApplicationContext();
		Intent mUpdateIntent = new Intent(mAppContext, WeatherUpdateWakefulReceiver.class);
    	mUpdateIntent.putExtra("weather_update_service", ForecastIoService.class.getName());
    	mAppContext.sendBroadcast(mUpdateIntent);
		return;
	}
}
