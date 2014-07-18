package com.play.bidwatcher.task;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.play.bidwatcher.trademe.TradeMeApi;

public class OAuthRequestTokenTask extends AsyncTask<Void, Void, Void> {

	public static final String TAG = OAuthRequestTokenTask.class.getSimpleName(); 
	
	protected Context	mContext;
	protected OAuthProvider mProvider;
	protected OAuthConsumer mConsumer;
	
	public OAuthRequestTokenTask(Context context, OAuthConsumer consumer, OAuthProvider provide){
		mContext = context; 
		mProvider = provide; 
		mConsumer = consumer;
	}
	
	@Override
	protected Void doInBackground(Void... params) {	
		
		try {
			String url = mProvider.retrieveRequestToken(mConsumer, TradeMeApi.CALLBACK_URL);
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_FROM_BACKGROUND);
			mContext.startActivity(intent);
		} catch (OAuthMessageSignerException e) {
			Log.e(TAG, "OAuthMessageSignerException; " + e.toString());
		} catch (OAuthNotAuthorizedException e) {
			Log.e(TAG, "OAuthNotAuthorizedException; " + e.toString());
		} catch (OAuthExpectationFailedException e) {
			Log.e(TAG, "OAuthExpectationFailedException; " + e.toString());
		} catch (OAuthCommunicationException e) {
			Log.e(TAG, "OAuthCommunicationException; " + e.toString());
		}		
		
		return null;
	}

}
