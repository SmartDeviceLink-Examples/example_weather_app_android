package com.ford.mobileweather.connection;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HttpConnection {
	
	public enum RequestMethod { GET, PUT, POST, DELETE };
	private static final Integer[] httpSuccessCodes = { 200, 201, 202, 203, 204, 205, 206, 207, 208, 226};
	
	/**
	 * Returns the result of an HTTP request.
	 * 
	 * @param url the request URL
	 * @param requestMethod the type of HTTP request to perform
	 * @param dataToSend the data to send to the server 
	 * @return the result of the request
	 */
	public String sendRequest(URL url, RequestMethod requestMethod, String dataToSend, String contentType) {
		HttpURLConnection httpConnection = null;
		
		// Open the URL, send the request and any data, and read the result into a string
		try {
			// Open the connection
			httpConnection = (HttpURLConnection) url.openConnection();
			return performRequest(httpConnection, requestMethod, dataToSend, contentType);
		}
		catch (ProtocolException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(httpConnection != null)
				httpConnection.disconnect();
		}

		return null;
	}
	
	/**
	 * Performs an HTTP request.
	 * @param httpConnection the HTTP connection.
	 * @param requestMethod the HTTP RequestMethod.
	 * @param dataToSend the data to send with the request, if any.
	 * @param contentType the content type of the request.
	 * @return the result of the request as a String.
	 */
	private String performRequest(HttpURLConnection httpConnection, RequestMethod requestMethod, String dataToSend, String contentType) {
		BufferedReader httpBufferedReader = null;
		String currentLine = null;
		DataOutputStream postDataWriter = null;
		String httpData = "";
				
		// Open the URL, send the request and any data, and read the result into a string
		try {
			httpConnection.setRequestMethod(requestMethod.name());
			httpConnection.setDoInput(true);

			// For certain methods, we need to send data
			if ((requestMethod == RequestMethod.PUT || requestMethod == RequestMethod.POST) && (dataToSend != null)) {
				httpConnection.setDoOutput(true);
				httpConnection.setUseCaches(false);
				if (contentType != null) {
					httpConnection.setRequestProperty("Content-Type", contentType);
				}
				else {
					httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				}
				httpConnection.setRequestProperty("Content-Language", "en-US");
				httpConnection.setRequestProperty("Content-Length", "" + Integer.toString(dataToSend.getBytes().length));
			
				postDataWriter = new DataOutputStream(httpConnection.getOutputStream());
				postDataWriter.writeBytes(dataToSend);
				postDataWriter.flush();
				postDataWriter.close();
			}
			else {
				httpConnection.setDoOutput(false);
			}

			// Get the HTTP response code
			int responseCode = httpConnection.getResponseCode();
			
			List<Integer> codes = Arrays.asList(httpSuccessCodes);
			// If it doesn't equal 'OK' or we performed a DELETE, return the code and message
			if (!codes.contains(Integer.valueOf(responseCode)) || requestMethod == RequestMethod.DELETE) {
				httpData = String.format(Locale.getDefault(), "STATUS=%d:%s", responseCode, httpConnection.getResponseMessage());
			}
			else {
				// Get any output from the site
				httpBufferedReader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
				while ((currentLine = httpBufferedReader.readLine()) != null) {
					httpData += currentLine;
				}
			}
		}
		catch (ProtocolException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if(httpConnection != null)
				httpConnection.disconnect();
		}

		return httpData;
	}

	/**
	 * Checks for an HTTP error message from the sendRequest() method.
	 * @param statusString the results of the request.
	 * @return the HTTP error message, if any occurred.
	 */
	public String getErrorMessage(String statusString) {
		if (statusString != null && statusString.startsWith("STATUS=")) {
			int separator = statusString.indexOf(":");
			if (separator > 0 && statusString.length() > separator) {
				return statusString.substring(separator + 1);
			}
		}
		return null;
	}

	/**
	 * Converts an HTTP error message from the sendRequest() method
	 * into the HTTP status code.
	 * 
	 * @param statusString the results from the request.
	 * @return the HTTP status code.
	 */
	public int getStatusCode(String statusString) {
		if (statusString != null && statusString.startsWith("STATUS=")) {
			int separator = statusString.indexOf(":");
			if (separator > 0) {
				try {
					return Integer.parseInt(statusString.substring(7, separator));
				}
				catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
		} else if (statusString == null){
			// a null statusString may indicate a network connection error.
			return -2;
		}
		return -1;
	}	
}
