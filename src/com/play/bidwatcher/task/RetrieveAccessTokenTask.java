package com.play.bidwatcher.task;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.play.bidwatcher.MainActivity;
import com.play.bidwatcher.dal.OAuthManager;

public class RetrieveAccessTokenTask extends AsyncTask<Void, Void, Void> {	

public static final String TAG = RetrieveAccessTokenTask.class.getSimpleName(); 
	
	protected Context	mContext;
	protected OAuthProvider mProvider;
	protected OAuthConsumer mConsumer;
	protected String mOAuthVerifier = "";
	
	public RetrieveAccessTokenTask(Context context, OAuthConsumer consumer, OAuthProvider provide, String oAuthVerifier){
		mContext = context; 
		mProvider = provide; 
		mConsumer = consumer;
		mOAuthVerifier = oAuthVerifier; 
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		try {
			mProvider.retrieveAccessToken(mConsumer, mOAuthVerifier);
			
			String token = mConsumer.getToken();			
			String tokenSecret = mConsumer.getTokenSecret();
			
			OAuthManager.setOAuthToken(mContext.getApplicationContext(), token, tokenSecret);
			mConsumer.setTokenWithSecret(token, tokenSecret);
			mContext.startActivity(new Intent(mContext, MainActivity.class));
			
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
