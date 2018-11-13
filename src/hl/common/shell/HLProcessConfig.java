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
	public static String _PROP_KEY_SHELL_COMMAND	 	= _PROP_KEY_SHELL+"command.{os.name}";
	
	public static String _PROP_KEY_SHELL_START_DELAY	= _PROP_KEY_SHELL+"start.delay.ms";
	public static String _PROP_KEY_SHELL_OUTPUT_FILENAME= _PROP_KEY_SHELL+"output.filename";
	public static String _PROP_KEY_SHELL_OUTPUT_CONSOLE = _PROP_KEY_SHELL+"output.console";
	public static String _PROP_KEY_SHELL_DEF2_SCRIPT_DIR = _PROP_KEY_SHELL+"default.to.script.dir";
	
	//-- INIT
	public static String _PROP_KEY_INIT					= "init.";
	public static String _PROP_KEY_INIT_TIMEOUT_MS 		= _PROP_KEY_INIT+"timeout.ms";
	public static String _PROP_KEY_INIT_SUCCESS_REGEX	= _PROP_KEY_INIT+"success.regex";
	public static String _PROP_KEY_INIT_FAILED_REGEX	= _PROP_KEY_INIT+"failed.regex";

	//-- DEPENDANCE
	public static String _PROP_KEY_DEP					= "dependance.";
	public static String _PROP_KEY_DEP_PROCESSES_LOCAL	= _PROP_KEY_DEP+"processes.local";
	public static String _PROP_KEY_DEP_PROCESSES_REMOTE	= _PROP_KEY_DEP+"processes.remote";
	public static String _PROP_KEY_DEP_CHK_INTERVAL_MS 	= _PROP_KEY_DEP+"check.interval.ms";
	public static String _PROP_KEY_DEP_TIMEOUT_MS 		= _PROP_KEY_DEP+"timeout.ms";
	
	public static String osname = null;
	private Pattern pattProcessId = Pattern.compile(_PROP_PREFIX_PROCESS+"(.+?)\\.");
	private Map<String, HLProcess> mapProcesses = new HashMap<String, HLProcess>();
	//
	
	static {
		String sOsNameAttrKey = "os.name";
		String sOsName = System.getProperty(sOsNameAttrKey).toLowerCase();
		if(sOsName.indexOf("windows")>-1)
			sOsName = "win";
		else if(sOsName.indexOf("linux")>-1)
			sOsName = "linux";
		else if(sOsName.indexOf("mac")>-1)
			sOsName = "mac";
		osname = sOsName;
		_PROP_KEY_SHELL_COMMAND = _PROP_KEY_SHELL_COMMAND.replaceAll("\\{"+sOsNameAttrKey+"\\}", osname);
	}
	
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
						p.setProcessCommand(sConfigVal.split(" "));
					}
					else if(sConfigKey.equals(_PROP_KEY_SHELL_OUTPUT_CONSOLE))
					{
						p.setOutputConsole("true".equalsIgnoreCase(sConfigVal));
					}
					else if(sConfigKey.equals(_PROP_KEY_SHELL_OUTPUT_FILENAME))
					{
						p.setProcessOutputFilename(sConfigVal);
					}
					else if(sConfigKey.equals(_PROP_KEY_SHELL_START_DELAY))
					{
						long lVal = Long.parseLong(sConfigVal);
						p.setProcessStartDelayMs(lVal);
					}
					else if(sConfigKey.equals(_PROP_KEY_SHELL_DEF2_SCRIPT_DIR))
					{
						p.setDefaultToScriptDir("true".equalsIgnoreCase(sConfigVal));
					}
					
					
				}
				else if(sConfigKey.startsWith(_PROP_KEY_INIT))
				{
					if(sConfigKey.equals(_PROP_KEY_INIT_SUCCESS_REGEX))
					{
						p.setInitSuccessRegex(sConfigVal);
					}
					else if(sConfigKey.equals(_PROP_KEY_INIT_FAILED_REGEX))
					{
						p.setInitFailedRegex(sConfigVal);
					}
					else if(sConfigKey.equals(_PROP_KEY_INIT_TIMEOUT_MS))
					{
						long lVal = Long.parseLong(sConfigVal);
						p.setInitTimeoutMs(lVal);
					}
				}
				else if(sConfigKey.startsWith(_PROP_KEY_DEP))
				{
					if(sConfigKey.equals(_PROP_KEY_DEP_PROCESSES_LOCAL)
						|| sConfigKey.equals(_PROP_KEY_DEP_PROCESSES_REMOTE))
					{
						String[] sDepIds = sConfigVal.split(",");
						for(String sDepId : sDepIds)
						{
							boolean isRemoteProc = sConfigKey.equals(_PROP_KEY_DEP_PROCESSES_REMOTE);
							sDepId = sDepId.trim();
							if(sDepId.length()>0)
							{
								HLProcess procDep = mapProcesses.get(sDepId);
								if(procDep==null)
								{
									procDep = new HLProcess(sDepId);
									if(isRemoteProc)
									{
										procDep.setRemoteRef(true);
										procDep.setDependCheckIntervalMs(0);
										procDep.setDependTimeoutMs(0);
									}
									mapProcesses.put(sDepId, procDep);
								}
								
								if(procDep.isRemoteRef() != isRemoteProc)
								{
									throw new RuntimeException("["+sPID+"] Mismatch dependancies configuration - "+sDepId);
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
			if(p.isRemoteRef())
			{
				if(p.getProcessCommand().trim().length()>0
					 
					 || p.getDependCheckIntervalMs()>0
					 || p.getDependTimeoutMs()>0
					 
					 || p.getProcessStartDelayMs()>0
					)
				{
					throw new RuntimeException("["+p.getProcessId()+"] Remote Process should not have local configuration !");
				}
			}
			else if(p.getProcessCommand().trim().length()==0)
			{
				throw new RuntimeException("["+p.getProcessId()+"] Process command cannot be empty ! - "+_PROP_KEY_SHELL_COMMAND);
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
	//--
	process.p1.shell.start.delay.ms=100
	process.p1.shell.command.win=cmd.exe /c   ping   www.google.com
	process.p1.shell.command.linux=ping   www.google.com -c 1
	process.p1.shell.output.max.history=100
	//--
	process.p1.init.timeout.ms=5000
	process.p1.init.success.regex=Request timed out
	//--
	process.p1.dependance.processes.local= p2,  p3
	process.p1.dependance.processes.remote=
	process.p1.dependance.check.interval.ms=100
	process.p1.dependance.timeout.ms=10000
	//--
	**/
	
}