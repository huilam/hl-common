package hl.common.shell;

public interface HLProcessEvent 
{
	void onProcessStart(HLProcess p);
	void onProcessError(HLProcess p, Throwable e);
	void onProcessTerminate(HLProcess p);
}