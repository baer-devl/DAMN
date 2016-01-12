package at.fhooe.mcm14.damn.xposed;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import at.fhooe.mcm14.damn.jni.DAMNServer;
import at.fhooe.mcm14.damn.jni.DAMNServer.Code;
import at.fhooe.mcm14.damn.objects.MessageObject;
import de.robv.android.xposed.XC_MethodHook;

public class XDAMNHook extends XC_MethodHook {

	private static final String TAG = XDAMNHook.class.getCanonicalName();
	
	public volatile boolean init_done = false;
	public volatile HashMap<Long, XListener> threads = new HashMap<>();
	private String app = null;
	
	public XDAMNHook(String app) {
		this.app = app;
	}
	
	@Override
	protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
		//TODO get div object from http class
		JSONObject jMethod = new JSONObject();
		jMethod.put("class", param.method.getDeclaringClass().getCanonicalName());
		jMethod.put("method", param.method.getName());
		jMethod.put("parameters", new JSONArray().put(getPrimitiveObjects(param.args, 0)));
		
		JSONObject jClass = new JSONObject();
		jClass.put("fields", new JSONArray().put(getFieldObjects(param.method.getDeclaringClass().getFields(), param.thisObject, 0)));
		
		long tid = Thread.currentThread().getId();
		
		//check if entry is available
		if(!threads.containsKey(tid)){
			//open new page
//			DAMNServer.newAppStarted(app, tid);
			
			XListener xl = new XListener(app, Thread.currentThread(), this);
			xl.STEPPING = false;
			xl.start();
			
			threads.put(tid, xl);
		}
		
		
		XListener lock = threads.get(tid);
		
		//if init not done- wait for it! (special case)
		synchronized (lock) {
			if(!init_done && lock.STEPPING){
				Log.d(TAG, "wait @" + param.method.getName());
				lock.wait();
				Log.d(TAG, "done waiting @" + param.method.getName());
			}
		}
		
		//send info to browser
		synchronized (lock) {
			DAMNServer.push(new MessageObject(Code.varParam, jMethod.toString(), app, tid));
			if(lock.STEPPING) {
				DAMNServer.push(new MessageObject(Code.varGlobal, jClass.toString(), app, tid));
				
				String source = null;
				try {
					source = readSource("data/data/" + app + "/source/" + param.method.getDeclaringClass().getCanonicalName().replace(".", "/") + ".java");
					
				} catch(Exception e) {
					Log.e(TAG, "something terrible going wrong");
				}
				if(source!=null)
					DAMNServer.push(new MessageObject(
						Code.methodCode, 
						source, 
						app, 
						tid));
			}
		}
		
