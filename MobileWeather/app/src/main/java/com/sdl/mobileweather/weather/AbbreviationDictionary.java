package com.sdl.mobileweather.weather;

import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.Log;

import com.sdl.mobileweather.R;
import com.sdl.mobileweather.smartdevicelink.SdlApplication;

public class AbbreviationDictionary {
	
	private static HashMap<String, String> mAbrvDictionary = null;	
	private static Object mDictionaryLock = new Object();
	
	public static String lookUp(String term){
		synchronized (mDictionaryLock) {
			return  mAbrvDictionary.get(term);
		}		
	}
	
	public static boolean isPrepared(){		
		synchronized (mDictionaryLock) {
			return (mAbrvDictionary != null);
		}
	}
	
	public static boolean loadDictionary(Context context){
		synchronized (mDictionaryLock) {
			XmlResourceParser parser = context.getResources().getXml(R.xml.weather_abrv);
			mAbrvDictionary = new HashMap<String, String>();
			String key = null;
			String value = null;
			try {
				int eventType = parser.getEventType();

				while (eventType != XmlPullParser.END_DOCUMENT) {
					if (eventType == XmlPullParser.START_DOCUMENT) {
						
						Log.i(SdlApplication.TAG,
								"START_DOCUMENT: Loading abbreviation XML file");
					} else if (eventType == XmlPullParser.START_TAG) {
						if (parser.getName().equals("entry")) {
							Log.d(SdlApplication.TAG, "START_TAG");
							key = parser.getAttributeValue(null, "key");
							if (key == null) {
								parser.close();
								break;
							}
						}
					} else if (eventType == XmlPullParser.TEXT) {
						if (key != null) {
							Log.d(SdlApplication.TAG, "TEXT");
							value = parser.getText();
						}

					} else if (eventType == XmlPullParser.END_TAG) {
						if (parser.getName().equals("entry")) {
							mAbrvDictionary.put(key, value);
							Log.v(SdlApplication.TAG,
									String.format("END_TAG: %s, %s", key, value));
							key = null;
							value = null;
						}
					}
					eventType = parser.next();
				}
			} catch (Exception e) {
				e.printStackTrace();
				Log.d(SdlApplication.TAG,
						"Loading exception: mAbrvDictionary=null");
				mAbrvDictionary = null;
				return false;
			}
			return true;
		}	
	}
	
}
