package hl.common.shell;

import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import hl.common.shell.HLProcess;
import hl.common.shell.HLProcessConfig;

public class HLProcessMgr
{
	private HLProcessConfig procConfig = null;
	private static Logger logger  	= Logger.getLogger(HLProcessMgr.class.getName());
	
	private HLProcessEvent event 	= null;
	private boolean is_terminating 	= false;
		
	public HLProcessMgr(String aPropFileName)
	{
		try {
			
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				@Override
				public void run() {
					terminateAllProcesses();
					waitForAllProcessesToBeTerminated(null);
					
				}
			}));
			
			procConfig = new HLProcessConfig(aPropFileName);
			
			event = new HLProcessEvent()
					{
						public void onProcessStart(HLProcess p) {
						}
						
						public void onProcessError(HLProcess p, Throwable e) {
						}

						public void onProcessTerminate(HLProcess p) 
						{
							if(p.isShutdownAllOnTermination())
							{
								terminateAllProcesses();
								waitForAllProcessesToBeTerminated(p);
							}
						}
					};
			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void waitForAllProcessesToBeTerminated(HLProcess aCurrentProcess)
	{
		long lStart = System.currentTimeMillis();
		long lShutdownElapsed 		= 0;
		long lShutdown_timeout_ms 	= 0;
		Vector<HLProcess> vProcesses = new Vector<HLProcess>();
		
		for(HLProcess proc : getAllProcesses())
		{
			if(aCurrentProcess!=null)
			{
				if(!proc.getProcessId().equals(aCurrentProcess.getProcessId()) && proc.isRunning())
				{
					vProcesses.add(proc);
				}
			}
			else
			{
				vProcesses.add(proc);
			}
		}
		
		if(aCurrentProcess!=null)
		{
			lShutdown_timeout_ms 	= aCurrentProcess.getShutdownTimeoutMs();
			logger.log(Level.INFO, "Waiting for all processes to be terminated ...");
		}
		
		int iActiveProcess = 1;
		while(iActiveProcess>0)
		{
			iActiveProcess = 0;
			for(HLProcess proc : vProcesses)
			{
				if(proc.isRunning())
				{
					iActiveProcess++;
				}
			}
			
			if(lShutdown_timeout_ms>0)
			{
				lShutdownElapsed = System.currentTimeMillis() - lStart;
				if(lShutdownElapsed >= lShutdown_timeout_ms)
				{
					//kill all 
					StringBuffer sb = new StringBuffer();
					
					sb.append("Shutdown timeout - ").append(lShutdown_timeout_ms).append("ms, processes pending termination:");
					
					int i = 1;
					for(HLProcess proc : getAllProcesses())
					{
						if(proc.isRunning())
						{
							sb.append("\n ").append(i++).append(". [").append(proc.getProcessId()).append("]:").append(proc.getProcessCommand());
						}
					}
	
					logger.log(Level.WARNING, sb.toString());
					System.exit(1);
				}
			}
			
			///////
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		logger.log(Level.INFO, "All processes terminated");
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
	
	public HLProcess[] getDisabledProcesses()
	{
		return procConfig.getDisabledProcesses();
	}
	
	public HLProcess getProcess(String aProcessID)
	{
		return procConfig.getProcess(aProcessID);
	}
	
	public synchronized void terminateAllProcesses()
	{		
		Vector<HLProcess> procPendingShutdown = new Vector<HLProcess>();
		
		for(HLProcess p : procConfig.getProcesses())
		{
			if(!p.isRemoteRef() && p.isRunning())
			{
				if(!p.isRemoteRef() && p.isRunning())
				{
					p.terminateProcess();
				}
				procPendingShutdown.add(p);
			}
		}
	}

	public void startAllProcesses()
	{
		startAllProcesses(200);
	}
	
	private synchronized void startAllProcesses(long lSleepMs)
	{
		if(is_terminating)
			return;
		
		is_terminating = true;
		
		long lStart = System.currentTimeMillis();
		int lPendingStart = procConfig.getProcesses().length;
		
		while(lPendingStart>0)
		{
			for(HLProcess p : procConfig.getProcesses())
			{
				p.setEventListener(event);
				if(p.isRemoteRef())
				{
					lPendingStart--;
				}
				else if(!p.isStarted())
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
					Thread.sleep(lSleepMs);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}