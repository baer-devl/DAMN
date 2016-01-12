package at.fhooe.mcm14.damn.jni;

public class InstallSysLib {

	/**
	 * Load install-sys-lib library.
	 */
	static {
        System.loadLibrary("install-sys-lib");
    }
	
	private static native boolean installSysLibJNI(String path_to_lib, String[] libname, int size);
	

	public static boolean installSystemLibrary(String path_to_library, String[] libname) {
		return installSysLibJNI(path_to_library, libname, libname.length);
	}
}