		//wait for response if stepping is on
		synchronized (lock) {
			if(lock.STEPPING) {
				String data = lock.waitForNextStep();
				if(data!=null){
					Log.d(TAG, "json: " + data);
					JSONObject payload = new JSONObject(data);
					Log.d(TAG, "set new params " + param.method.getName());
					
					//change parameters
					try{
						JSONArray parameters = (JSONArray) payload.get("parameters");
					
						for (int n = 0; n < parameters.length(); n++) {
							JSONObject obj = parameters.getJSONObject(n);
							Iterator<?> keys = obj.keys();
		
							while( keys.hasNext() ) {
							    String key = (String)keys.next();
							    Log.d(TAG, "key: " + key);
							    String[] io = key.replaceAll("\\s","").split(":");
							    
							    int pos = Integer.valueOf(io[0]);
							    if(io[1].equals("class"))
							    	continue;
							    
							    
							    if(param.args[pos-1].getClass().getCanonicalName().equalsIgnoreCase(obj.get(key).getClass().getCanonicalName()))
							    	param.args[pos-1] = obj.get(key);
							    
							    else{
							    	Log.e(TAG, "old arg: " + param.args[pos-1].getClass().getCanonicalName());
								    Log.e(TAG, "new arg: " + key + "@" + (pos-1) + " : " + obj.get(key).getClass().getCanonicalName());
							    }
							    	
							    
							}
						}
					}catch(Exception e) {
						Log.e(TAG, "get params error: " + e.getMessage());
						return;
					}
					
				}else{
					Log.d(TAG, "proceed without new data" + param.method.getName());
				}
				
			}else{
//				Log.d(TAG, "no stepping");
			}
		}

	}
	
	@Override
	protected void afterHookedMethod(MethodHookParam param) throws Throwable {
		JSONObject jMethod = new JSONObject();
		jMethod.put("class", param.method.getDeclaringClass().getCanonicalName());
		jMethod.put("method", param.method.getName());
		jMethod.put("result", getPrimitiveObjects(new Object[]{param.getResult()}, 0));
		JSONObject jClass = new JSONObject();
		jClass.put("fields", new JSONArray().put(getFieldObjects(param.method.getDeclaringClass().getFields(), param.thisObject, 0)));
		
		
		long tid = Thread.currentThread().getId();
		XListener lock = threads.get(tid);
		
		//check if entry is available
		if(!threads.containsKey(tid)){
			//open new page
//			DAMNServer.newAppStarted(app, tid);
			
			XListener xl = new XListener(app, Thread.currentThread(), this);
			xl.STEPPING = false;
			xl.start();
			
			threads.put(tid, xl);
		}
		
		synchronized (lock) {
			DAMNServer.push(new MessageObject(Code.varReturn, jMethod.toString(), app, tid));
			if(lock.STEPPING) {
				DAMNServer.push(new MessageObject(Code.varGlobal, jClass.toString(), app, tid));
				
				String source = null;
				try {
					source = readSource("data/data/" + app + "/source/" + param.method.getDeclaringClass().getCanonicalName().replace(".", "/") + ".java");
					
				} catch(Exception e) {
					Log.e(TAG, "something terrible going wrong");
				}
				if(source!=null)
					DAMNServer.push(new MessageObject(
						Code.methodCode, 
						source, 
						app, 
						tid));
			}
		}
				
		//wait for response if stepping is on
		synchronized (lock) {
			if(lock.STEPPING) {
				String data = lock.waitForNextStep();
				if(data!=null){
					Log.d(TAG, "json: " + data);
					JSONObject payload = new JSONObject(data);
					Log.d(TAG, "set new return val " + param.method.getName());
					
					JSONObject ret = null;
					//change parameters
					try{
						ret = (JSONObject) payload.get("result");
						
					}catch(Exception e) {
						Log.e(TAG, "returnvalue: " + e.getMessage());
						return;
					}
					
					Iterator<?> keys = ret.keys();

					while( keys.hasNext() ) {
					    String key = (String)keys.next();
					    Log.d(TAG, "key: " + key);
					    String[] io = key.replaceAll("\\s","").split(":");
					    
					    int pos = Integer.valueOf(io[0]);
					    if(io[1].equals("class"))
					    	continue;
					    
					    
					    if(param.getResult().getClass().getCanonicalName().equalsIgnoreCase(ret.get(key).getClass().getCanonicalName()))
					    	param.setResult(ret.get(key));
					    
					    else{
					    	Log.e(TAG, "old result: " + param.getResult().getClass().getCanonicalName());
						    Log.e(TAG, "new result: " + key + "@" + (pos-1) + " : " + ret.get(key).getClass().getCanonicalName());
					    }
					}
					
				}else{
					Log.d(TAG, "proceed without new data" + param.method.getName());
				}
				
			}else{
//						Log.d(TAG, "no stepping");
			}
		}
	}
	
	
	private String readSource(String clazz) {
		StringBuilder sb = new StringBuilder();
		
		FileInputStream fis = null;
	    InputStreamReader isr = null;
	    BufferedReader bufferedReader = null;
		
		try { 
			fis = new FileInputStream(clazz);
		    isr = new InputStreamReader(fis);
		    bufferedReader = new BufferedReader(isr);
		    
		    String line;
		    while ((line = bufferedReader.readLine()) != null)
		        sb.append(line + "<br>");
		    
		    return sb.toString();
	    
		}catch(Exception e) {
			Log.e(TAG, e.getMessage());
			
		}finally {
			try {
				if(bufferedReader!=null)
					bufferedReader.close();
				if(isr!=null)
					isr.close();
				if(fis!=null)
					fis.close();
			    
			} catch (Exception e) { }
		}
		
		return "no code available";
	}
	
	
	private JSONObject getFieldObjects(Field[] fields, Object object, int limit) {
		JSONObject jParameters = new JSONObject();
		try {
			
			for(int n=0; n<fields.length; n++){
				fields[n].setAccessible(true);
				Class<?> clazz = fields[n].getType();
				String key = (n+1) + ": " + fields[n].getName();
				Object value = fields[n].get(object);
				
					if(clazz.getCanonicalName().equals(String.class.getCanonicalName()))
						jParameters.put(key, (String)value);
					else if(clazz.getCanonicalName().equals(Integer.class.getCanonicalName()))
						jParameters.put(key, (Integer)value);
					else if(clazz.getCanonicalName().equals(Long.class.getCanonicalName()))
						jParameters.put(key, (Long)value);
					else if(clazz.getCanonicalName().equals(Boolean.class.getCanonicalName()))
						jParameters.put(key, (Boolean)value);
					else if(clazz.getCanonicalName().equals(Float.class.getCanonicalName()))
						jParameters.put(key, (Float)value);
					else if(clazz.getCanonicalName().equals(Double.class.getCanonicalName()))
						jParameters.put(key, (Double)value);
					else if(clazz.getCanonicalName().equals(Character.class.getCanonicalName()))
						jParameters.put(key, (Character)value);
					else if(clazz.getCanonicalName().equals(Byte.class.getCanonicalName()))
						jParameters.put(key, (Byte)value);
					else if(clazz.getCanonicalName().equals(int.class.getCanonicalName()))
						jParameters.put(key, (int)value);
					else if(clazz.getCanonicalName().equals(long.class.getCanonicalName()))
						jParameters.put(key, (long)value);
					else if(clazz.getCanonicalName().equals(boolean.class.getCanonicalName()))
						jParameters.put(key, (boolean)value);
					else if(clazz.getCanonicalName().equals(float.class.getCanonicalName()))
						jParameters.put(key, (float)value);
					else if(clazz.getCanonicalName().equals(double.class.getCanonicalName()))
						jParameters.put(key, (double)value);
					else if(clazz.getCanonicalName().equals(char.class.getCanonicalName()))
						jParameters.put(key, (char)value);
					else if(clazz.getCanonicalName().equals(byte.class.getCanonicalName()))
						jParameters.put(key, (byte)value);
					else if(clazz.getCanonicalName().equals(byte[].class.getCanonicalName()))
						jParameters.put(key, new String((byte[])object));
					else {
						jParameters.put(key, clazz.getCanonicalName());
					}
			}
		} catch (Exception e) {
//			Log.e(TAG, "error: " + e.getLocalizedMessage());
		}
		
		return jParameters;
	}
	
	private JSONObject getPrimitiveObjects(Object[] objects, int limit) throws JSONException {
		JSONObject jParameters = new JSONObject();
		
		if(objects!=null)
			for(int n=0; n<objects.length; n++)
				if(objects[n]!=null)
					jParameters = getObjects(objects[n].getClass(), objects[n], jParameters, n, 0);
		
		return jParameters;
	}
	
	private JSONObject getObjects(Class<?> clazz, Object object, JSONObject jObject, int position, int limit) {
		
		try {
			if(clazz.getCanonicalName().equals(String.class.getCanonicalName()))
				jObject.put((position) + ": String", (String)object);
			else if(clazz.getCanonicalName().equals(Integer.class.getCanonicalName()))
				jObject.put((position+1) + ": Integer", (Integer)object);
			else if(clazz.getCanonicalName().equals(Long.class.getCanonicalName()))
				jObject.put((position+1) + ": Long", (Long)object);
			else if(clazz.getCanonicalName().equals(Boolean.class.getCanonicalName()))
				jObject.put((position+1) + ": Boolean", (Boolean)object);
			else if(clazz.getCanonicalName().equals(Float.class.getCanonicalName()))
				jObject.put((position+1) + ": Float", (Float)object);
			else if(clazz.getCanonicalName().equals(Double.class.getCanonicalName()))
				jObject.put((position+1) + ": Double", (Double)object);
			else if(clazz.getCanonicalName().equals(Character.class.getCanonicalName()))
				jObject.put((position+1) + ": Character", (Character)object);
			else if(clazz.getCanonicalName().equals(Byte.class.getCanonicalName()))
				jObject.put((position+1) + ": Byte", (Byte)object);
			else if(clazz.getCanonicalName().equals(int.class.getCanonicalName()))
				jObject.put((position+1) + ": int", (int)object);
			else if(clazz.getCanonicalName().equals(long.class.getCanonicalName()))
				jObject.put((position+1) + ": long", (long)object);
			else if(clazz.getCanonicalName().equals(boolean.class.getCanonicalName()))
				jObject.put((position+1) + ": boolean", (boolean)object);
			else if(clazz.getCanonicalName().equals(float.class.getCanonicalName()))
				jObject.put((position+1) + ": float", (float)object);
			else if(clazz.getCanonicalName().equals(double.class.getCanonicalName()))
				jObject.put((position+1) + ": double", (double)object);
			else if(clazz.getCanonicalName().equals(char.class.getCanonicalName()))
				jObject.put((position+1) + ": char", (char)object);
			else if(clazz.getCanonicalName().equals(byte.class.getCanonicalName()))
				jObject.put((position+1) + ": byte", (byte)object);
			else if(clazz.getCanonicalName().equals(byte[].class.getCanonicalName()))
				jObject.put((position+1) + ": byte[]", new String((byte[])object));
			else if(clazz.getCanonicalName().equals(ArrayList.class.getCanonicalName()))
				jObject.put((position+1) + ": ArrayList", ((ArrayList<?>)object).toString());
			else {
				jObject.put((position+1) + ": class", clazz.getCanonicalName());
			}
		
		} catch (Exception e) { 
//			Log.e(TAG, "error: " + e.getLocalizedMessage());
		}
		
		return jObject;
	}
}
