package com.play.bidwatcher.task;

import java.io.IOException;
import java.util.Date;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.play.bidwatcher.model.WatchListItem;
import com.play.bidwatcher.trademe.TradeMeApi;

public class BidTask extends AsyncTask<Void, Void, WatchListItem> {
	
	public static final String TAG = BidTask.class.getSimpleName();  
	
	public static final int BID_ERROR_CODE_TOO_HIGH = 1001; 
	public static final int BID_ERROR_CODE_TOO_LOW 	= 1002;	

	public interface BidTaskListener{
		
		public void onBidTaskFailed(int errorCode, String errorMessage);
		
		public void onBidTaskComplete(WatchListItem watchListItem);
	}
	
	protected OAuthConsumer mConsumer;
	protected BidTaskListener mListener;
	protected int mListingId = 0; 
	protected float mBidAmount = 0.0f; 
	
	protected int mErrorCode = -1; 
	protected String mErrorMessage = null;
	
	public BidTask(OAuthConsumer consumer, int listingId, float bidAmount, BidTaskListener listener){
		mConsumer = consumer; 
		mListingId = listingId; 
		mBidAmount = bidAmount; 
		mListener = listener; 		
	}
	
	@Override
	protected WatchListItem doInBackground(Void... params) {
		// TODO: handle multiple pages 
		
		WatchListItem watchListItem = null;
				
	    try {
	    	HttpClient httpclient = new DefaultHttpClient();
	    	HttpPost httppost = new HttpPost(TradeMeApi.getBidUrl());
	    	httppost.setHeader("Accept", "application/json");
	    	httppost.setHeader("Content-type", "application/json");
	    	
	    	// add post data 
	    	JSONObject postData = new JSONObject();
	    	postData.put("ListingId", mListingId);
	    	postData.put("Amount", mBidAmount); 
	    	postData.put("AutoBid", false); // TODO: allow user to explicitly set this (or derive based on previous behaviour) 
	    	postData.put("ShippingOption", 1); // TODO: allow user to explicitly set this or derive best option e.g. if in same town then select 'pick up' 	    		    		    	
	    	httppost.setEntity(new StringEntity(postData.toString(), "UTF8"));
	    	
			mConsumer.sign(httppost);
			
			Log.i(TAG, "Posting " + httppost.getURI().toString());
			Log.i(TAG, "Post Data: " + postData.toString());
			
			HttpResponse httpResponse = httpclient.execute(httppost);
			
			if (httpResponse == null || (httpResponse.getStatusLine().getStatusCode() < 200 && httpResponse.getStatusLine().getStatusCode() > 200)){
	    		Log.e(TAG, "doInBackground.http error");
                mErrorCode = (httpResponse != null ? httpResponse.getStatusLine().getStatusCode() : 1000);
                mErrorMessage = (httpResponse != null ? httpResponse.getStatusLine().getReasonPhrase() : "Unknown error");                
            } else{            	
            	HttpEntity httpEntity = httpResponse.getEntity();
    	        String responseBody = EntityUtils.toString(httpEntity);
    	        
    	        Log.i(TAG, responseBody);
    	        
    	        JSONObject result = new JSONObject(responseBody);
    	        
    	        boolean success = result.getBoolean("Success");
    	        
    	        if( success ){
    	        	if(result.has("IsTooHigh") && result.getBoolean("IsTooHigh")){
    	        		mErrorCode = BID_ERROR_CODE_TOO_HIGH;
    	        	} else if(result.has("IsTooLow") && result.getBoolean("IsTooLow")){
    	        		mErrorCode = BID_ERROR_CODE_TOO_LOW;
    	        	} else{
    	        		mErrorCode = 1000;
    	        	}    	        	
    	        	mErrorMessage = result.getString("Description");
    	        } else{
    	        	boolean isReserveMet = result.has("IsReserveMet") ? result.getBoolean("IsReserveMet") : false;
    	        	watchListItem = new WatchListItem();
    	        	watchListItem.mListingId = mListingId; 
    	        	watchListItem.mIsReserveMet = isReserveMet; 
    	        	watchListItem.mMaxBidAmount = mBidAmount; 
    	        }    	        
            }
			
		} catch (OAuthMessageSignerException e) {
			Log.e(TAG, e.toString());
			mErrorCode = e.hashCode();
			mErrorMessage = e.toString(); 
		} catch (OAuthExpectationFailedException e) {
			Log.e(TAG, e.toString());
			mErrorCode = e.hashCode();
			mErrorMessage = e.toString();
		} catch (OAuthCommunicationException e) {
			Log.e(TAG, e.toString());
			mErrorCode = e.hashCode();
			mErrorMessage = e.toString();
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.toString());
			mErrorCode = e.hashCode();
			mErrorMessage = e.toString();
		} catch (IOException e) {
			Log.e(TAG, e.toString());
			mErrorCode = e.hashCode();
			mErrorMessage = e.toString();
		} catch (JSONException e) {
			Log.e(TAG, e.toString());
			mErrorCode = e.hashCode();
			mErrorMessage = e.toString();
		}
	    
		return watchListItem;
	}	
	
	protected Date parseDate(String dateString){
		Date parsedDate = null;
		dateString = dateString.replaceAll("[^0-9]", "");
		if (!TextUtils.isEmpty(dateString)){
            try {
            	parsedDate = new Date(Long.parseLong(dateString));
            } catch (NumberFormatException e) {
            	Log.e(TAG, "parseDate: " + e.toString());
            }
        }		
		return parsedDate; 
	}
	
	@Override
	protected void onPostExecute(WatchListItem result) {
		if( mListener != null ){
			if( mErrorCode > 0 ){
				mListener.onBidTaskFailed(mErrorCode, mErrorMessage);
			} else{
				mListener.onBidTaskComplete(result);
			}
		}		
	}

}
