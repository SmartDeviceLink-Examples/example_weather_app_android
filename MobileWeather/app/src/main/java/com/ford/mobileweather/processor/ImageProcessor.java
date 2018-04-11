package com.ford.mobileweather.processor;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import com.ford.mobileweather.smartdevicelink.SmartDeviceLinkApplication;

public class ImageProcessor {

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
			return mConditionsImageMap.get(conditionsImage) + suffix;
		}
		else {
			return null;
		}
	}
	
	public static Bitmap getBitmapFromResources(String imageName) {
		Resources resources = SmartDeviceLinkApplication.getInstance().getResources();
		int resId = resources.getIdentifier(imageName, "drawable", "com.ford.mobileweather");
		return BitmapFactory.decodeResource(resources, resId);
	}
	
	public static String getFileFromURL(URL url) {
		if (url != null) {
			String urlPath = url.getFile();
			return urlPath.substring(urlPath.lastIndexOf('/') + 1, urlPath.length());
		}
		else {
			return null;
		}
	}
	
	public static void setConditionsImage(ImageView imageView, URL conditionsImageURL, boolean small) {
		String conditionsImageName = getFileFromURL(conditionsImageURL);
		String mappedName = getMappedConditionsImageName(conditionsImageName, small);
		if (mappedName != null) {
			Bitmap mappedImage = getBitmapFromResources(mappedName);
			imageView.setImageBitmap(mappedImage);
		}
		else {
			// TODO
			/*final DownloadTask downloadTask = new DownloadTask(context);
			downloadTask.execute(conditionsImageURL.toString());
			//imageView.setImageBitmap(bm);*/
		}
	}
	
	public static byte[] getConditionsImageBytes(URL conditionsImageURL) {
		String conditionsImageName = getFileFromURL(conditionsImageURL);
		String mappedName = getMappedConditionsImageName(conditionsImageName, false);
		Bitmap bm = null;
		if (mappedName != null) {			
			bm = getBitmapFromResources(mappedName);
		}
		else {
			// TODO
		}
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bm.compress(Bitmap.CompressFormat.PNG, 100, stream);
		return stream.toByteArray();
	}
}


