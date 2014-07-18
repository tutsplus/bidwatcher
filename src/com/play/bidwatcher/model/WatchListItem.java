package com.play.bidwatcher.model;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.os.Parcel;
import android.os.Parcelable;


public class WatchListItem implements Parcelable{
	
	public static final String TAG = WatchListItem.class.getSimpleName();  

	public int mListingId;
	public String mTitle;
	public float mStartPrice;
	public Date mStartDate; 
	public Date mEndDate;
	public float mMaxBidAmount; 
	public String mPictureHref;
	public int mBidCount = 0; 
	public boolean mIsReserveMet = false; 
	public boolean mHasReserve = false; 
	public boolean mIsLeading = false; 
	public boolean mIsOutbid = false; 
	
	public WatchListItem(){
		
	}
	
	public WatchListItem(Parcel p){
		readFromParcel(p);
	}
	
	public String getTimeRemainingAsString(){
		String bidFinishString = "";
		
		Date now = new Date();
		
		long timeDiff = this.mEndDate.getTime() - now.getTime();
		
		if( timeDiff > TimeUnit.DAYS.toMillis(1) ){
			int diffInDays = (int) (timeDiff / (1000 * 60 * 60 * 24));
			bidFinishString = String.format(Locale.getDefault(), "%d days", diffInDays);
		} else if( timeDiff > TimeUnit.HOURS.toMillis(1)){
			int diffHours = (int)(timeDiff / (60 * 60 * 1000));
			bidFinishString = String.format(Locale.getDefault(), "%d hours", diffHours);
		} else if( timeDiff > TimeUnit.MINUTES.toMillis(1)){
			int diffMinutes = (int)(timeDiff / (60 * 1000) % 60);
			bidFinishString = String.format(Locale.getDefault(), "%d mins", diffMinutes);
		} else{
			int diffSeconds = (int)(timeDiff / 1000 % 60);
			bidFinishString = String.format(Locale.getDefault(), "%d secs", diffSeconds);
		}
		return bidFinishString; 
	}
	
	@Override
	public String toString(){
		return String.format(Locale.getDefault(), "Title: %s @ £%.2f (leading %s)", mTitle, mMaxBidAmount, mBidCount, (mIsLeading ? "yes" : "no"));
	}

	public void readFromParcel(Parcel parcel) {
		mListingId = parcel.readInt(); 
		mTitle = parcel.readString();
		mStartPrice = parcel.readFloat(); 
		mStartDate = new Date(parcel.readLong());
		mEndDate = new Date(parcel.readLong());
		mMaxBidAmount = parcel.readFloat(); 
		mPictureHref = parcel.readString(); 
		mBidCount = parcel.readInt(); 
		mIsReserveMet = parcel.readByte() == 1 ? true : false; 
		mHasReserve = parcel.readByte() == 1 ? true : false;
		mIsLeading = parcel.readByte() == 1 ? true : false;
		mIsOutbid = parcel.readByte() == 1 ? true : false;		
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mListingId);
		dest.writeString(mTitle);
		dest.writeFloat(mStartPrice);
		dest.writeLong(mStartDate.getTime());
		dest.writeLong(mEndDate.getTime());
		dest.writeFloat(mMaxBidAmount);
		dest.writeString(mPictureHref);
		dest.writeInt(mBidCount);
		dest.writeByte(mIsReserveMet ? (byte)1 : (byte)0);
		dest.writeByte(mHasReserve ? (byte)1 : (byte)0);
		dest.writeByte(mIsLeading ? (byte)1 : (byte)0);
		dest.writeByte(mIsOutbid ? (byte)1 : (byte)0);
	}

	@Override
	public int describeContents() {
		return TAG.hashCode();
	}
	
	public static final Parcelable.Creator<WatchListItem> CREATOR = new Parcelable.Creator<WatchListItem>() {
        public WatchListItem createFromParcel(Parcel in) {
            return new WatchListItem(in); 
        }

        public WatchListItem[] newArray(int size) {
            return new WatchListItem[size];
        }
    };	
}
