package hl.common.shell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
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
	public char commandBlockStart 	= '[';
	public char commandBlockEnd 	= ']';
	
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
			
			else if(' ' == ch)
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
		
		Map map = new TreeMap();
		map.putAll(aProperties);
		
		
		Iterator iter = map.keySet().iterator();

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
						p.setProcessCommand(splitCommands(sConfigVal));
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
					else if(sConfigKey.equals(_PROP_KEY_SHELL_CMD_BLOCK))
					{
						if(sConfigVal.trim().length()==1)
						{
							this.commandBlockStart = sConfigVal.trim().charAt(0);
						}
					}
					else if(sConfigKey.equals(_PROP_KEY_SHELL_CMD_BLOCK))
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
	
	//--
	/** sample 'process.properties'
	//--
	process.pid.shell.command.block=[]
	process.pid.shell.start.delay.ms=	
	process.pid.shell.command.win=
	process.pid.shell.command.linux=
	process.pid.shell.command.mac=
	process.pid.shell.default.to.script.dir=
	process.pid.shell.output.filename=
	process.pid.shell.output.console=true
	//--
	process.pid.init.timeout.ms=
	process.pid.init.success.regex=
	process.pid.init.failed.regex=
	//--
	process.p1.dependance.processes.local=
	process.p1.dependance.processes.remote=
	process.pid.dependance.check.interval.ms=
	process.pid.dependance.timeout.ms=
	//--
	**/
	
}