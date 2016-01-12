package at.fhooe.mcm14.damn.fragments;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import at.fhooe.mcm14.damn.R;
import at.fhooe.mcm14.damn.activities.AppDetailActivity;

public class AppListFragment extends ListFragment {
	
	public static enum AppListType {DOWNLOADED, SYSTEM, RUNNING, ALL, ACTIVE}
	public static final String LIST_TYPE = "listtype";
	public static final String ACTIVE_APPS = "active apps";
	
	private static final String TAG = AppListFragment.class.getName();
	private AppListType listType = null;
	private PackageArrayAdapter listAdapter = null;
    private List<ApplicationInfo> packages = null;
    
    
    @SuppressWarnings("incomplete-switch")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //get list type
        listType = (AppListType) getArguments().get(LIST_TYPE);

        //get list of all installed applications
        packages = getAllApps();
        
        List<ApplicationInfo> list = new ArrayList<ApplicationInfo>();
        
        switch(listType){
        case DOWNLOADED:
        	//filter all applications which are not system apps
        	for(int n=0;n<packages.size();n++)
            	if((packages.get(n).flags & ApplicationInfo.FLAG_SYSTEM) != ApplicationInfo.FLAG_SYSTEM)
            		list.add(packages.get(n));
        	
        	packages = list;
        	break;
        case SYSTEM:
        	//filter all applications which are system apps
        	for(int n=0;n<packages.size();n++)
            	if((packages.get(n).flags & ApplicationInfo.FLAG_SYSTEM) == ApplicationInfo.FLAG_SYSTEM)
            		list.add(packages.get(n));
        	
        	packages = list;
        	break;
        case RUNNING:
        	//filter all applications which are running
        	final ActivityManager am = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        	List<RunningAppProcessInfo> running = am.getRunningAppProcesses();
        	
        	for(int n=0;n<running.size();n++)
        		for(int m=0;m<packages.size();m++)
        			if(running.get(n).processName.equalsIgnoreCase(packages.get(m).processName))
        				list.add(packages.get(m));
        	
        	packages = list;
        	break;
        case ACTIVE:
        	packages = getActiveApps();
        	break;
        }
        
        listAdapter = new PackageArrayAdapter(getActivity(), packages);
        setListAdapter(listAdapter);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.i(TAG, "onResume");
    	if(listType == AppListType.ACTIVE)
    		notifyDataSetChangedActiveTrackedApps();
    }
    
    private List<ApplicationInfo> getAllApps() {
    	final PackageManager pm = getActivity().getApplicationContext().getPackageManager();
        return pm.getInstalledApplications(PackageManager.GET_META_DATA);
    }
    
    private List<ApplicationInfo> getActiveApps() {
    	List<ApplicationInfo> list = new ArrayList<ApplicationInfo>();
    	List<ApplicationInfo> all = getAllApps();
    	
    	//filter all applications which are actively tracked
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
    	Set<String> actives = prefs.getStringSet(ACTIVE_APPS, null);
    	
    	if(actives!=null){
    		for(String activity : actives){
    			for(int m=0;m<all.size();m++)
    				if(all.get(m).packageName.equalsIgnoreCase(activity))
    					list.add(all.get(m));
    		}
    	}
    	
    	return list;
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        //open AppDetailActivity and put ApplicationInfo into the Intent
        Intent i = new Intent(l.getContext(), AppDetailActivity.class);
        i.putExtra(AppDetailActivity.APPINFO, packages.get(position));

        startActivity(i);
    }
    
    public AppListType getAppListType() {
    	return listType;
    }
    
    public void notifyDataSetChangedActiveTrackedApps() {
    	packages = getActiveApps();
		listAdapter.clear();
		listAdapter.addAll(packages);
		listAdapter.notifyDataSetChanged();
		Log.i(TAG, "notifyDataSetChangedActiveTrackedApps count: " +listAdapter.getCount());
    }
    
    
    
    /**
     * Handle the listing of the application which are installed on the device.
     */
    private class PackageArrayAdapter extends ArrayAdapter<ApplicationInfo> {
        private Context context = null;

        /**
         * Constructor which also set the context for later use.
         *
         * @param _context Context of application
         * @param _values Array of all installed applications
         */
        public PackageArrayAdapter(Context _context, List<ApplicationInfo> _values) {
            super(_context, R.layout.listitem_package, _values);

            context = _context;
        }

        @SuppressLint("ViewHolder")
		@Override
        public View getView(int _position, View _convertView, ViewGroup _parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.listitem_package, _parent, false);

            //get views
            ImageView icon = (ImageView) rowView.findViewById(R.id.iv_package_icon);
            TextView name = (TextView) rowView.findViewById(R.id.tv_package_name);
            TextView location = (TextView) rowView.findViewById(R.id.tv_package_location);

            PackageManager pm = getActivity().getPackageManager();
            ApplicationInfo app = this.getItem(_position);

            //set values from application
            icon.setImageDrawable(pm.getApplicationIcon(app));
            name.setText(app.loadLabel(pm));
            location.setText(app.sourceDir);

            return rowView;
        }
    }
}
