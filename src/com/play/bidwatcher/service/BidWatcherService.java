package com.play.bidwatcher.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.drive.internal.GetDriveIdFromUniqueIdentifierRequest;
import com.play.bidwatcher.R;
import com.play.bidwatcher.dal.OAuthManager;
import com.play.bidwatcher.model.WatchListItem;
import com.play.bidwatcher.task.BidTask;
import com.play.bidwatcher.task.RetrieveWatchListTask;

@SuppressLint("DefaultLocale")
public class BidWatcherService extends Service {

	public static final String TAG = BidWatcherService.class.getSimpleName(); 
	
	/* Key for the string that's delivered in the action's intent */ 
	public static final String EXTRA_BID_AMOUNT = "extra_bid_amount";
	/* key referencing to the WatchListItem assigned to a bid Intent */ 
	public static final String EXTRA_WATCHLIST_ITEM = "extra_watchlist_item";
	/* key referencing the notification id */ 
	public static final String EXTRA_NOTIFICATION_ID = "extra_notification_id";
	/* group for our notifications - grouping notifications will create a stack on the Android Ware device (to avoid multiple notifications of the same type 
	 * queueing in the context stream */ 
	public static final String NOTIFICATION_GROUP_KEY = "group_key_trademe";
	/* number of concurrent threads we want to make available */ 
	protected static final int MAX_CONCURRENT_THREADS = 1;	
	/* action associated to the alarm */ 
	protected static final String ALARM_ACTION = "com.play.bidwatcher.service.BidWatcherService.AlarmBroadcastReceiver.ALARM"; 
	/* action for default increments when initiated by the user */
	protected static final String DEFAULT_BID_ACTION = "com.play.bidwatcher.service.BidWatcherService.DEFAULT_BID";
	/* action for custom bids based from user input */ 
	protected static final String CUSTOM_BID_ACTION = "com.play.bidwatcher.service.BidWatcherService.CUSTOM_BID";			
	/* receiver for handling the alarm notification */ 
	protected final AlarmBroadcastReceiver mAlarmBroadcastReceiver = new AlarmBroadcastReceiver();
	/* intent filter for alarm the ALARM ACTION */ 
	protected IntentFilter mAlarmIntentFilter = null;
	/* pending intent given to the alarm notification */ 
	protected PendingIntent mAlarmPendingIntent = null;
	/* broadcast receiver responsible for intercepting the bid notifications */ 
	protected final BidBroadcastReceiver mBidBroadcastReceiver = new BidBroadcastReceiver();
	/* system alarm manager used to schedule alarm notifications */ 
	protected AlarmManager mAlarmManager = null;
	/* system notification manager for pushing and dismissing notifications, nb: using NotificationManagerCompat as opposed to NotificationManager for Android Ware extended support */
	protected NotificationManagerCompat mNotificationManager;
	/* system power manager used to set and release wake locks */ 
	protected PowerManager mPowerManager = null;
	/* default alarm delay */ 
	protected long mAlarmDelay = TimeUnit.MINUTES.toMillis(30);
	/* thread manager */ 
	protected Executor mExecutor;
	/* application context */ 
	protected Context mContext; 
	
	@Override
	public void onCreate() {
		
		mContext = this.getApplicationContext(); 
		
		// get system managers (power, alarm, and notification) 
		mPowerManager = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        mAlarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE); 
        mNotificationManager = NotificationManagerCompat.from(mContext);
        
        // register broadcast reciever for bidding intents 
        IntentFilter bidIntentFilter = new IntentFilter();
        bidIntentFilter.addAction(DEFAULT_BID_ACTION);
        bidIntentFilter.addAction(CUSTOM_BID_ACTION);
        this.registerReceiver(mBidBroadcastReceiver, bidIntentFilter);
        
        mExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_THREADS,
        		new ThreadFactory(){
        	@Override
            public Thread newThread(Runnable r) {
        		Thread t = new Thread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                t.setName("watcher");
                return t;
        	}
        });
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(TAG, "onStart" + intent.getAction());	    
	}
			
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "onStartCommand");							
		
		setAlarm(TimeUnit.SECONDS.toMillis(5));
		
		return START_STICKY;		
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy"); 							
		mAlarmManager.cancel(mAlarmPendingIntent);
    	
    	if( mAlarmIntentFilter != null ){
    		this.unregisterReceiver(mAlarmBroadcastReceiver);
    		mAlarmIntentFilter = null; 
    	}   
    	
    	this.unregisterReceiver(mBidBroadcastReceiver);
	}						
	
	protected void setAlarm(long delay){
		
		if( mAlarmPendingIntent != null ){
			mAlarmManager.cancel(mAlarmPendingIntent);    		
    	}
		
    	if( mAlarmIntentFilter == null ){
    		mAlarmIntentFilter = new IntentFilter();   
    		mAlarmIntentFilter.addAction(ALARM_ACTION);
    		this.registerReceiver(mAlarmBroadcastReceiver, mAlarmIntentFilter);
    		
    		Log.i(TAG, "registerReceiver " + mAlarmIntentFilter.getAction(0) );
    	}
    	
    	if( mAlarmPendingIntent == null ){
    		Intent intent = new Intent(ALARM_ACTION);
        	mAlarmPendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);    		
    	}
    	
    	long startTime = Calendar.getInstance().getTimeInMillis() + delay;
    	mAlarmManager.set(AlarmManager.RTC_WAKEUP, (startTime), mAlarmPendingIntent);
    }
	
	protected void onHandleAlarmBroadcast(Intent intent){
		Log.i(TAG, "onAlarmBroadcastReceived");				 			
				
		aquireWakeLock();
		
		if(OAuthManager.hasValidOAuthToken(this)){
			new RetrieveWatchListTask(OAuthManager.getOAuthConsumer(this), new RetrieveWatchListTask.RetrieveWatchListTaskListener() {
				
				@Override
				public void onRetrieveWatchListTaskFailed(int errorCode, String errorMessage) {
					// TODO handle exception
					updateAlarmDelay(); 		
					setAlarm(mAlarmDelay);
					releaseWakeLock(); 
				}
				
				@Override
				public void onRetrieveWatchListTaskComplete(final List<WatchListItem> watchListItems) {
					Log.i(TAG, "onRetrieveWatchListTaskComplete " + watchListItems.size());										
					
					mExecutor.execute(new Runnable() {
						
						@Override
						public void run() {
							processWatchListItems(watchListItems);
							
							updateAlarmDelay(); 		
							setAlarm(mAlarmDelay);
							releaseWakeLock();							
						}
					});										
				}
			}).execute();
		} else{
			updateAlarmDelay(); 		
			setAlarm(mAlarmDelay);
			releaseWakeLock(); 
		}				
	}	
	
	protected void onHandleBidBroadcast(Intent intent){
		Log.i(TAG, "onBidBroadcastReceived");					
		
		if(intent != null){
			WatchListItem item = intent.getParcelableExtra(EXTRA_WATCHLIST_ITEM);
			
			if(item != null ){
				float bidAmount = 0.0f;
				
				if(intent.getAction().equals(DEFAULT_BID_ACTION)){
					// increase by 1.00
					bidAmount += item.mMaxBidAmount += 1.0f; 
				} else if(intent.getAction().equals(CUSTOM_BID_ACTION)){
					// handle voice input
					String input = "";
					Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
			        if (remoteInput != null) {
			        	CharSequence inputAsCharSequence = remoteInput.getCharSequence(EXTRA_BID_AMOUNT);
			        	if( inputAsCharSequence != null ){
			        		input = inputAsCharSequence.toString();
			        	}
			        }
			        
			        // TODO: implement more sophisticated parsing of users voice input   
			        
			        try{
			        	bidAmount = Float.parseFloat(input);
			        } catch(NumberFormatException e){
			        	Log.e(TAG, "onBidBroadcastReceived; " + e.toString());
			        }
				}
				
				if(bidAmount > item.mMaxBidAmount){
					aquireWakeLock();
					
					new BidTask(OAuthManager.getOAuthConsumer(this), item.mListingId, bidAmount, new BidTask.BidTaskListener() {
						@Override
						public void onBidTaskFailed(int errorCode, String errorMessage) {
							// TODO: handle response 
							releaseWakeLock();
						}
						
						@Override
						public void onBidTaskComplete(WatchListItem watchListItem) {
							// TODO: handle response 
							releaseWakeLock();
						}
					}).execute();
				} else{
					Log.w(TAG, "onBidBroadcastReceived; bid amount lower that current bid, your bid " + bidAmount + " and current bid " + item.mMaxBidAmount);
					// TODO: notify user
				}
			}			
		}
	}

	protected void updateAlarmDelay(){
		// TODO: update mAlarmDelay based on auction expiry date/time
	}
	
	/**
	 * 
	 * @param watchListItems
	 */
	protected void processWatchListItems(List<WatchListItem> watchListItems){
		if( watchListItems == null || watchListItems.size() == 0 ){
			return; 
		}
		
		for(WatchListItem item : watchListItems){
			if(!isWatchListItemNotifiable(item)){
				continue; 
			}
			
			int notificationId = item.mListingId; 
			
//			// fetch image TODO: add image caching 		
//			Bitmap listingBitmap = null;
//			if(!TextUtils.isEmpty(item.mPictureHref)){
//				listingBitmap = loadImageFromUrl(item.mPictureHref);
//			}						
			
			Intent defaultBidIntent = new Intent(DEFAULT_BID_ACTION).putExtra(EXTRA_WATCHLIST_ITEM, item).putExtra(EXTRA_NOTIFICATION_ID, notificationId);
			PendingIntent pendingDefaultBidIntent = PendingIntent.getBroadcast(mContext, 0, defaultBidIntent, PendingIntent.FLAG_UPDATE_CURRENT);						
			
			Intent customBidIntent = new Intent(CUSTOM_BID_ACTION).putExtra(EXTRA_WATCHLIST_ITEM, item).putExtra(EXTRA_NOTIFICATION_ID, notificationId);
			PendingIntent pendingCustomBidIntent = PendingIntent.getBroadcast(mContext, 0, customBidIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			
			// create big style for the 2nd page 
			BigTextStyle autionDetailsPageStyle = new NotificationCompat.BigTextStyle()
				.setBigContentTitle(mContext.getString(R.string.title_auction_details))
				.bigText(String.format(this.getString(R.string.copy_notification_details), item.mMaxBidAmount, item.getTimeRemainingAsString(), item.mBidCount));
						
			// create 2nd page notification
			Notification detailsPageNotification = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.small_icon)
				.setStyle(autionDetailsPageStyle)
				.build();
			
			NotificationCompat.Action defaultBidAction = new NotificationCompat.Action
					.Builder(R.drawable.icon_action_bid, mContext.getString(R.string.label_auto_bid), pendingDefaultBidIntent)
					.build();
			
			RemoteInput remoteInput = new RemoteInput.Builder(EXTRA_BID_AMOUNT).setLabel(mContext.getString(R.string.copy_specify_bid)).build();
			
			NotificationCompat.Action customBidAction = new NotificationCompat.Action
				.Builder(R.drawable.icon_action_bid, mContext.getString(R.string.label_bid), pendingCustomBidIntent)
				.addRemoteInput(remoteInput)
				.build();						
			
			// Create a WearableExtender to add functionality for wearables
			NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender();			
			wearableExtender.setBackground(BitmapFactory.decodeResource(getResources(), R.drawable.notification_background));			
			wearableExtender.addPage(detailsPageNotification);
			wearableExtender.addAction(defaultBidAction).addAction(customBidAction);
			
			NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.small_icon)
			    .setContentTitle(mContext.getString(R.string.title_auction_update))
			    .setContentText(item.mTitle)
			    .setGroup(NOTIFICATION_GROUP_KEY)
			    .extend(wearableExtender); 						
			
			// Build the notification and issues it with notification manager.
			mNotificationManager.notify(notificationId, notificationBuilder.build());
		}
	}
	
	protected Bitmap loadImageFromUrl(String href){
		Log.i(TAG, "loadImageFromUrl: " + href);
		
		try {
			URL url = new URL(href);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream input = connection.getInputStream();
			Bitmap myBitmap = BitmapFactory.decodeStream(input);

			return myBitmap;
		} catch (IOException e) {
			Log.e(TAG, "loadImageFromUrl; " + e.toString());
			return null;
		}
	}
	
	protected boolean isWatchListItemNotifiable(WatchListItem watchListItem){
		// TODO: add ignored notify count and threshold i.e. if we've already notified the user twice without any interaction then ignore until the bid is almost closed
		// TODO: add 'do not disturb threshold' e.g. if the time is between 22:00-07:00 and the user is in idle (not active - presumed asleep) and not explicitly specified 
		// 	by the user, then ignore
		// TODO: add 'budget' i.e. amount threshold, once reached - ignore 
		
		if( !watchListItem.mIsLeading && watchListItem.mIsOutbid ){
			return true; 
		}
		
		//return false;
		return true; 
	}
	
	protected void aquireWakeLock(){
		PowerManager.WakeLock wl = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);    	    	
    	boolean wakeLockIsHeld = wl.isHeld(); 
    	
    	if( !wakeLockIsHeld ){
    		wl.acquire(); 
    	}
	}
	
	protected void releaseWakeLock(){
		PowerManager.WakeLock wl = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);    	    	
    	boolean wakeLockIsHeld = wl.isHeld(); 
    	
    	if( wakeLockIsHeld ){
    		wl.release();  
    	}
	}
	
	private class AlarmBroadcastReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, final Intent intent) {	
			mExecutor.execute( new Runnable() {	
				@Override
				public void run() {
					onHandleAlarmBroadcast(intent); 
				}
			});
		}		
	}	
	
	private class BidBroadcastReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, final Intent intent) {	
			mExecutor.execute( new Runnable() {	
				@Override
				public void run() {
					// cancel the notification 
					int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1);
					mNotificationManager.cancel(notificationId);
					 
					onHandleBidBroadcast(intent);
				}
			});
		}		
	}
}
