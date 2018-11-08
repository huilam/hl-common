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
	
	private long dep_wait_interval_ms	= 100;
	private long dep_wait_timeout_ms	= 10000;
	
	private Collection<HLProcess> depends = new ArrayList<HLProcess>();
	private String[] commands			= null;
	private Process proc 				= null;
	
	private static Logger logger = Logger.getLogger(HLProcess.class.getName());
	
	public HLProcess(String[] aShellCmd)
	{
		this.commands = aShellCmd;
	}
	
	public String getCommand()
	{
		return String.join(" ",commands);
	}

	public List<String> getOutputHist()
	{
		return this.listOutput;
	}
	
	public void setId(String aId)
	{
		this.id = aId;
	}
	
	public void setMaxOutputHist(long aMaxHistLines)
	{
		this.output_hist_max_lines = aMaxHistLines;
	}

	public boolean isInitSuccess()
	{
		return this.is_init_success;
	}
	
	public void setInitSuccessRegex(String aRegex)
	{
		if(aRegex==null || aRegex.trim().length()==0)
			this.patt_init_success = null;
		else
			this.patt_init_success = Pattern.compile(aRegex);
	}
	
	public void addDependProcess(HLProcess aDepProcess)
	{
		depends.add(aDepProcess);
	}
	
	public void clearDependProcesses()
	{
		depends.clear();;
	}

	public boolean isRunning()
	{
		return this.run_start_timestamp>0 || (proc!=null && proc.isAlive());
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
		logger.log(Level.INFO, "HLProcess.run() start. - ["+getCommand()+"]");
		String sPrefix = (id==null?"":"["+id+"] ");
		this.run_start_timestamp = System.currentTimeMillis();
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
							sbDepCmd.append(d.getCommand());
						}
						
						if(lElapsed >= this.dep_wait_timeout_ms)
						{
							throw new RuntimeException(sPrefix+"Dependance process(es) init timeout ! "+sbDepCmd.toString());
						}
					}
					
					try {
						Thread.sleep(this.dep_wait_interval_ms);
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
									logDebug(sPrefix + "init_success - Elapsed: "+(System.currentTimeMillis()-this.run_start_timestamp));
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
								throw new RuntimeException(sPrefix+"Init timeout ! "+getCommand());
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			
		}
		finally
		{
			logDebug(sPrefix+"end - Elapsed: "+(System.currentTimeMillis()-this.run_start_timestamp));
			logger.log(Level.INFO, "HLProcess.run() completed. - ["+getCommand()+"]");
		}
	}
	
}