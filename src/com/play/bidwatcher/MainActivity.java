package com.play.bidwatcher;

import java.util.List;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.play.bidwatcher.dal.OAuthManager;
import com.play.bidwatcher.model.WatchListItem;
import com.play.bidwatcher.service.BidWatcherService;
import com.play.bidwatcher.task.RetrieveWatchListTask;

public class MainActivity extends Activity {

	public static final String TAG = MainActivity.class.getSimpleName();
	
	protected static final long REFRESH_FREQUENCY_THRESHOLD = TimeUnit.MINUTES.toMillis(45);
	
	protected ListView mListView; 
	protected ArrayAdapter<WatchListItem> mListAdapter;	
	protected long mLastRefreshTimestamp = 0L; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		
		mListView = (ListView)findViewById(R.id.list_view);
		
		mListAdapter = new ArrayAdapter<WatchListItem>(this, android.R.layout.simple_list_item_1);
		mListView.setAdapter(mListAdapter);
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		if( OAuthManager.hasValidOAuthToken(this.getApplicationContext())){
			Log.i(TAG, "onStart: valid token found");
			startService(new Intent(this.getApplicationContext(), BidWatcherService.class));			
		} else{
			Log.i(TAG, "onStart: NO valid token found");
			startActivity(new Intent(this, OAuthActivity.class).setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_SINGLE_TOP));
		}
	}		

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onResume(){
		super.onResume();
		
		if( OAuthManager.hasValidOAuthToken(this.getApplicationContext())){
			refreshWatchlist(); 			
		}
	}		
	
	@Override
	protected void onPause(){
		super.onPause(); 
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	protected void refreshWatchlist(){
		if(System.currentTimeMillis()-mLastRefreshTimestamp >= REFRESH_FREQUENCY_THRESHOLD){
			
			new RetrieveWatchListTask(OAuthManager.getOAuthConsumer(this), new RetrieveWatchListTask.RetrieveWatchListTaskListener() {
				
				@Override
				public void onRetrieveWatchListTaskFailed(int errorCode, String errorMessage) {
					Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
					mLastRefreshTimestamp = System.currentTimeMillis();
				}
				
				@Override
				public void onRetrieveWatchListTaskComplete(final List<WatchListItem> watchListItems) {
					Log.i(TAG, "onRetrieveWatchListTaskComplete " + watchListItems.size());										
					
					mListAdapter.clear(); 
					mListAdapter.addAll(watchListItems);
					
					mLastRefreshTimestamp = System.currentTimeMillis();									
				}
			}).execute();						
		}		
	}		
		
}
