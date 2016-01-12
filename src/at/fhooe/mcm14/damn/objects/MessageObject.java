package at.fhooe.mcm14.damn.objects;

import at.fhooe.mcm14.damn.jni.DAMNServer.Code;

public class MessageObject {
	public Code code;
	public String payload;
	public String app;
	public long threadId;
	
	public MessageObject(Code _code, String _payload, String _app, long _threadId) {
		code = _code;
		payload = _payload;
		app = _app;
		threadId = _threadId;
	}
}
