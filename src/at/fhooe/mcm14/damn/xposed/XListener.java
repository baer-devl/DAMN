package at.fhooe.mcm14.damn.xposed;

import android.util.Log;
import at.fhooe.mcm14.damn.jni.DAMNServer;
import at.fhooe.mcm14.damn.objects.MessageObject;

public class XListener extends Thread {
	public boolean STEPPING = true;
	private boolean first = false;
	private boolean running = true;
	private final static String TAG = XListener.class.getName();
	private volatile String app = null;
	private volatile String data = null;
	private Thread thread = null;
	private long tid = 0;
	private XDAMNHook hook = null;
	
	
	public XListener(String _app, Thread _thread, XDAMNHook _hook) {
		app = _app;
		thread = _thread;
		hook = _hook;
		
		tid = thread!=null ? thread.getId() : 1L;
		
		if(thread!=null)
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					// check if thread running
					Log.d(TAG, "join thread [" + tid + "]");
					
					try{
						thread.join();
						
					}catch(Exception e) {
						Log.e(TAG, "error thread join [" + tid + "]");
						Log.e(TAG, e.getMessage());
					}
					
					Log.i(TAG, "thread ended [" + tid + "]");
					hook.threads.remove(tid);
					running = false;
					
					//close browser tab remotely
					DAMNServer.endAppThread(app, tid);
				}
			}).start();
	}
	
	
	@Override
	public void run() {
		while(running) {
			Log.d(TAG, "wait for " + app + ":" + tid);
			MessageObject msg = DAMNServer.getReceivedMessageBlocking(app, tid);
			Log.d(TAG, "got: " + msg.code.name() + ":" + tid);
			
			switch (msg.code) {
			case play:
				synchronized (app) {
					STEPPING = false;
					app.notifyAll();
					Log.d(TAG, "play");
				}
				break;
				
			case pause:
				synchronized (app) {
					STEPPING = true;
					app.notify();
					Log.d(TAG, "pause");
				}
				break;
				
			case step:
				synchronized (app) {
					STEPPING = true;
					data = msg.payload.length()==0 ? null : msg.payload;
					app.notify();
					Log.d(TAG, "step");
				}
				break;

			default:
				Log.d(TAG, "default: " + msg.code.name());
				break;
			}
		}
		
		Log.i(TAG, "end listening to " + app + ":" + tid);
	}
	
	public String waitForNextStep() {
		synchronized (app) {
			try {
				if(!first) {
					first = true;
					Log.d(TAG, "first was flase");
					
				}else{
					app.wait();
					return data;
				}
				
			} catch (InterruptedException e) {
				e.printStackTrace();
				Log.e(TAG, "error: " + e.getLocalizedMessage());
			}
			
			return null;
		}
	}
}
