package hl.common.system;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;


public class Windows {

	private static Map<String, List<String>> mapSysInfo = null;
	private static final String[] OS_SYSINFO_CMD = new String[] {"systeminfo"};
	
	static {
		
		try {
			mapSysInfo = parseSystemInfo();
		} catch (Throwable t) {
		}
	}
	
    private static Map<String, List<String>> parseSystemInfo()
    {
    	if(mapSysInfo!=null)
    		return mapSysInfo;
    	
    	mapSysInfo = new LinkedHashMap<String, List<String>>();
    	
    	List<String> listCmd = CommonInfo.execCommand(OS_SYSINFO_CMD);
    	
    	if(listCmd.size()>0)
    	{
	        List<String> listValues = new ArrayList<String>();
	        String sPrevKey = null;
	        for (String sLine : listCmd) 
	        {
	        	if(sLine.startsWith(" "))
	        	{
	        		listValues.add(sLine.trim());
	        		continue;
	        	}
	        	
	        	int iPos = sLine.indexOf(":");
	        	if(iPos>-1)
	        	{
	        		String sKey = sLine.substring(0, iPos);
	        		String sVal = sLine.substring(iPos+1);
	        		
	        		if(listValues.size()>0)
	        		{
	        			List<String> listPrevVals = mapSysInfo.get(sPrevKey);
	        			if(listPrevVals!=null)
	        			{
	        				listPrevVals.addAll(listValues);
	        			}
	        			mapSysInfo.put(sPrevKey, listPrevVals);
	        			listValues.clear();
	        		}
	        		
	        		if(sVal!=null && sVal.trim().length()>0)
		        	{
		        		List<String> list = new ArrayList<String>();
		        		list.add(sVal.trim());
		        		mapSysInfo.put(sKey.trim(), list);
	        		}
	        		
	        		sPrevKey = sKey;
	        	}
	        }
        }
    	return mapSysInfo;
    }

    public static JSONObject getSystemInfo()
    {   
    	if(mapSysInfo.size()==0)
    	{
    		return null;
    	}
    	
    	JSONObject jsonAll = new JSONObject();
    	JSONObject jsonTmp = new JSONObject();
    	String sOSName 		= getSysInfoMapVal("OS Name", 0);
    	String sOSVer 		= getSysInfoMapVal("OS Version", 0);
       	String sTimeZone 	= getSysInfoMapVal("getSysInfoMapVal", 0);
       	String sLastHotfix 	= getSysInfoMapVal("Hotfix(s)", 1000);
       	
       	jsonTmp.put("os.name", sOSName);
       	jsonTmp.put("os.version", sOSVer);
       	jsonTmp.put("os.timezone", sTimeZone);
       	jsonTmp.put("os.last.hotfix", sLastHotfix);
       	
       	jsonAll.put("os", jsonTmp);
       	
    	String sTotalRAM 	= getSysInfoMapVal("Total Physical Memory", 0);
       	String sAvailRAM 	= getSysInfoMapVal("Available Physical Memory", 0);
       	jsonTmp = new JSONObject();
       	jsonTmp.put("memory.total", sTotalRAM);
       	jsonTmp.put("memory.available", sAvailRAM);
       	jsonAll.put("memory", jsonTmp);
       	
    	return jsonAll;
    }
    
    
    private static String getSysInfoMapVal(String aKey, int aValIndex)
    {
    	if(aValIndex<0)
    		aValIndex = 0;
    	
    	List<String> listVal = mapSysInfo.get(aKey);
    	if(listVal==null)
    		return null;
    	
    	if(aValIndex>listVal.size())
    		aValIndex = listVal.size()-1;
    	
    	return listVal.get(aValIndex);
    }
    
    
    public static void main(String args[]) throws Exception
    {
    	System.out.println(getSystemInfo());
    	
    }
}
