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
								logger.log(Level.INFO, p.getProcessId()+" waiting for other processes to terminate ...");
								terminateAllProcesses();
								
								long lStart = System.currentTimeMillis();
								long lShutdownElapsed 		= 0;
								long lShutdown_timeout_ms 	= p.getShutdownTimeoutMs();
								int iActiveProcess = 1;
								while(iActiveProcess>0)
								{
									iActiveProcess = 0;
									for(HLProcess proc : getAllProcesses())
									{
										if(!proc.getProcessId().equals(p.getProcessId()) && proc.isRunning())
										{
											iActiveProcess++;
										}
									}
									
									lShutdownElapsed = System.currentTimeMillis() - lStart;
									
									if(lShutdown_timeout_ms>0 && lShutdownElapsed >= lShutdown_timeout_ms)
									{
										//kill all 
										StringBuffer sb = new StringBuffer();
										
										
										sb.append("Shutdown timeout - ").append(lShutdown_timeout_ms).append("ms, processes pending termination:");
										
										int i = 1;
										for(HLProcess proc : getAllProcesses())
										{
											if(!proc.getProcessId().equals(p.getProcessId()) && proc.isRunning())
											{
												sb.append("\n ").append(i++).append(". [").append(proc.getProcessId()).append("]:").append(proc.getProcessCommand());
											}
										}

										logger.log(Level.WARNING, sb.toString());
										
										System.exit(1);
									}
									try {
										Thread.sleep(100);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
						}
					};
			
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

	public synchronized void startAllProcesses()
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
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}