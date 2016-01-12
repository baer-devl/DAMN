package at.fhooe.mcm14.damn.xposed;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.List;

import android.util.Log;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XModule implements IXposedHookLoadPackage, IXposedHookZygoteInit {
	
	private static final String TAG = XModule.class.toString();
	
	public static final String PACKAGE_FILE = "damn_packages.pkg";

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		//pushMessage("application " + lpparam.packageName + " loading");
		Log.d(TAG, "application " + lpparam.packageName + " loading");
		
		//update trackable apps
		List<String> packages = null;
		try {
			packages = XModule.getTrackablePackages(lpparam.appInfo.dataDir);
			
		}catch(Exception e) {
			Log.e(TAG, "cant open getTrackablePackages()..");
			return;
		}
		
		// search if actual loaded application match with trackable applications
		if(packages!=null){
			Log.d(TAG, "package: " + lpparam.packageName);
			if (lpparam.packageName.contains(packages.get(0))) {
				packages.remove(0);
				new XHookAll(lpparam.packageName, lpparam.appInfo.sourceDir, lpparam.classLoader, packages);
				Log.d(TAG, "application " + lpparam.packageName + " done");
			}
		}
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		Log.i(TAG, "finalize");
	}

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		Log.i(TAG, "zygote startup");
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	public static List<String> getTrackablePackages(String app) {
		InputStream is = null;
	    InputStream buffer = null;
	    ObjectInput input = null;
	    List<String> packages = null;
	    
	    File file = new File(app, XModule.PACKAGE_FILE);
	    if(!file.exists()){
	    	Log.e(TAG, file.getAbsolutePath() + " dont exist");
	    	return null;
	    }
	      
		try {
			  is = new FileInputStream(file);
		      buffer = new BufferedInputStream(is);
		      input = new ObjectInputStream (buffer);
		      
		      //deserialize the List
		      packages = (List<String>)input.readObject();
		      
		      
		      //log its data
//		      for(String app: trackableApps){
//		        Log.i(TAG, "trackable app: " + app);
//		      }
		      
	    }catch(Exception ex){
	    	Log.e(TAG, "error: " + ex.getMessage());
	    	ex.printStackTrace();
	    }finally{
	    	try {if(input!=null)input.close();if(buffer!=null)buffer.close();if(is!=null)is.close();} catch (Exception e) {}
	    }
		
		return packages;
	}
}
