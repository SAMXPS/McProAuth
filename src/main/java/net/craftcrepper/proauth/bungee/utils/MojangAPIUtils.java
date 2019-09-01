package net.craftcrepper.proauth.bungee.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import lombok.AllArgsConstructor;
import lombok.Getter;


public class MojangAPIUtils {

	public static final String API_URL = "https://api.mojang.com/";
	private static HashMap<String, MinecraftAccountInfo> cache = new HashMap<>();
	
	public static MinecraftAccountInfo loadAccountInfo(String name) throws IOException, JSONException, MojangAPIException{
		if (cache.containsKey(name.toLowerCase())){
			MinecraftAccountInfo cached = cache.get(name.toLowerCase());
			// Default 15 min valid time
			if (!cached.hasExpired())
				return cached;
			else cache.remove(name.toLowerCase());
		}
		
		HttpURLConnection conn = HttpUtils.prepareConnection(HttpUtils.createConnection(API_URL + "users/profiles/minecraft/" + name));
		MinecraftAccountInfo $return = null;
		
		if (conn.getResponseCode() == 200){		
			JSONObject response = new JSONObject(HttpUtils.readSourceCode(conn));
			if (response.has("id")){
				$return = new MinecraftAccountInfo(true, response.getString("id"), response.has("name") ? response.getString("name") : name, response.has("legacy"), response.has("demo"));
			} else if (response.has("error")){
				throw new MojangAPIException(response.getString("error"), response.has("errorMessage") ? response.getString("errorMessage") : "");
			}
		} else if (conn.getResponseCode() == 204){
			// Player has invalid account (Offline mode).
			$return = new MinecraftAccountInfo(false, null, name, false, true);
		} else if (conn.getResponseCode() == 429){
			throw new TooManyRequestsException("The client has sent too many requests within a certain amount of time");
		}
		if ($return == null)
			throw new InvalidResponseException();
		else {
			cache.put(name.toLowerCase(), $return);
			return $return;
		}
	}
	
	@AllArgsConstructor
	@Getter
	public static class MinecraftAccountInfo {
		
		private boolean valid;
		private String id;
		private String name;
		private boolean legacy;
		private boolean demo;
		private final long creation = System.currentTimeMillis();
		
		private boolean hasExpired(){
			return System.currentTimeMillis() - creation > 900000;
		}
		
	}	
	
	@Getter
	@AllArgsConstructor
	public static class MojangAPIException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1336494032976206655L;
		public String error;
		public String errorMessage;
		
	}
	
	public static class InvalidResponseException extends MojangAPIException{

		/**
		 * 
		 */
		private static final long serialVersionUID = 8536494668575092739L;
		
		public InvalidResponseException(){
			super("invalidResponseException", "The API JSON response is invalid or has no data");
		}
		
	}
	
	public static class TooManyRequestsException extends MojangAPIException{

		/**
		 * 
		 */
		private static final long serialVersionUID = 6539332228413624000L;
		
		public TooManyRequestsException(String errorMessage){
			super("tooManyRequestsException", errorMessage);
		}
		
	}
}
