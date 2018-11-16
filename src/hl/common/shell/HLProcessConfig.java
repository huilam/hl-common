package hl.common.shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
	public static String _PROP_KEY_SHELL_CMD_BLOCK		= _PROP_KEY_SHELL+"command.block";
	
	public static String _PROP_KEY_SHELL_START_DELAY	= _PROP_KEY_SHELL+"start.delay.ms";
	public static String _PROP_KEY_SHELL_OUTPUT_FILENAME= _PROP_KEY_SHELL+"output.filename";
	public static String _PROP_KEY_SHELL_OUTPUT_CONSOLE = _PROP_KEY_SHELL+"output.console";
	public static String _PROP_KEY_SHELL_DEF2_SCRIPT_DIR = _PROP_KEY_SHELL+"default.to.script.dir";
	
	public static String _PROP_KEY_SHELL_TERMINATE_CMD	= _PROP_KEY_SHELL+"terminated.command.{os.name}";

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
	
	public static String osname 	= null;
	
	public static char commandSpace = ' ';
	public char commandBlockStart 	= commandSpace;
	public char commandBlockEnd 	= commandSpace;
	
	private Pattern pattProcessId 	= Pattern.compile(_PROP_PREFIX_PROCESS+"(.+?)\\.");	
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
		_PROP_KEY_SHELL_TERMINATE_CMD = _PROP_KEY_SHELL_TERMINATE_CMD.replaceAll("\\{"+sOsNameAttrKey+"\\}", osname);
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
	
	private String[] splitCommands(String aCmdString)
	{
		if(commandSpace == commandBlockStart && commandSpace == commandBlockEnd)
		{
			return aCmdString.split(""+commandSpace);
		}
		
		List<String> list = new ArrayList<String>();
		
		StringBuffer sb = new StringBuffer();
		
		boolean isSameBlockChar = commandBlockStart==commandBlockEnd;
		boolean isGrouping = false;
		for(char ch : aCmdString.toCharArray())
		{
			if(commandBlockStart == ch || commandBlockEnd == ch)
			{
				if(sb.length()>0)
				{
					list.add(sb.toString());
				}
				sb.setLength(0);
				
				if(isSameBlockChar)
				{
					isGrouping = !isGrouping;
				}
				else
				{
					isGrouping = commandBlockStart == ch;
				}
			}
			
			else if(commandSpace == ch)
			{
				if(isGrouping)
				{
					sb.append(ch);
				}
				else
				{
					if(sb.length()>0)
					{
						list.add(sb.toString());
					}
					sb.setLength(0);
				}
			}
			else
			{
				sb.append(ch);
			}				
		}
		
		return list.toArray(new String[list.size()]);
	}
	
	public void init(Properties aProperties) throws IOException
	{		
		if(aProperties==null)
			aProperties = new Properties();
		
		Matcher m = null;
		
		Map<String, Map<String, String>> mapPID = new HashMap<String, Map<String, String>> ();
		
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
				
				Map<String, String> mapProcessConfig = mapPID.get(sPID);
				if(mapProcessConfig==null)
					mapProcessConfig = new HashMap<String, String>();
				
				mapProcessConfig.put(sConfigKey, sConfigVal);
				mapPID.put(sPID, mapProcessConfig);
			}
		}
				
		
		iter = mapPID.keySet().iterator();
		String sConfigVal = null;
		while(iter.hasNext())
		{
			String sPID = (String) iter.next();
			Map<String, String> mapProcessConfig = mapPID.get(sPID);
		
			HLProcess p = mapProcesses.get(sPID);
			if(p==null)
			{
				p = new HLProcess(sPID);
			}
				
			// SHELL
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_CMD_BLOCK);
			if(sConfigVal!=null)
			{
				sConfigVal = sConfigVal.trim();
				
				if(sConfigVal.length()==1)
				{
					this.commandBlockStart = sConfigVal.charAt(0);
					this.commandBlockEnd = this.commandBlockStart;
				}
				else if(sConfigVal.length()==2)
				{
					this.commandBlockStart = sConfigVal.charAt(0);
					this.commandBlockEnd = sConfigVal.charAt(1);
				}
			}
			// 
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_COMMAND);
			if(sConfigVal!=null)
			{
				p.setProcessCommand(splitCommands(sConfigVal));
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_OUTPUT_CONSOLE);
			if(sConfigVal!=null)
			{
				p.setOutputConsole("true".equalsIgnoreCase(sConfigVal));
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_OUTPUT_FILENAME);
			if(sConfigVal!=null)
			{
				p.setProcessOutputFilename(sConfigVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_START_DELAY);
			if(sConfigVal!=null)
			{
				long lVal = Long.parseLong(sConfigVal);
				p.setProcessStartDelayMs(lVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_TERMINATE_CMD);
			if(sConfigVal!=null)
			{
				p.setTerminatedCommand(sConfigVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_SHELL_DEF2_SCRIPT_DIR);
			if(sConfigVal!=null)
			{
				p.setDefaultToScriptDir("true".equalsIgnoreCase(sConfigVal));
			}

			//INIT
			sConfigVal = mapProcessConfig.get(_PROP_KEY_INIT_SUCCESS_REGEX);
			if(sConfigVal!=null)
			{
				p.setInitSuccessRegex(sConfigVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_INIT_FAILED_REGEX);
			if(sConfigVal!=null)
			{
				p.setInitFailedRegex(sConfigVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_INIT_TIMEOUT_MS);
			if(sConfigVal!=null)
			{
				long lVal = Long.parseLong(sConfigVal);
				p.setInitTimeoutMs(lVal);
			}
			
			//DEPENDANCIES
			for(String sConfigKey : new String[] {_PROP_KEY_DEP_PROCESSES_LOCAL, _PROP_KEY_DEP_PROCESSES_REMOTE})
			{
				sConfigVal = mapProcessConfig.get(sConfigKey);
				if(sConfigVal!=null)
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
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_DEP_TIMEOUT_MS);
			if(sConfigVal!=null)
			{
				long lVal = Long.parseLong(sConfigVal);
				p.setDependTimeoutMs(lVal);
			}
			//
			sConfigVal = mapProcessConfig.get(_PROP_KEY_DEP_CHK_INTERVAL_MS);
			if(sConfigVal!=null)
			{
				long lVal = Long.parseLong(sConfigVal);
				p.setDependCheckIntervalMs(lVal);
			}
			//
			mapProcesses.put(sPID, p);
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
		if(mapProcesses==null)
			return new HLProcess[]{};
		
		Collection<HLProcess> c = mapProcesses.values();
		return c.toArray(new HLProcess[c.size()]);
	}
	
	//--
	/** sample 'process.properties'
	//--
# process.@.shell.start.delay.ms=
# process.@.shell.default.to.script.dir=false
# process.@.shell.command.block=
# process.@.shell.command.win=
# process.@.shell.command.linux=
# process.@.shell.command.mac=
# process.@.shell.output.filename=
# process.@.shell.output.console=false
# process.@.shell.terminated.command.win=
# process.@.shell.terminated.command.linux=
# process.@.shell.terminated.command.mac=

# process.@.init.timeout.ms=
# process.@.init.success.regex=
# process.@.init.failed.regex=

# process.@.dependance.processes.local=
# process.@.dependance.processes.remote=
# process.@.dependance.check.interval.ms=
# process.@.dependance.timeout.ms=
	//--
	**/
	
}