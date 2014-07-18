package com.play.bidwatcher;

import java.io.UnsupportedEncodingException;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.play.bidwatcher.task.OAuthRequestTokenTask;
import com.play.bidwatcher.task.RetrieveAccessTokenTask;
import com.play.bidwatcher.trademe.TradeMeApi;

public class OAuthActivity extends Activity {	
	
	public static final String TAG = OAuthActivity.class.getSimpleName(); 
    
    protected Button mLoginButton;
    protected OAuthConsumer mConsumer; 
    protected OAuthProvider mProvider;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oauth_activity);
        
        mLoginButton = (Button)findViewById(R.id.button_login);
        
        mLoginButton.setOnClickListener(new View.OnClickListener() {			
			@Override
			public void onClick(View v) {
				mLoginButton.setEnabled(false);
				doOAuthRequestToken(); 				
			}
		});
        
        if(!TextUtils.isEmpty(initOAuthConsumerAndProvider())){
        	mLoginButton.setEnabled(false);
        	// TODO: notify user of error 
        }                
                                
    }
    
    protected void processRequestResponse(Uri uri){
    	final String oAuthVerifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
    	if(!TextUtils.isEmpty(oAuthVerifier)){
    		new RetrieveAccessTokenTask(this,this.mConsumer,this.mProvider, oAuthVerifier).execute();
    	} else{
    		// TODO: Handle exceptional case 
    	}
    	
    }
    
    protected String initOAuthConsumerAndProvider(){
    	if( mConsumer != null && mProvider != null ){
    		return null;
    	}
    	
    	try{
    		this.mConsumer = new CommonsHttpOAuthConsumer(TradeMeApi.CONSUMER_KEY, TradeMeApi.CONSUMER_SECRET);
    		this.mProvider = new CommonsHttpOAuthProvider(
	        		TradeMeApi.getRequestTokenEndpoint(),
	        		TradeMeApi.getAccessTokenEndpoint(),
	        		TradeMeApi.getAuthorizationUrl());    		    		
    	} catch(UnsupportedEncodingException e){
    		Log.e(TAG, e.toString());
    		return e.toString();
    	} catch(Exception e){
    		Log.e(TAG, e.toString());
    		return e.toString(); 
    	}
    	
    	return null; 
    }
    
    protected void doOAuthRequestToken(){
    	new OAuthRequestTokenTask(this,this.mConsumer,this.mProvider).execute();
    }
    
    @Override
	protected void onResume(){
		super.onResume();
	}
	
	@Override
	protected void onPause(){
		super.onPause(); 
	}
	
	@Override
	protected void onDestroy (){
		Log.i(TAG, "onDestroy");
		super.onDestroy();		
	}
    
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent); 						
		
		if( intent != null ){
        	String scheme = intent.getScheme();
        	final Uri uri = intent.getData();
        	
            if(scheme != null && uri != null && scheme.equalsIgnoreCase("http")){            	
            	mLoginButton.setEnabled(false);
            	processRequestResponse(uri);
            	finish();
            }
        } 
	}

}
