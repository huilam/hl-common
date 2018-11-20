package hl.common.shell;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import hl.common.shell.HLProcess;
import hl.common.shell.HLProcessConfig;

public class HLProcessMgr
{
	private HLProcessConfig procConfig = null;
	private static Logger logger  = Logger.getLogger(HLProcessMgr.class.getName());
	
	public HLProcessMgr(String aPropFileName)
	{
		try {
			procConfig = new HLProcessConfig(aPropFileName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void setLogLevel(Level aLogLevel)
	{
		logger.setLevel(aLogLevel);
		HLProcess.logger.setLevel(aLogLevel);
		HLProcessConfig.logger.setLevel(aLogLevel);
	}
	
	public Level getLogLevel()
	{
		return logger.getLevel();
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
		long lStart = System.currentTimeMillis();
		int lPendingStart = procConfig.getProcesses().length;
		
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
						p.startProcess();
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