package at.fhooe.mcm14.damn.xposed;

import java.util.concurrent.Callable;

import de.robv.android.xposed.XposedBridge;

public class XCallableHookConstructors implements Callable<Boolean> {

	private XDAMNHook hook;
	private Class<?> clazz;
	
	
	public XCallableHookConstructors(XDAMNHook hook, Class<?> clazz) {
		this.hook = hook;
		this.clazz = clazz;
	}
	
	@Override
	public Boolean call() {
		try{
			XposedBridge.hookAllConstructors(clazz, hook);
			return true;
			
		}catch (Exception e) {}
		
		return false;
	}

}
