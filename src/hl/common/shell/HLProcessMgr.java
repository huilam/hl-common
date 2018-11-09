package hl.common.shell;

import java.io.IOException;
import java.io.PrintStream;

import hl.common.shell.HLProcess;
import hl.common.shell.HLProcessConfig;

public class HLProcessMgr
{
	private HLProcessConfig procConfig = null;
	
	public HLProcessMgr(String aPropFileName)
	{
		try {
			procConfig = new HLProcessConfig("process.properties");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public HLProcess[] getAllProcesses()
	{
		return procConfig.getProcesses();
	}
	
	public HLProcess getProcess(String aProcessID)
	{
		return procConfig.getProcess(aProcessID);
	}
	
	public void startAllProcesses()
	{
		startAllProcesses(null);
	}
	
	public void startAllProcesses(PrintStream aPrintStream)
	{
		long lStart = System.currentTimeMillis();
		int lPendingStart = procConfig.getProcesses().length;
		
		for(HLProcess p : procConfig.getProcesses())
		{
			p.out = aPrintStream;
		}
		
		while(lPendingStart>0)
		{
			for(HLProcess p : procConfig.getProcesses())
			{
				if(p.isRemoteRef())
				{
					lPendingStart--;
				}
				else if(!p.isRunning())
				{
					long lElapsed = System.currentTimeMillis()-lStart;
					if(lElapsed >= p.getProcessStartDelayMs())
					{
						new Thread(p).start();
						lPendingStart--;
					}
				}
			}
			
			if(lPendingStart>0)
			{
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}