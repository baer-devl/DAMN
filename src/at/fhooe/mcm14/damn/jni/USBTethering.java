package at.fhooe.mcm14.damn.jni;


public class USBTethering {

	/**
	 * Load usb-tethering library.
	 */
	static {
        System.loadLibrary("usb-tethering");
        
        //check state
        state = checkUSBTethering() ? USBState.usb_ready : USBState.usb_off;
    }
	
	public static enum USBState {
		usb_off(0),
		usb_init(1),
		usb_ready(2),
		usb_connected(3),
		usb_unknown(4);
		
		private int val;

	    private USBState(int val) {
	        this.val = val;
	    }

	    public int getNumVal() {
	        return val;
	    }
	}
	
	private static native String startUSBTetheringJNI();
	private static native boolean stopUSBTetheringJNI();
	private static native boolean checkUSBTetheringJNI();
	private static native int getStateBlockingJNI();
	private static native int getStateJNI();
	
	private static USBState state = USBState.usb_unknown;
	
	/**
	 * Prepare device for usb-tethering and wait till a client connect or the timeout occurs.
	 * 
	 * @return client IP address or null if no client connects
	 */
	public static String startUSBTethering() {
		//change state to init
		state = USBState.usb_init;
		//update notification
//		context.startService(new Intent(context, NotificationService.class));
		
		//start usb-tethering
		String result = startUSBTetheringJNI();
		if(result!=null)
			state = USBState.usb_connected;
		else
			state = USBState.usb_off;
		
		//update notification
//		context.startService(new Intent(context, NotificationService.class));
		
		return result;
	}

	/**
	 * Stop usb-tethering.
	 * 
	 * @return true if everything works fine, false otherwise
	 */
	public static boolean stopUSBTethering() {
		boolean result = stopUSBTetheringJNI(); 
		if(result){
			state = USBState.usb_off;
			
		}else{
			state = USBState.usb_ready; //TODO check if usb state is really on?
		}
		
		//update notification
//		context.startService(new Intent(context, NotificationService.class));
		
		return result;
	}
	
	/**
	 * Explicit check if usb-tethering is active.
	 * 
	 * @return true if active, false otherwise
	 */
	public static boolean checkUSBTethering() {
		//TODO get all states...
		return checkUSBTetheringJNI();
	}
	
	/**
	 * Block till state changed.
	 * 
	 * @return state of usb-tethering
	 */
	public static USBState getStateBlocking() {
		return USBState.values()[getStateBlockingJNI()];
	}
	
	/**
	 * Return the actual state.
	 * 
	 * @return state of usb-tethering
	 */
	public static USBState getState() {
		return USBState.values()[getStateJNI()];
	}
}
