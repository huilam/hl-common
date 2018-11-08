package hl.common.shell;

import java.io.IOException;
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
		long lStart = System.currentTimeMillis();
		int lPendingStart = procConfig.getProcesses().length;
		while(lPendingStart>0)
		{
			for(HLProcess p : procConfig.getProcesses())
			{
				p.out = System.out;
				if(!p.isRunning())
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