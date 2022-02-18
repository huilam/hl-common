package hl.common.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;


public class CommonInfo {

	private static JSONObject jsonEnvProp = null;
	private static JSONObject jsonSysProp = null;
	
	private static final String[] CMD_CPU_MODEL_LINUX 	= new String[] {"/bin/sh", "-c", "cat /proc/cpuinfo | grep 'model name'" };
	private static final String[] CMD_CPU_MODEL_MACOS  	= new String[] {"/bin/sh", "-c", "sysctl -n machdep.cpu.brand_string | grep ''" };
	private static final String[] CMD_CPU_MODEL_WIN  	= new String[] {"echo","%PROCESSOR_IDENTIFIER%"};
	
	private static String sCpuInfo = null;
	
    private static String getCpuInfo()
    {
    	if(sCpuInfo!=null && sCpuInfo.length()>0)
    	{
    		return sCpuInfo;
    	}
    	
    	String sOSName = System.getProperty("os.name");
		if(sOSName==null)
			sOSName = "";
		
		boolean isWindows = sOSName.toLowerCase().indexOf("win")>-1;
		boolean isMac = sOSName.toLowerCase().indexOf("mac")>-1;
		
		String[] sCpuCmd = CMD_CPU_MODEL_LINUX;
		if(isWindows)
			sCpuCmd = CMD_CPU_MODEL_LINUX;
		else if(isMac)
			sCpuCmd = CMD_CPU_MODEL_MACOS;
		
    	List<String> listCpu = CommonInfo.execCommand(sCpuCmd);
    	if(listCpu!=null && listCpu.size()>0)
    	{
    		if(listCpu.get(0)!=null)
    		{
    			sCpuInfo = listCpu.get(0);
    		}
    	}
    	return sCpuInfo;
    }
	
    
    public static JSONObject getJDKInfo()
    {
    	JSONObject jsonInfo = new JSONObject();
    	
    	Runtime rt = Runtime.getRuntime();
    	jsonInfo.put("processor.available", rt.availableProcessors());
    	jsonInfo.put("memory.free", toWords(rt.freeMemory()));
    	jsonInfo.put("memory.total", toWords(rt.totalMemory()));
    	jsonInfo.put("memory.maximum", toWords(rt.maxMemory()));
    	
    	Package pkg = Runtime.class.getPackage();
    	jsonInfo.put("implementation.vendor", pkg.getImplementationVendor());
    	jsonInfo.put("implementation.title", pkg.getImplementationTitle());
    	jsonInfo.put("implementation.version", pkg.getImplementationVersion());

    	
		JSONArray jsonArr = new JSONArray();
    	URLClassLoader urlClassLoader = (URLClassLoader) CommonInfo.class.getClassLoader();
    	for(URL url : urlClassLoader.getURLs())
    	{
    		jsonArr.put(url.getPath());
    	}
    	jsonInfo.put("java.classpath", jsonArr);
    	
    	return jsonInfo;
    }
    
    
    //
    public static JSONObject getSysProperties()
	{
		if(jsonSysProp!=null)
			return jsonSysProp;
		
		jsonSysProp = new JSONObject();
		
		Properties prop = System.getProperties();
		for(Object oKey : prop.keySet())
		{
			String sKey = oKey.toString();
			String sVal = prop.getProperty(sKey);
			
			int iPos = sKey.indexOf(".");
			if(iPos>-1)
			{
				String sPrefix = sKey.substring(0, iPos);
				
				JSONObject jsonSection = jsonSysProp.optJSONObject(sPrefix);
				if(jsonSection==null)
					jsonSection = new JSONObject();
				
				jsonSection.put(sKey, sVal);
				jsonSysProp.put(sPrefix , jsonSection);
			}
			else
			{
				jsonSysProp.put(sKey , sVal);
			}
		}
		return jsonSysProp;
	}
    
    //
    public static JSONObject getEnvProperties()
	{
		if(jsonEnvProp!=null)
			return jsonEnvProp;
		
		jsonEnvProp = new JSONObject();
		
		//System Prop
		Map<String, String> mapProp = System.getenv();
		for(String sKey : mapProp.keySet())
		{
			jsonEnvProp.put(sKey , mapProp.get(sKey));
		}
		
		if(jsonEnvProp.optString("PROCESSOR_IDENTIFIER")==null)
		{
			String sCpuInfo = getCpuInfo();
			if(sCpuInfo!=null && sCpuInfo.length()>0)
			{
				jsonEnvProp.put("PROCESSOR_IDENTIFIER", sCpuInfo);
			}
		}
		
		return jsonEnvProp;
	}
	
    
    public static JSONObject getDiskInfo()
    {
    	JSONObject jsonDrives = new JSONObject();
       	
    	JSONObject jsonTmp = null;
       	for(File drive : File.listRoots())
       	{
           	long lTotalSpace 	= drive.getTotalSpace();
           	long lAvailSpace 	= drive.getFreeSpace();
           	long lUsableSpace   = drive.getUsableSpace();
           	jsonTmp = new JSONObject();
           	jsonTmp.put("total", toWords(lTotalSpace));
           	jsonTmp.put("available", toWords(lAvailSpace));
           	jsonTmp.put("usable", toWords(lUsableSpace));
       		       		
           	jsonDrives.put(drive.getPath(), jsonTmp);
       	}
       	
       	return jsonDrives;
    }
 
    protected static String toWords(long aBytes)
    {
    	long _KB = 1000;
    	long _MB = 1000 * _KB;
    	long _GB = 1000 * _MB;
    	
    	StringBuffer sb = new StringBuffer();
    	
    	if(aBytes >= _GB)
    	{
    		long lGB = aBytes / _GB;
    		sb.append(lGB).append(" GB ");
    		aBytes = aBytes % _GB;
    	}
    	
    	if(aBytes >= _MB)
    	{
    		long lMB = aBytes / _MB;
    		sb.append(lMB).append(" MB ");
    		aBytes = aBytes % _MB;
    	}
    	/*
    	if(aBytes >= _KB)
    	{
    		long lKB = aBytes / _KB;
    		sb.append(lKB).append(" KB ");    		
    		aBytes = aBytes % _KB;
    	}
    	*/
    	
    	/**
		if(aBytes>0)
    		sb.append(aBytes).append(" bytes ");
    	**/
    	
    	if(sb.length()==0)
    	{
    		sb.append("0");
    	}
    	
    	return sb.toString().trim();
    }
    
    protected static List<String> execCommand(final String aCommand)
    {
    	return execCommand(aCommand.split(" "));
    }
    
    protected static List<String> execCommand(final String[] aCommand)
    {
    	try {
	    	Process p = Runtime.getRuntime().exec(aCommand);
	    	BufferedReader reader =
	                new BufferedReader(new InputStreamReader(p.getInputStream()));
	
	        List<String> listContent = new ArrayList<String>();

	        String sLine 	= null;
	        while ((sLine = reader.readLine()) != null) 
	        {
	        	listContent.add(sLine);
	        }
	        p.waitFor();
	        
	        return listContent;
    	}
        catch(Throwable t)
        {
        	System.err.println("Failed to execute '"+String.join(" ", aCommand)+"'.");
        	//t.printStackTrace();
        }
    	
    	return null;
    }
    
    public static void main(String args[]) throws Exception
    {
    	System.out.println(getCpuInfo());
    	System.out.println(getDiskInfo());
    	System.out.println(getJDKInfo());
    
       	System.out.println(getSysProperties());
       	System.out.println(getEnvProperties());
           	
    }
}
