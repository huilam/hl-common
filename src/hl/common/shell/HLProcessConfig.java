package hl.common.shell;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hl.common.PropUtil;
import hl.common.shell.HLProcess;

public class HLProcessConfig {
	
	public static String _PROP_FILENAME 			= "process.properties";
	//
	public static String _PROP_PREFIX_PROCESS 		= "process.";

	//-- SHELL
	public static String _PROP_KEY_SHELL				= "shell.";
	public static String _PROP_KEY_SHELL_COMMAND	 	= _PROP_KEY_SHELL+"command.";
	public static String _PROP_KEY_SHELL_COMMAND_WIN 	= _PROP_KEY_SHELL_COMMAND+"win";
	public static String _PROP_KEY_SHELL_COMMAND_LINUX 	= _PROP_KEY_SHELL_COMMAND+"linux";	
	
	public static String _PROP_KEY_SHELL_START_DELAY	= _PROP_KEY_SHELL+"start.delay.ms";
	public static String _PROP_KEY_SHELL_MAX_HIST		= _PROP_KEY_SHELL+"output.max.history";
	
	//-- INIT
	public static String _PROP_KEY_INIT					= "init.";
	public static String _PROP_KEY_INIT_TIMEOUT_MS 		= _PROP_KEY_INIT+"timeout.ms";
	public static String _PROP_KEY_INIT_SUCCESS_REGEX	= _PROP_KEY_INIT+"success.regex";

	//-- DEPENDANCE
	public static String _PROP_KEY_DEP					= "dependance.";
	public static String _PROP_KEY_DEP_PROCESSES 		= _PROP_KEY_DEP+"processes";
	public static String _PROP_KEY_DEP_CHK_INTERVAL_MS 	= _PROP_KEY_DEP+"check.interval.ms";
	public static String _PROP_KEY_DEP_TIMEOUT_MS 		= _PROP_KEY_DEP+"timeout.ms";
	
	
	private String osName = null;
	private Pattern pattProcessId = Pattern.compile(_PROP_PREFIX_PROCESS+"(.+?)\\.");
	private Map<String, HLProcess> mapProcesses = new HashMap<String, HLProcess>();
	//
	public HLProcessConfig(String aPropFileName) throws IOException
	{
		init(aPropFileName);
	}
	
	public HLProcessConfig(Properties aProperties) throws IOException
	{
		init(aProperties);
	}
	
	public void init(String aPropFilename) throws IOException
	{		
		Properties props = null;
		if(aPropFilename!=null && aPropFilename.trim().length()>0)
		{
			props = PropUtil.loadProperties(aPropFilename);
		}
		
		/////////
		if(props==null || props.size()==0)
		{
			props = PropUtil.loadProperties(_PROP_FILENAME);
		}
		
		init(props);
	}
	
	public void init(Properties aProperties) throws IOException
	{		
		if(aProperties==null)
			aProperties = new Properties();
		
		Matcher m = null;
		Iterator iter = aProperties.keySet().iterator();
		
		this.osName = System.getProperty("os.name").toLowerCase();
		
		if(this.osName.indexOf("windows")>-1)
			this.osName = "win";
		else if(this.osName.indexOf("linux")>-1)
			this.osName = "linux";
		else if(this.osName.indexOf("mac")>-1)
			this.osName = "mac";
		
		while(iter.hasNext())
		{
			String sKey = (String) iter.next();
			
			m = pattProcessId.matcher(sKey);
			if(m.find())
			{
				String sPID = m.group(1);
				String sConfigKey = sKey.substring(m.end(1)+1);
				String sConfigVal = aProperties.getProperty(sKey);
				
				if(sConfigVal==null)
					sConfigVal = "";
				
				sConfigVal = sConfigVal.replaceAll("  {2,8}", " ").trim();
				
				if(sConfigVal.length()==0)
					continue;
				
				HLProcess p = mapProcesses.get(sPID);
				if(p==null)
					p = new HLProcess(sPID);
				//
				
				if(sConfigKey.startsWith(_PROP_KEY_SHELL))
				{
					if(sConfigKey.indexOf(_PROP_KEY_SHELL_COMMAND)>-1)
					{
						if(sConfigKey.endsWith("."+this.osName))
						{
							p.setProcessCommand(sConfigVal.split(" "));
						}
					}
					else if(sConfigKey.equals(_PROP_KEY_SHELL_MAX_HIST))
					{
						long lVal = Long.parseLong(sConfigVal);
						p.setProcessOutputMaxHist(lVal);
					}
					else if(sConfigKey.equals(_PROP_KEY_SHELL_START_DELAY))
					{
						long lVal = Long.parseLong(sConfigVal);
						p.setProcessStartDelayMs(lVal);
					}
				}
				else if(sConfigKey.startsWith(_PROP_KEY_INIT))
				{
					if(sConfigKey.equals(_PROP_KEY_INIT_SUCCESS_REGEX))
					{
						p.setInitSuccessRegex(sConfigVal);
					}
					else if(sConfigKey.equals(_PROP_KEY_INIT_TIMEOUT_MS))
					{
						long lVal = Long.parseLong(sConfigVal);
						p.setInitTimeoutMs(lVal);
					}
				}
				else if(sConfigKey.startsWith(_PROP_KEY_DEP))
				{
					if(sConfigKey.equals(_PROP_KEY_DEP_PROCESSES))
					{
						String[] sDepIds = sConfigVal.split(",");
						for(String sDepId : sDepIds)
						{
							sDepId = sDepId.trim();
							if(sDepId.length()>0)
							{
								HLProcess procDep = mapProcesses.get(sDepId);
								if(procDep==null)
								{
									procDep = new HLProcess(sDepId);
									mapProcesses.put(sDepId, procDep);
								}
								p.addDependProcess(procDep);
							}
						}
					}
					else if(sConfigKey.equals(_PROP_KEY_DEP_TIMEOUT_MS))
					{
						long lVal = Long.parseLong(sConfigVal);
						p.setDependTimeoutMs(lVal);
					}
					else if(sConfigKey.equals(_PROP_KEY_DEP_CHK_INTERVAL_MS))
					{
						long lVal = Long.parseLong(sConfigVal);
						p.setDependCheckIntervalMs(lVal);
					}
				}
				//
				mapProcesses.put(sPID, p);
			}
		}
		validateProcessConfigs();
	}
	

	private void validateProcessConfigs()
	{
		for(HLProcess p : getProcesses())
		{
			if(p.getProcessCommand().trim().length()==0)
			{
				throw new RuntimeException("["+p.getProcessId()+"] Process command cannot be empty ! - "+_PROP_KEY_SHELL_COMMAND+osName);
			}
		}
	}
	
	protected HLProcess getProcess(String aProcessID)
	{
		return mapProcesses.get(aProcessID);
	}
	
	protected HLProcess[] getProcesses()
	{
		Collection<HLProcess> c = mapProcesses.values();
		return c.toArray(new HLProcess[c.size()]);
	}
	
	/** sample 'process.properties'
	process.p1.shell.start.delay.ms=100
	process.p1.shell.command=cmd.exe /c   ping   www.google.com
	process.p1.shell.output.max.history=100
	process.p1.init.timeout.ms=5000
	process.p1.init.success.regex=Request timed out
	process.p1.dependance.processes= p2,  p3
	process.p1.dependance.check.interval.ms=100
	process.p1.dependance.timeout.ms=10000
	 **/
	
}