package at.fhooe.mcm14.damn.services;

import java.util.HashSet;
import java.util.Set;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import at.fhooe.mcm14.damn.fragments.AppListFragment;

public class BootCompleteNotifier extends BroadcastReceiver {
	
	private static final String TAG = BootCompleteNotifier.class.toString();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(TAG, "boot complete");
		
		//check if we have something where we should start the service
		//get shared preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        Set<String> values = prefs.getStringSet(AppListFragment.ACTIVE_APPS, new HashSet<String>());
        
		if(values.size()>0) {
			//start service
			ComponentName name = context.startService(new Intent(context, XModuleService.class));
			Log.i(TAG, name + " started");
		}
	}
}
