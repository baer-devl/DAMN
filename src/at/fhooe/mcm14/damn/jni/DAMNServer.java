package at.fhooe.mcm14.damn.jni;

import android.util.Log;
import at.fhooe.mcm14.damn.objects.MessageObject;


public class DAMNServer {
	
	//load library
	static {
		System.loadLibrary("damn-server");
		
        //check state
        state = getState();
    }
	
	public static enum ServerState {
		server_off(0),
		server_init(1),
		server_on(2),
		server_unknown(3);
		
		private int val;

	    private ServerState(int val) {
	        this.val = val;
	    }

	    public int getNumVal() {
	        return val;
	    }
	}
	
	public static enum Code {
		connected(0),
		pause(1),
		play(2),
		step(3),
		methodCode(4),
		varGlobal(5),
		varParam(6),
		varReturn(7),
		varManipulatedParam(8),
		varManipulatedReturn(9),
		unhook(10),
		undefined(11),
		close(12);
		
		private int val;

	    private Code(int val) {
	        this.val = val;
	    }

	    public int getNumVal() {
	        return val;
	    }
	}
	
	private static native void 		startServerJNI(String docroot, String pemfile, String cachedir);
	private static native boolean	stopServerJNI();
	private static native int 		getConnectionsJNI();
	private static native int 		getStateBlockingJNI();
	private static native int 		getStateJNI();
	private static native void 		pushJNI(int code, String payload, String application, long threadId);
	private static native String 	receiveBlockingJNI(String application, long threadId);
	private static native void		newAppJNI(String application, long threadId);
	private static native void		deleteAppJNI(String application, long threadId);
	
	private static ServerState state = ServerState.server_unknown;
	
	/**
	 * Starts the DAMN server and block until it gets terminated.
	 * 
	 * @param docroot holds the absolute path to the document folder
	 * @param pemfile holds the absolute path to the pem file
	 */
	public static void startServer(String docroot, String pemfile, String cachedir) {
		
		startServerJNI(docroot, pemfile, cachedir);
	}

	/**
	 * Stops the DAMN server and will cause the {@link DAMNServer#startServer(String, String)} to return.
	 * 
	 * @return true if server
	 */
	public static boolean stopServer() {
		return stopServerJNI();
	}
	
	/**
	 * Return the count of connected clients.
	 * 
	 * @return count of connected clients
	 */
	public static int getConnections() {
		return getConnectionsJNI();
	}
	
	/**
	 * Blocking till a state change of the server occur and return it.
	 * 
	 * @return actual state of the server
	 */
	public static ServerState getStateBlocking() {
		return ServerState.values()[getStateBlockingJNI()];
	}
	
	/**
	 * Return the actual state of the server.
	 * 
	 * @return actual state of the server
	 */
	public static ServerState getState() {
		return ServerState.values()[getStateJNI()];
	}
	
	public synchronized static void push(MessageObject msg) {
		pushJNI(msg.code.val, msg.payload, msg.app, msg.threadId);
		try {Thread.sleep(0, 100);} catch (InterruptedException e) { }
	}
	
	public static MessageObject getReceivedMessageBlocking(String app, long threadId) {
		String msg = receiveBlockingJNI(app, threadId);
		
		String payload = "";
		Code code = Code.undefined;
		try{
			code = Code.values()[Integer.valueOf(msg.substring(0, 3))];
			payload = msg.substring(3, msg.length());
			
		} catch(Exception e) {}
		
		return new MessageObject(code, payload, app, threadId);
	}
	
	public static void newAppStarted(String app) {
		newAppJNI(app, 1);
	}
	
	public static void startAppThread(String app, long threadId) {
		Log.d("faaaaat", "threadId: " + threadId);
		newAppJNI(app, threadId);
	}
	
	public static void endAppThread(String app, long threadId) {
		Log.d("faaaaat", "threadId: " + threadId);
		deleteAppJNI(app, threadId);
	}
}
