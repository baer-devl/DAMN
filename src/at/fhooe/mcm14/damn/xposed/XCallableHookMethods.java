package at.fhooe.mcm14.damn.xposed;

import java.util.concurrent.Callable;

import de.robv.android.xposed.XposedBridge;

public class XCallableHookMethods implements Callable<Boolean> {

	private XDAMNHook hook;
	private Class<?> clazz;
	private String method;
	
	
	public XCallableHookMethods(XDAMNHook hook, Class<?> clazz, String method) {
		this.hook = hook;
		this.clazz = clazz;
		this.method = method;
	}
	
	@Override
	public Boolean call() {
		try{
			XposedBridge.hookAllMethods(clazz, method, hook);
			return true;
			
		}catch (Exception e) {}
		
		return false;
	}

}
