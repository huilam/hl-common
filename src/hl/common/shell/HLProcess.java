package hl.common.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HLProcess implements Runnable
{
	public PrintStream out 				= null;//System.out;
	
	private String id					= null;
	private List<String> listOutput		= new ArrayList<String>();
	private long output_hist_max_lines	= 100;
	
	private Pattern patt_init_success	= null;
	private boolean is_init_success		= false;
	private long init_timeout_ms		= 0;
	
	private long run_start_timestamp	= 0;
	private long delay_start_ms			= 0;
	
	private long dep_check_interval_ms	= 100;
	private long dep_wait_timeout_ms	= 10000;
	
	private Collection<HLProcess> depends = new ArrayList<HLProcess>();
	private String[] commands			= null;
	
	private boolean remote_ref			= false;
	private Process proc 				= null;
	
	public static Logger logger = Logger.getLogger(HLProcess.class.getName());
	
	public HLProcess(String aId, String[] aShellCmd)
	{
		this.id = aId;
		this.commands = aShellCmd;
	}

	public HLProcess(String aId)
	{
		this.id = aId;
	}

	public void setProcessCommand(String[] aShellCmd)
	{
		this.commands = aShellCmd;
	}
	
	public void setProcessId(String aId)
	{
		this.id = aId;
	}
	
	public String getProcessId()
	{
		return this.id;
	}	
	
	public void setRemoteRef(boolean aRemoteRef)
	{
		this.remote_ref = aRemoteRef;
	}
	
	public boolean isRemoteRef()
	{
		return this.remote_ref;
	}	
	
	public void setProcessOutputMaxHist(long aMaxHistLines)
	{
		this.output_hist_max_lines = aMaxHistLines;
	}
	
	public long getProcessOutputMaxHist()
	{
		return this.output_hist_max_lines;
	}
	
	public void setProcessStartDelayMs(long aDelayMs)
	{
		this.delay_start_ms = aDelayMs;
	}
	
	public long getProcessStartDelayMs()
	{
		return this.delay_start_ms;
	}
	
	public void setInitTimeoutMs(long aInitTimeoutMs)
	{
		this.init_timeout_ms = aInitTimeoutMs;
	}
	
	public long getInitTimeoutMs()
	{
		return this.init_timeout_ms;
	}
	
	public void setInitSuccessRegex(String aRegex)
	{
		if(aRegex==null || aRegex.trim().length()==0)
			this.patt_init_success = null;
		else
			this.patt_init_success = Pattern.compile(aRegex);
	}
	
	public String getInitSuccessRegex()
	{
		if(this.patt_init_success==null)
			return null;
		else
			return this.patt_init_success.pattern();
	}
	//

	public String getProcessCommand()
	{
		if(this.commands==null)
			return "";
		return String.join(" ",this.commands);
	}

	public List<String> getProcessOutputHist()
	{
		return this.listOutput;
	}
	
	public void setDependTimeoutMs(long aTimeoutMs)
	{
		this.dep_wait_timeout_ms = aTimeoutMs;
	}
	
	public long getDependTimeoutMs()
	{
		return this.dep_wait_timeout_ms;
	}
	
	public void setDependCheckIntervalMs(long aCheckIntervalMs)
	{
		this.dep_check_interval_ms = aCheckIntervalMs;
	}
	
	public long getDependCheckIntervalMs()
	{
		return this.dep_check_interval_ms;
	}
	
	//////////
	
	public void addDependProcess(HLProcess aDepProcess)
	{
		depends.add(aDepProcess);
	}
	
	public void clearDependProcesses()
	{
		depends.clear();;
	}	
	//////////
	
	public boolean isRunning()
	{
		return this.run_start_timestamp>0 || (proc!=null && proc.isAlive());
	}
	
	public boolean isInitSuccess()
	{
		return this.is_init_success;
	}
	
	private void logDebug(String aMsg)
	{
		if(out!=null)
		{
			out.println(aMsg);
		}
		logger.log(Level.FINEST, aMsg);
	}
	
	@Override
	public void run() {
		this.run_start_timestamp = System.currentTimeMillis();
		String sPrefix = (id==null?"":"["+id+"] ");
		
		logger.log(Level.INFO, "HLProcess.run() start - "+getProcessId());
		try {
			if(depends!=null && depends.size()>0)
			{
				Collection<HLProcess> tmpDepends = new ArrayList<HLProcess>();
				tmpDepends.addAll(depends);
				
				StringBuffer sbDepCmd = new StringBuffer();
				while(tmpDepends.size()>0)
				{
					sbDepCmd.setLength(0);
					
					Iterator<HLProcess> iter = tmpDepends.iterator();
					long lElapsed = System.currentTimeMillis() - this.run_start_timestamp;
					while(iter.hasNext())
					{
						HLProcess d = iter.next();
						
						if(d.isInitSuccess())
						{
							iter.remove();
							continue;
						}
						else
						{
							sbDepCmd.append("\n - ");
							sbDepCmd.append(d.id).append(" : ");
							if(d.isRemoteRef())
								sbDepCmd.append("(remote)");
							else
								sbDepCmd.append(d.getProcessCommand());
						}
						
						if(lElapsed >= this.dep_wait_timeout_ms)
						{
							String sErr = sPrefix+"Dependance process(es) init timeout ! "+sbDepCmd.toString();
							logger.log(Level.SEVERE, sErr);
							throw new RuntimeException(sErr);
						}
					}
					
					try {
						Thread.sleep(this.dep_check_interval_ms);
					} catch (InterruptedException e) {
						logger.log(Level.WARNING, e.getMessage(), e);
					}
				}
			}
			
			ProcessBuilder pb = new ProcessBuilder(this.commands);
			try {
				proc = pb.start();
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			}
			
			long lStart = System.currentTimeMillis();
			BufferedReader rdr = null;
			try {
				rdr = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				
				while(proc.isAlive())
				{
					String sLine = rdr.readLine();
					
					if(sLine!=null)
					{
						logDebug(sPrefix + sLine);
					}
					else
					{
						sLine = "";
					}
					
					if(this.output_hist_max_lines>0)
					{
						if(this.listOutput.size()>=this.output_hist_max_lines)
						{
							this.listOutput.remove(0);
						}
					}
					this.listOutput.add(sLine);
					
					if(!isInitSuccess())
					{
						if(this.patt_init_success!=null)
						{
							if(!this.is_init_success && sLine!=null)
							{
								Matcher m = patt_init_success.matcher(sLine);
								this.is_init_success = m.find();
								if(this.is_init_success)
								{
									logger.log(Level.INFO, 
											sPrefix + "init_success - Elapsed: "+milisec2Words(System.currentTimeMillis()-this.run_start_timestamp));
								}
							}
						}
						else
						{
							is_init_success = true;
						}
						
						if(init_timeout_ms>0)
						{
							long lElapsed = System.currentTimeMillis() - lStart;
							if(lElapsed>=init_timeout_ms)
							{
								String sErr = sPrefix+"Init timeout ! "+milisec2Words(lElapsed)+" - "+getProcessCommand();
								logger.log(Level.SEVERE, sErr);
								throw new RuntimeException(sErr);
							}
							
						}
					}
				}
			} catch (IOException e) {
				logger.log(Level.WARNING, e.getMessage(), e);
			}
			finally
			{
				if(rdr!=null)
					try {
						rdr.close();
					} catch (IOException e) {
					}
			}
			
		}
		finally
		{
			long lElapsed = (System.currentTimeMillis()-this.run_start_timestamp);
			logger.log(Level.INFO, "HLProcess.run() end - "+getProcessId()+" (elapsed: "+milisec2Words(lElapsed)+")");
		}
	}
	
	private String milisec2Words(long aElapsed)
	{
		StringBuffer sb = new StringBuffer();
		long lTmp = aElapsed;
		
		if(lTmp>=3600000)
		{
			sb.append(lTmp / 3600000).append("h ");
			lTmp = lTmp % 3600000;
		}
		
		if(lTmp>=60000)
		{
			sb.append(lTmp / 60000).append("m ");
			lTmp = lTmp % 60000;
		}
		
		if(lTmp>=1000)
		{
			sb.append(lTmp / 1000).append("s ");
			lTmp = lTmp % 1000;
		}
		
		if(lTmp>0)
		{
			sb.append(lTmp).append("ms ");
		}
		
		return sb.toString().trim();
	}
	
	public String toString()
	{
		String sPrefix = "["+getProcessId()+"]";
		StringBuffer sb = new StringBuffer();
		sb.append("\n").append(sPrefix).append("is.running=").append(isRunning());
		sb.append("\n").append(sPrefix).append("is.init.success=").append(isInitSuccess());
		sb.append("\n").append(sPrefix).append("is.remote=").append(isRemoteRef());
		
		sb.append("\n").append(sPrefix).append("process.command.").append(HLProcessConfig.osname).append("=").append(getProcessCommand());
		sb.append("\n").append(sPrefix).append("process.start.delay.ms=").append(getProcessStartDelayMs());
		
		sb.append("\n").append(sPrefix).append("init.timeout.ms=").append(this.init_timeout_ms);
		sb.append("\n").append(sPrefix).append("init.success.regex=").append(this.patt_init_success==null?"":this.patt_init_success.pattern());
		
		StringBuffer sbDeps = new StringBuffer();
		if(this.depends!=null)
		{
			for(HLProcess d : this.depends)
			{
				if(sbDeps.length()>0)
				{
					sbDeps.append(",");
				}
				sbDeps.append(d.getProcessId());
				
				if(d.isRemoteRef())
					sbDeps.append("(remote)");
			}
		}
		sb.append("\n").append(sPrefix).append("dep.processes=").append(sbDeps.toString());
		sb.append("\n").append(sPrefix).append("dep.timeout.ms=").append(this.dep_wait_timeout_ms);
		sb.append("\n").append(sPrefix).append("dep.check.interval.ms=").append(this.dep_check_interval_ms);
		return sb.toString();
	}
	
}