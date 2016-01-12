package at.fhooe.mcm14.damn.xposed;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import android.util.Log;
import at.fhooe.mcm14.damn.jni.DAMNServer;
import dalvik.system.DexFile;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.XposedHelpers.ClassNotFoundError;

public class XHookAll {
	private static final String TAG = XHookAll.class.getCanonicalName();
	
	private String app;
	private String source;
	private List<String> pkgs = null;
	private ClassLoader clazzloader;
	private List<Callable<Boolean>> tasks = new ArrayList<Callable<Boolean>>();
	private ExecutorService pool = null;
	
	
	public XHookAll(String _app, String _source, ClassLoader _clazzloader, List<String> _pkgs) {
		app = _app;
		source = _source;
		clazzloader = _clazzloader;
		pkgs = _pkgs;
		
		hook();
	}
	
	
	private void hook() {
		// get all classes and hook every constructor and method
		DexFile df;
		try {
			df = new DexFile(source);
			
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		Log.i(TAG, "register " + df.getName());
		XDAMNHook hook = new XDAMNHook(app);
		
		DAMNServer.newAppStarted(app);
		XListener xl = new XListener(app, null, hook);
		xl.start();
		
		hook.threads.put(1L, xl);
		Log.e(TAG, "opened new site");
		
		//if there are no packages specified - return
		if(pkgs==null)
			return;
		

		// literate through every class of application
		for (Enumeration<String> iter = df.entries(); iter.hasMoreElements();) {
			String clazzName = iter.nextElement();
			
			//check if package is in
			boolean pack = false;
			for(String pkg : pkgs)
				if(clazzName.startsWith(pkg)){
					pack = true;
					break;
				}
			
			if(!pack)
				continue;
			
			// get class
			Class<?> clazz = null;
			try {
				clazz = XposedHelpers.findClass(clazzName, clazzloader);
//				Log.i(TAG, "in class: " + clazzName);
				
				//hook all constructors
				tasks.add(new XCallableHookConstructors(hook, clazz));
//				Log.i(TAG, "constructors hooked");
				
				//get all methods
	            Method[] methods = clazz.getMethods();
	            
	            //hook methods
	            if(methods != null)
		            for(Method method : methods){
		            	tasks.add(new XCallableHookMethods(hook, clazz, method.getName()));
		            	
//		            	Log.i(TAG, "method " + method.getName() + " hooked");
		            }
	            
//	            Log.d(TAG, "before done: " + clazzName);

	            
			} catch (NoClassDefFoundError ncd) {
//				Log.e(TAG, "def not found " + clazzName);
				
			} catch (ClassNotFoundError cn) {
//				Log.e(TAG, "can't find " + clazzName);
				
			} catch (Exception e) {
//				Log.e(TAG, "error: " + e.getMessage());
			}
			
//			Log.d(TAG, "done: " + clazzName);
		}
		
		Log.d(TAG, "size of tasks:" + tasks.size());
		
//		pool = Executors.newSingleThreadExecutor();
		int size = tasks.size()/10;
		int max = 300;
		
		if(size>max)
			size=max;
		else if(size<1)
			size=1;
		
		pool = Executors.newFixedThreadPool(size);
		
		try {
			List<Future<Boolean>> ret = pool.invokeAll(tasks, 5, TimeUnit.SECONDS);
			
			Log.i(TAG, "served methods: " + ret.size());
				
			
		} catch (InterruptedException e1) {
			e1.printStackTrace();
			Log.e(TAG, e1.getMessage());
		}
		
		//go on
		Log.d(TAG, "notifyAll...");
		synchronized (hook.threads) {
			hook.init_done = true;
			Log.d(TAG, "notifyAll");
			Collection<XListener> values = hook.threads.values();
			for(XListener l : values)
				try{l.notify();}catch(IllegalMonitorStateException e){ }
		}
		
		pool.shutdown();
		Log.d(TAG, "wait till all down");
//		try {
//			pool.awaitTermination(60, TimeUnit.SECONDS);
//			
//		} catch (InterruptedException e) {
//		  Log.e(TAG, e.getMessage());
//		}
		
		Log.e(TAG, "DONE WITH HOOKING");
	}
}
