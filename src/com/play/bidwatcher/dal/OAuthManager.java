package com.play.bidwatcher.dal;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.play.bidwatcher.trademe.TradeMeApi;


public final class OAuthManager {

	public static final String TAG = OAuthManager.class.getSimpleName();
	
	public static boolean hasValidOAuthToken(Context context){
		SharedPreferences store = context.getSharedPreferences(TAG, Context.MODE_MULTI_PROCESS);
		if(store.contains("token_timestamp")){
			long elapsedTime = System.currentTimeMillis() - store.getLong("token_timestamp", 0L);
			return TimeUnit.DAYS.toDays(elapsedTime) > 10;  
		}
		
		return false; 
	}
	
	public static HashMap<String, String> getOAuthToken(Context context){
		SharedPreferences store = context.getSharedPreferences(TAG, Context.MODE_MULTI_PROCESS);
		String token = store.getString("token", "");
		String tokenSecret = store.getString("token_secret", "");
		
		HashMap<String, String> result = new HashMap<String, String>(); 
		result.put("token", token);
		result.put("token_secret", tokenSecret);
		
		return result; 
	}
	
	public static void setOAuthToken(Context context, String token, String tokenSecret){
		Log.i(TAG, String.format("setOAuthToken - Token: %s Token Secret: %s", token, tokenSecret));
		
		SharedPreferences store = context.getSharedPreferences(TAG, Context.MODE_MULTI_PROCESS);
		SharedPreferences.Editor editor = store.edit();		
		editor.putString("token", token);
		editor.putString("token_secret", tokenSecret);
		editor.putLong("token_timestamp", System.currentTimeMillis());
		editor.commit();
	}
	
	public static OAuthConsumer getOAuthConsumer(Context context) {
		HashMap<String, String> issuedToken = getOAuthToken(context);
		
		String token = issuedToken.get("token");
		String tokenSecret = issuedToken.get("token_secret");
		
		if(TextUtils.isEmpty(token) || TextUtils.isEmpty(tokenSecret)){
			return null; 
		}
		
		OAuthConsumer consumer = new CommonsHttpOAuthConsumer(TradeMeApi.CONSUMER_KEY, TradeMeApi.CONSUMER_SECRET);
		consumer.setTokenWithSecret(token, tokenSecret);
		return consumer;
	}
	
}
