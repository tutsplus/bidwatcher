package com.play.bidwatcher.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.play.bidwatcher.model.WatchListItem;
import com.play.bidwatcher.trademe.TradeMeApi;

public class RetrieveWatchListTask extends AsyncTask<Void, Void, List<WatchListItem>> {
	
	public static final String TAG = RetrieveWatchListTask.class.getSimpleName();  

	public interface RetrieveWatchListTaskListener{
		
		public void onRetrieveWatchListTaskFailed(int errorCode, String errorMessage);
		
		public void onRetrieveWatchListTaskComplete(List<WatchListItem> watchListItems);
	}
	
	protected OAuthConsumer mConsumer;
	protected RetrieveWatchListTaskListener mListener;
	
	protected int mErrorCode = -1; 
	protected String mErrorMessage = null;
	
	public RetrieveWatchListTask(OAuthConsumer consumer, RetrieveWatchListTaskListener listener){
		mConsumer = consumer; 
		mListener = listener; 
	}
	
	@Override
	protected List<WatchListItem> doInBackground(Void... params) {
		// TODO: handle multiple pages 
		
		List<WatchListItem> watchListItems = new ArrayList<WatchListItem>();
				
	    try {
	    	HttpClient httpclient = new DefaultHttpClient();
		    HttpGet httpget = new HttpGet(TradeMeApi.getWatchListUrl());
		    httpget.setHeader("Accept", "application/json");
		    httpget.setHeader("Content-type", "application/json");
			mConsumer.sign(httpget);
			
			Log.i(TAG, "Posting " + httpget.getURI().toString());
			
			HttpResponse httpResponse = httpclient.execute(httpget);
			
			if (httpResponse == null || (httpResponse.getStatusLine().getStatusCode() < 200 && httpResponse.getStatusLine().getStatusCode() > 200)){
	    		Log.e(TAG, "doInBackground.http error");
                mErrorCode = (httpResponse != null ? httpResponse.getStatusLine().getStatusCode() : 1000);
                mErrorMessage = (httpResponse != null ? httpResponse.getStatusLine().getReasonPhrase() : "Unknown error");                
            } else{            	
            	HttpEntity httpEntity = httpResponse.getEntity();
    	        String responseBody = EntityUtils.toString(httpEntity);
    	        
    	        Log.i(TAG, responseBody);
    	        
    	        JSONObject result = new JSONObject(responseBody);
    	        
    	        if(result.has("List")){
    	        	JSONArray listArray = result.getJSONArray("List");
    	        	int listArrayLen = listArray.length(); 
    	        	for( int i=0; i<listArrayLen; i++ ){
    	        		WatchListItem watchListItem = jsonObjectToWatchListItem(listArray.getJSONObject(i));
    	        		if( watchListItem != null ){
    	        			Log.i(TAG, "doInBackground; adding " + watchListItem.toString());
    	        			watchListItems.add(watchListItem);
    	        		}    	        		
    	        	}
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
	    
		return watchListItems;
	}
	
	protected WatchListItem jsonObjectToWatchListItem(JSONObject jsonObject){				
		try {
			WatchListItem item = new WatchListItem();
			
			item.mListingId = jsonObject.getInt("ListingId");
			item.mTitle = jsonObject.getString("Title");
			item.mStartPrice = (float)jsonObject.getDouble("StartPrice");
			item.mStartDate = parseDate(jsonObject.getString("StartDate"));
			item.mEndDate = parseDate(jsonObject.getString("EndDate"));
			item.mMaxBidAmount = (float)jsonObject.getDouble("MaxBidAmount");
			item.mPictureHref = jsonObject.getString("PictureHref");
			item.mBidCount = jsonObject.getInt("BidCount");
			item.mIsReserveMet = jsonObject.getBoolean("IsReserveMet");
			if(jsonObject.has("jsonObject")){
				item.mHasReserve = jsonObject.getBoolean("HasReserve");
			}
			if(jsonObject.has("IsLeading")){
				item.mIsLeading = jsonObject.getBoolean("IsLeading");
			}			
			if(jsonObject.has("IsOutbid")){
				item.mIsOutbid = jsonObject.getBoolean("IsOutbid");
			}
			
			return item; 
		} catch (JSONException e) {
			Log.e(TAG, "jsonObjectToWatchListItem: " + e.toString());
			return null;
		}				
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
	protected void onPostExecute(List<WatchListItem> result) {
		if( mListener != null ){
			if( mErrorCode > 0 ){
				mListener.onRetrieveWatchListTaskFailed(mErrorCode, mErrorMessage);
			} else{
				mListener.onRetrieveWatchListTaskComplete(result);
			}
		}		
	}

}
