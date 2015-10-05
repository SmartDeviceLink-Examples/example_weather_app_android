package com.ford.mobileweather.weather;

public final class UnitConverter {
	
	private UnitConverter(){
		// Constructor is private to prevent instantiation of static
		// method provider
	}
	
	/**
	 * Convert length from mm to inches.
	 * @param length
	 * @return
	 */
	public static float convertLengthToImperial(float length) {
		return (float) (length * 0.0393701);
	}

	/**
	 * Convert speed from kph to mph
	 * @param speed
	 * @return
	 */
	public static float convertSpeedToImperial(float speed) {
		return (float) (speed * 0.621371);
	}

	/**
	 * Convert from Celsius to Fahrenheit.
	 * @param temperature the temperature in Celsius.
	 * @return temperature temperature in Fahrenheit.
	 */
	public static float convertTemperatureToImperial(float temperature) {
		return (float) ((temperature * 9 / 5) + 32);
	}
	
	/**
	 * Convert length from inches to mm.
	 * @param length
	 * @return
	 */
	public static float convertLengthToMetric(float length) {
		return (float) (length / 0.0393701);
	}

	/**
	 * Convert speed from mph to kph
	 * @param speed
	 * @return
	 */
	public static float convertSpeedToMetric(float speed) {
		return (float) (speed / 0.621371);
	}

	/**
	 * Convert from Fahrenheit to Celsius.
	 * @param temperature the temperature in Fahrenheit.
	 * @return temperature temperature in Celsius.
	 */
	public static float convertTemperatureToMetric(float temperature) {
		return (float) ((temperature - 32) * 5 / 9);
	}
	
}
