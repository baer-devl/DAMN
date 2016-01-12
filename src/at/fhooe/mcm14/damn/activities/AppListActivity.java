package at.fhooe.mcm14.damn.activities;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import at.fhooe.mcm14.damn.R;
import at.fhooe.mcm14.damn.fragments.AppListFragment;
import at.fhooe.mcm14.damn.jni.DAMNServer;
import at.fhooe.mcm14.damn.misc.Extract;
import at.fhooe.mcm14.damn.services.BootCompleteNotifier;
import at.fhooe.mcm14.damn.services.XModuleService;

/**
 * List all activities which are installed on the phone.
 */
public class AppListActivity extends FragmentActivity {
    private static final String TAG = ListActivity.class.getName();

    
    private ViewPageAdapter pageAdapter;
	private ViewPager pager;
	
	public Context getContext() {
		return getApplicationContext();
	}
	
	@Override
	protected void onCreate(Bundle _bundle) {
		super.onCreate(_bundle);
		
		//create necessary files
        File dir = new File("/data/data/at.fhooe.mcm.faaaat/files/root");
        if(!dir.exists()){
        	dir.mkdirs();
        	dir.setReadable(true, false);
        	dir.setWritable(true, false);
        	dir.setExecutable(true, false);
        }
        
        Extract.copyAssetFolder(getApplicationContext(), "root", "/data/data/at.fhooe.mcm.faaaat/files/root");
		
		Log.d(TAG, DAMNServer.getState().name());
		this.sendBroadcast(new Intent("DAMN").putExtra("key", "value"));
		
		Log.i(TAG, "onCreate AppListActivity");
		setContentView(R.layout.fragment_app_list);
		
		//check if we have to start the service -> same as after boot complete
		try {
			PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(getApplicationContext(), BootCompleteNotifier.class), 0).send();
		} catch (CanceledException e) {
			e.printStackTrace();
		}
		
		pageAdapter = new ViewPageAdapter(getSupportFragmentManager());
		pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(pageAdapter);
		
		//check if it comes from ControlService
		Bundle e = getIntent().getExtras();
		if(e!=null && e.getBoolean(XModuleService.FROM_SERVICE))
			pager.setCurrentItem(pageAdapter.getCount());
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings)
            return true;
            
        return super.onOptionsItemSelected(item);
    }
    
    
    private class ViewPageAdapter extends FragmentStatePagerAdapter {
    	
    	private static final int PAGES = 5;
    	private List<AppListFragment> fragments = null;
    	
    	public ViewPageAdapter(FragmentManager fm) {
			super(fm);
			fragments = new ArrayList<AppListFragment>();
		}
    	
    	@Override
    	public CharSequence getPageTitle(int position) {
    		switch(position) {
    		case 0:
    			return getString(R.string.fragment_title_download);
    		case 1:
    			return getString(R.string.fragment_title_system);
    		case 2:
    			return getString(R.string.fragment_title_running);
    		case 3:
    			return getString(R.string.fragment_title_all);
    		case 4:
    			return getString(R.string.fragment_title_active);
    			
			default:
				return null;
    		}
    	}

		@Override
		public Fragment getItem(int pos) {
			AppListFragment frag = null;
			
			try{
				frag = fragments.get(pos);
				
			}catch(IndexOutOfBoundsException e){
				
				frag = new AppListFragment();
				Bundle bundle = new Bundle();
				
				switch (pos) {
				case 0:
					bundle.putSerializable(AppListFragment.LIST_TYPE, AppListFragment.AppListType.DOWNLOADED); break;
				case 1:
					bundle.putSerializable(AppListFragment.LIST_TYPE, AppListFragment.AppListType.SYSTEM); break;
				case 2:
					bundle.putSerializable(AppListFragment.LIST_TYPE, AppListFragment.AppListType.RUNNING); break;
				case 3:
					bundle.putSerializable(AppListFragment.LIST_TYPE, AppListFragment.AppListType.ALL); break;
				case 4:
					bundle.putSerializable(AppListFragment.LIST_TYPE, AppListFragment.AppListType.ACTIVE); break;
				}
				
				//set arguments
				frag.setArguments(bundle);
				fragments.add(frag);
				Log.i(TAG, "add "+ bundle.get(AppListFragment.LIST_TYPE).toString());
			}
			
			return frag;
		}

		@Override
		public int getCount() {
			return PAGES;
		}
    }
}
