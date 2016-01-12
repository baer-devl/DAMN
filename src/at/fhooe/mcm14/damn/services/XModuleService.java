package at.fhooe.mcm14.damn.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import at.fhooe.mcm14.damn.R;
import at.fhooe.mcm14.damn.activities.AppListActivity;

public class XModuleService extends Service {

	public static final String FROM_SERVICE = "controlservice"; 
	private static final String TAG = XModuleService.class.toString();
	
	
	@Override
	public int onStartCommand(Intent _intent, int _flags, int _startId) {
		Log.i(TAG, "onStartCommand");
		
		// show a notification in the status bar
		Intent intent = new Intent(getApplicationContext(), AppListActivity.class);
		intent.putExtra(FROM_SERVICE, true);
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// build notification
		Notification notifitaction  = new Notification.Builder(getApplicationContext())
		        .setContentTitle("DAMN")
		        .setContentText("Service running")
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentIntent(pIntent)
		        .setAutoCancel(true).build();
		  
		NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		//show notification
		notificationManager.notify(0, notifitaction);

		//cause we want to start or end the service
		return Service.START_STICKY;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "onCreate");
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.i(TAG, "onBind");
		return null;
	}
	
	public Context getXModuleServiceContext() {
		return getApplicationContext();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
		
		//clear notifitaction
		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notificationManager.cancel(0);
	}
}
