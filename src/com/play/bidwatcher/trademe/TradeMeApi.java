package com.play.bidwatcher.trademe;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public final class TradeMeApi {

	public static final String CONSUMER_KEY 	= "YOUR CONSUMER KEY";
	public static final String CONSUMER_SECRET 	= "YOUR CONSUMER SECRET";
	
	public static final String ENCODING 		= "UTF-8";
	    
	public static final String	CALLBACK_SCHEME	= "x-oauthflow";
	public static final String	CALLBACK_HOST = "callback";
//	public static final String	CALLBACK_URL = CALLBACK_SCHEME + "://" + CALLBACK_HOST;
	
    public static String CALLBACK_URL = "http://localhost/oauth";
    
	/**
	 * http://developer.trademe.co.nz/api-overview/sandbox-environment/
	 * trademe.co.nz for live
	 * tmsandbox.co.nz for sandbox environment 
	 */
	protected static String getDomain(){
		return "tmsandbox";
	}
	
	public static String getRequestTokenEndpoint() throws UnsupportedEncodingException {	
		return String.format("https://secure.%s.co.nz/Oauth/RequestToken?scope=%s", 
				getDomain(),
				URLEncoder.encode("MyTradeMeRead,MyTradeMeWrite,BiddingAndBuying", ENCODING));
	}

	public static String getAccessTokenEndpoint() {
		return String.format("https://secure.%s.co.nz/Oauth/AccessToken", getDomain());
	}

//	public static String getAuthorizationUrl(Token requestToken) {
//		return String.format("https://secure.%s.co.nz/Oauth/Authorize?oauth_token=%s", getDomain(), requestToken.getToken());
//	}
	
	public static String getAuthorizationUrl() {
		return String.format("https://secure.%s.co.nz/Oauth/Authorize", getDomain());
	}

	public static String getWatchListUrl(){
		return String.format("https://api.%s.co.nz/v1/MyTradeMe/Watchlist/All.json?photo_size=Gallery", getDomain()); 		
	}
	
	public static String getBidUrl(){
		return String.format("https://api.%s.co.nz/v1/Bidding/Bid.json", getDomain());
	}

}
