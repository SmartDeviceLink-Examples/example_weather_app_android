package com.sdl.mobileweather.processor;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.ImageView;

import com.sdl.mobileweather.BuildConfig;
import com.sdl.mobileweather.smartdevicelink.SdlApplication;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

public class ImageProcessor {
	private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private static final String IMAGE_DIR = "imageDir";
	private static final String PNG_EXTENSION = ".png";
	private static final int BITMAP_QUALITY = 100;
	private static final String TAG = "ImageProcessor";
	private static final String RESOURCE_SCHEME = "android.resource://";
	private static final String DRAWABLE_FOLDER = "drawable";
	private static final String ICON_RESOURCE_TEMPLATE = "ic_%s";

	private static final Map<String, String> mConditionsImageMap = 
		    Collections.unmodifiableMap(new HashMap<String, String>() {
				private static final long serialVersionUID = 1L;
			{ 
		        put("sunny.gif", "clearday");
		        put("clear.gif", "clearday");
		        put("mostlysunny.gif", "partlycloudyday");
		        put("partlysunny.gif", "partlysunny");
		        put("hazy.gif", "hazy");
		        put("fog.gif", "fogcloudy");
		        put("partlycloudy.gif", "partlycloudyday");
		        put("chancetstorms.gif", "chancethunderstorm");
		        put("tstorms.gif", "thunderstorm");
		        put("chancerain.gif", "lightrain");
		        put("rain.gif", "rain");
		        put("chanceflurries.gif", "chancesnow");
		        put("flurries.gif", "lightsnow");
		        put("chancesnow.gif", "chancesnow");
		        put("snow.gif", "snow");
		        put("chancesleet.gif", "sleet");
		        put("sleet.gif", "sleet");
		        put("mostlycloudy.gif", "partlysunny");
		        put("cloudy.gif", "cloudy");
		        
		        put("nt_sunny.gif", "clearday");
		        put("nt_clear.gif", "clearday");
		        put("nt_mostlysunny.gif", "partlycloudyday");
		        put("nt_partlysunny.gif", "partlysunny");
		        put("nt_hazy.gif", "hazy");
		        put("nt_fog.gif", "fogcloudy");
		        put("nt_partlycloudy.gif", "partlycloudyday");
		        put("nt_chancetstorms.gif", "chancethunderstorm");
		        put("nt_tstorms.gif", "thunderstorm");
		        put("nt_chancerain.gif", "lightrain");
		        put("nt_rain.gif", "rain");
		        put("nt_chanceflurries.gif", "chancesnow");
		        put("nt_flurries.gif", "lightsnow");
		        put("nt_chancesnow.gif", "chancesnow");
		        put("nt_snow.gif", "snow");
		        put("nt_chancesleet.gif", "sleet");
		        put("nt_sleet.gif", "sleet");
		        put("nt_mostlycloudy.gif", "partlysunny");
		        put("nt_cloudy.gif", "cloudy");
		        
		        put("clear-day.gif", "clearday");
		        put("clear-night.gif", "clearnight");
		        //put("rain.gif", "rain");
		        //put("snow.gif", "snow");
		        //put("sleet.gif", "sleet");
		        put("wind.gif", "windycloudy");
		        //put("fog.gif", "fog");
		        //put("cloudy.gif", "cloudy");
		        put("partly-cloudy-day.gif", "partlycloudyday");
		        put("partly-cloudy-night.gif", "partlycloudynight");
		    }});
	
	public static String getMappedConditionsImageName(String conditionsImage, boolean small) {
		String suffix = "";
		if (small) {
			suffix = "_50";
		}
		if (mConditionsImageMap.containsKey(conditionsImage)) {
			return mConditionsImageMap.get(conditionsImage);
		}
		else {
			return null;
		}
	}
	
	public static Bitmap getBitmapFromResources(String conditionId) {
		String imageName = String.format(ICON_RESOURCE_TEMPLATE, conditionId);
		Resources resources = SdlApplication.getInstance().getResources();
		int resId = resources.getIdentifier(imageName, "drawable", "com.sdl.mobileweather");
		return BitmapFactory.decodeResource(resources, resId);
	}

	public static byte[] getBytesFromURL(URL  url) {
		byte[] bytes = null;
		HttpsURLConnection connection = null;
		try {
			connection = (HttpsURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream inputStream = connection.getInputStream();
			final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
			int size = bitmap.getHeight() * bitmap.getRowBytes();
			ByteBuffer buffer = ByteBuffer.allocate(size);
			bitmap.copyPixelsToBuffer(buffer);
			bytes = buffer.array();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return bytes;
	}

	public static Uri getImageUriFromURL(String imageName, URL url, Context context) {
		return Uri.parse(url.toString());
	}
	
	public static String getFileFromURL(URL url) {
		if (url != null) {
			return url.getFile();
		}
		else {
			return null;
		}
	}
	
	public static void setConditionsImage(ImageView imageView, String conditionId) {

		Bitmap bitmap = getBitmapFromResources(conditionId);

		imageView.setImageBitmap(bitmap);
	}

	public static Uri getWeatherIconUri(String iconId) {

		String iconFileString = String.format(ICON_RESOURCE_TEMPLATE, iconId);
		return Uri.parse(RESOURCE_SCHEME + BuildConfig.APPLICATION_ID + "/" + DRAWABLE_FOLDER + "/" + iconFileString);
	}
}


