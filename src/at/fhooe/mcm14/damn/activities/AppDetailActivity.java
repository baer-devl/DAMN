package at.fhooe.mcm14.damn.activities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import at.fhooe.mcm14.damn.R;
import at.fhooe.mcm14.damn.fragments.AppListFragment;
import at.fhooe.mcm14.damn.services.XModuleService;
import at.fhooe.mcm14.damn.xposed.XModule;
import dalvik.system.DexFile;

public class AppDetailActivity extends Activity {

    /**
     * Used for the extra in the bundle.
     */
    public static final String APPINFO = "appinfo";

    private static final String TAG = AppDetailActivity.class.getName();
    private ApplicationInfo applicationInfo = null;
    private Button activate = null;
    private HashSet<String> actives = null;
    private HashSet<String> packages = null;
    private HashSet<String> allPackages = null;
    private PackageArrayAdapter adapter = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //get applicationInfo from bundle
        applicationInfo = (ApplicationInfo)getIntent().getExtras().get(APPINFO);

        //close this activity if appinfo is empty
        if(applicationInfo==null){
            AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(this);
            dialogbuilder
                .setMessage("Sorry, no detailed information available")
                .setCancelable(false)
                .setTitle("No Information")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        AppDetailActivity.this.finish();
                    }
                });

            AlertDialog alert = dialogbuilder.create();
            alert.show();
        }
        
        //get shared preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        actives = (HashSet<String>) prefs.getStringSet(AppListFragment.ACTIVE_APPS, new HashSet<String>());
        packages = (HashSet<String>) prefs.getStringSet(applicationInfo.packageName, new HashSet<String>());
        Log.d(TAG, "loaded packages:" + packages.size());

        //set view
        setContentView(R.layout.activity_app_detail);
        
        //get packages
        DexFile df;
		try {
			df = new DexFile(applicationInfo.sourceDir);
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		
		if(df!=null){
			ArrayList<String> pkgs = new ArrayList<String>();
			for (Enumeration<String> iter = df.entries(); iter.hasMoreElements();) {
				String pkg = iter.nextElement();
				pkg.replaceAll("\\.*\\.java", "");
				
				String[] parts = pkg.split("\\.");
				
				if(parts.length>3){
					int i = pkg.indexOf(parts[3]);
					pkg = pkg.substring(0, i);
					if(pkg.endsWith("."))
						pkg = pkg.substring(0, pkg.length()-1);
				}
				
				if(!pkgs.contains(pkg))
					pkgs.add(pkg);
			}
			
			allPackages = new HashSet<String>(pkgs);
			adapter = new PackageArrayAdapter(getApplicationContext(), R.layout.listitem_packages, pkgs);
			((ListView)findViewById(R.id.lv_details_permissons)).setAdapter(adapter);
		}
		

        //set detail information
        ((TextView)findViewById(R.id.tv_detail_name)).setText(applicationInfo.loadLabel(getPackageManager()));
        ((TextView)findViewById(R.id.tv_detail_package)).setText(applicationInfo.packageName);
        ((ImageView)findViewById(R.id.iv_detail_icon)).setImageDrawable(getPackageManager().getApplicationIcon(applicationInfo));
        
        //set button
        activate = (Button)findViewById(R.id.b_detail_activate);
        
	    if(actives.contains(applicationInfo.packageName))
	    	activate.setText(getString(R.string.fragment_title_deactivate));
	    else
	    	activate.setText(getString(R.string.fragment_title_active));
        	
       	activate.setOnClickListener(new OnClickListener() {
			
       		
			@SuppressWarnings("unchecked")
			@Override
			public void onClick(View _view) {
				//activate tracing, change button text
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				Editor editor = prefs.edit();
				
				if(actives==null)
					actives = new HashSet<String>();
				
				if(actives.contains(applicationInfo.packageName) && !activate.getText().toString().equalsIgnoreCase("save changes")){
					Log.i(TAG, "remove package");
		        	activate.setText(getString(R.string.fragment_title_active));
		        	actives.remove(applicationInfo.packageName);
		        	Set<String> tmp_actives = (Set<String>) actives.clone();
		        	
		        	editor.remove(AppListFragment.ACTIVE_APPS);
		        	editor.commit();
    				editor.putStringSet(AppListFragment.ACTIVE_APPS, tmp_actives);
        			if(actives.isEmpty()){
        				//deactivate service if nothing left to track
        				Log.i(TAG, "stop service because of empty list of tracked apps");
        				stopService(new Intent(getApplicationContext(), XModuleService.class));
        			}
        			
        			for(int n=0;n<adapter.packageItems.length;n++)
						adapter.packageItems[n] = false;
        			adapter.notifyDataSetChanged();
		        	
				}else{
					Log.i(TAG, "add package");
		        	actives.add(applicationInfo.packageName);
		        	Set<String> tmp_actives = (Set<String>) actives.clone();
		        	editor.remove(AppListFragment.ACTIVE_APPS);
		        	editor.commit();
					editor.putStringSet(AppListFragment.ACTIVE_APPS, tmp_actives);
					if(actives.size()==1){
        				//activate service
        				Log.i(TAG, "start service because of new entry of app");
        				startService(new Intent(getApplicationContext(), XModuleService.class));
					}
					
					if(activate.getText().toString().equalsIgnoreCase("save changes")){
						//save packages
						Set<String> tmp_packages = (Set<String>) packages.clone();
						editor.remove(applicationInfo.packageName);
						editor.commit();
						editor.putStringSet(applicationInfo.packageName, tmp_packages);
						Log.i(TAG, "save packages:" + packages.size());
						
					}else{
						//otherwise the user want all packages!
						packages = allPackages;
						for(int n=0;n<adapter.packageItems.length;n++)
							adapter.packageItems[n] = true;
	        			adapter.notifyDataSetChanged();
					}
					
					activate.setText(getString(R.string.fragment_title_deactivate));
				}
				
				//save changes
				editor.commit();
				
				//check if file exists
				File tmp = new File(getApplicationContext().getCacheDir().getAbsoluteFile(), XModule.PACKAGE_FILE);
				File target = new File(applicationInfo.dataDir, XModule.PACKAGE_FILE);
				ArrayList<String> tmpList = new ArrayList<String>((Set<String>)packages.clone());
				tmpList.add(0, applicationInfo.packageName);

				try {
					if(!tmp.exists()) {
						tmp.createNewFile();
						tmp.setReadable(true, false);
						tmp.setWritable(true, false);
					}
					
					//create tmp file
					OutputStream os = new FileOutputStream(tmp);
					OutputStream buffer = new BufferedOutputStream(os);
				    ObjectOutput output = new ObjectOutputStream(buffer);
				    output.writeObject(tmpList);
				    output.flush();
				    output.close();
				    
				    
				    
				    //copy tmp file to target file and change permissions
				    Log.d(TAG, "cp:    " + Runtime.getRuntime().exec(new String[]{"su", String.valueOf(applicationInfo.uid), "cp", tmp.getAbsolutePath(), target.getAbsolutePath()}).waitFor());
				    Log.d(TAG, "chmod: " + Runtime.getRuntime().exec(new String[]{"su", String.valueOf(applicationInfo.uid), "chmod", "0666", target.getAbsolutePath()}).waitFor());
				    
					tmp.delete();
				    
			    }catch(Exception ex){
			    	ex.printStackTrace();
			    }
				
				
				Log.i(TAG, "saved changes to shared preferences");
			}
		});
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.app_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings)
            return true;
            
        return super.onOptionsItemSelected(item);
    }
    
    
    private class PackageArrayAdapter extends ArrayAdapter<String> {
    	
    	public boolean[] packageItems = null;

    	public PackageArrayAdapter(Context context, int resource, List<String> objects) {
			super(context, resource, objects);
			packageItems = new boolean[objects.size()];
			
			//get active packages
	    	String[] pkgs = objects.toArray(new String[objects.size()]);
	    	Log.i(TAG, "length: " + pkgs.length);
	    	
	    	if(pkgs!=null && packages!=null)
	    		for(int n=0;n<pkgs.length;n++)
	    			if(packages.contains(pkgs[n]))
	    				packageItems[n] = true;
		}
    	
    	
		
		@Override
		public void add(String object) {
			super.add(object);
			Log.i(TAG, "add item");
		}
		
		
		@Override
		public void addAll(Collection<? extends String> collection) {
			super.addAll(collection);
			Log.i(TAG, "add collection");
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			CheckBox view = (CheckBox) super.getView(position, convertView, parent);
			view.setChecked(isChecked(position));
			
			view.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					ListView list = (ListView) buttonView.getParent();
					if(list!=null)
						packageItems[list.getPositionForView(buttonView)] = isChecked;
					
					String pkg = buttonView.getText().toString();
					
					if(!isChecked && packages.contains(pkg))
						packages.remove(pkg);
					else if(isChecked && !packages.contains(pkg))
						packages.add(pkg);
					
					activate.setText("save changes");
				}
			});
			
			return view;
		}
		
		private boolean isChecked(int pos) {
			return packageItems[pos];
		}
    	
    }
}
