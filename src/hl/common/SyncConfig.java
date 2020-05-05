package hl.common;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

import hl.common.PropUtil;
import hl.common.http.HttpResp;
import hl.common.http.RestApiUtil;

public class SyncConfig {

	private static String _PROPCACHE_PREFIX 			= "_SyncConfig.";
	private static String _PROPCACHE_LASTSYNC_EPOCHTIME = _PROPCACHE_PREFIX+"lastsync.epochtime";
	private static String _PROPCACHE_LASTSYNC_URL 		= _PROPCACHE_PREFIX+"lastsync.url";
	private static String _PROPCACHE_SOURCE 			= _PROPCACHE_PREFIX+"source";
	
	private static String _RESULT_JSON 					= "result";

	private String cache_filename 			= null;
	protected Map<String, String> mapProp 	= null;
	private String endpoint_url				= null;
	private String[] endpoint_keyattrs 		= null;
	private int endpoint_timeout			= 5000;
	

	private long last_sync_timestamp 		= 0;
	
	private static Logger logger = Logger.getLogger(SyncConfig.class.getName());
	
	public SyncConfig(String aCacheFileName)
	{
		this.cache_filename = aCacheFileName;
		this.mapProp = null;
	}
	
	public void setEndpointUrl(String aEndPointUrl)
	{
		this.endpoint_url = aEndPointUrl;
	}
	
	public void setEndpointKeyAttrs(String[] aEndPointKeyAttrs)
	{
		this.endpoint_keyattrs = aEndPointKeyAttrs;
	}
	
	public void setEndpointTimeout(int aEndPointTimeout)
	{
		this.endpoint_timeout = aEndPointTimeout;
	}
	
	public void setCacheFileName(String aCacheFileName)
	{
		this.cache_filename = aCacheFileName;
	}
	
	public String getIdentifier(JSONObject jsonData)
	{
		if(jsonData==null)
		{
			return "";
		}
			
		StringBuffer sbIdentifier = new StringBuffer();
		for(String sAttr : this.endpoint_keyattrs)
		{
			if(jsonData.has(sAttr))
			{
				Object oVal = jsonData.get(sAttr);
				if(sbIdentifier.length()>0)
				{
					sbIdentifier.append(".");
				}
				sbIdentifier.append(String.valueOf(oVal));
			}
			
		}
		return sbIdentifier.toString();
	}
	
	public String getCacheFileName()
	{
		return this.cache_filename;
	}
	
	public Properties getProperties()
	{
		Map<String, String> map = getPropMap();
		if(map==null)
		{
			return null;
		}
		else
		{
			Properties p = new Properties();
			p.putAll(map); 
			return p;
		}
	}
	
	public Map<String, String> getPropMap()
	{
		if(this.mapProp==null || this.mapProp.size()==0)
		{
			this.mapProp = reloadProp();
		}
		
		if(this.mapProp==null)
		{
			this.mapProp = new TreeMap<String, String>();
		}

		return this.mapProp;
	}
	
	public String getPropValue(String aPropKey)
	{
		return getProperties().getProperty(aPropKey);
	}
	
	public Map<String, String> reloadProp()
	{
		Map<String, String> mapTemp = fetchProperties();

		if(mapTemp==null)
		{
			mapTemp = LoadFromFile();
		}
		
		return mapTemp;
	}
	
	private Map<String, String> LoadFromFile() 
	{
		if(this.cache_filename==null)
			return null;
		
		
		Properties props = null;
		
		try {
			
			props = PropUtil.loadProperties(this.cache_filename);
			if(props!=null && props.size()>0)
			{
				props.put(_PROPCACHE_SOURCE, this.cache_filename);
			}
			else
			{
				props = null;
			}
		} catch (IOException e) {
			
			logger.log(Level.SEVERE, e.getMessage(), e.getCause());
			props = null;
		}
		
		
		Map<String, String> map = null;
		if(props!=null)
		{
			map = new TreeMap<String, String>();
			for(Object oKey : props.keySet())
			{
				String sKey = oKey.toString();
				String sVal = props.getProperty(sKey);
				if(sVal==null)
					sVal = "";
				map.put(sKey, sVal);
			}
		}
		
		return map;
	}
	
	protected boolean postJson(String aEndpointURL, JSONObject aJsonData)
	{
		HttpResp resp = null;
		
		try {
			resp = RestApiUtil.httpPost(aEndpointURL, "application/json", aJsonData.toString());
		} catch (IOException e) {
			// silent
			logger.log(Level.SEVERE, e.getMessage(), e.getCause());
		}
		
		if(resp!=null && resp.getHttp_status()>=200 && resp.getHttp_status()<300)
		{
			return true;
		}
		
		logger.log(Level.WARNING, "[Warning] "+SyncConfig.class.getName()+".postJson() : Response from "+aEndpointURL+"\n"+resp);
		return false;
	}
	
	
	public long getLastSyncTimestamp()
	{
		return this.last_sync_timestamp;
	}
	
	private Map<String, String> fetchProperties() 
	{
		if(this.endpoint_url==null)
			return null;
		
		Map<String, String> map = null;
		HttpResp resp = null;
		
		try {
			resp = RestApiUtil.httpGet(this.endpoint_url, this.endpoint_timeout);
		} catch (IOException e) {
			// silent
		}
		
		if(resp!=null && resp.getHttp_status()>=200 && resp.getHttp_status()<300)
		{
			String sData = resp.getContent_data();
			if(sData!=null)
			{
				sData = sData.trim();
				if(sData.startsWith("{") && sData.endsWith("}")) 
				{
					map = new TreeMap<String, String>();
					JSONObject jsonResult = new JSONObject(sData);
					
					JSONArray jArr = new JSONArray();
					
					if(jsonResult.has(_RESULT_JSON))
					{
						jArr = jsonResult.getJSONArray(_RESULT_JSON);
					}
					else
					{
						jArr.put(jsonResult);
					}
					
					for(int i=0; i<jArr.length(); i++)
					{
						jsonResult = jArr.getJSONObject(i);
						for(String sKey : jsonResult.keySet())
						{
							Object oVal = jsonResult.get(sKey);
							if(oVal==null || oVal==JSONObject.NULL)
								oVal = "";
							map.put(getIdentifier(jsonResult)+"."+sKey, String.valueOf(oVal));
						}
					}
					if(map.size()==0)
					{
						map = null;
					}
					else
					{
						this.last_sync_timestamp = System.currentTimeMillis();
						map.put(_PROPCACHE_LASTSYNC_URL, this.endpoint_url);
						map.put(_PROPCACHE_LASTSYNC_EPOCHTIME, String.valueOf(this.last_sync_timestamp));
						
						try {
							
							if(this.cache_filename!=null)
							{
								Properties p = new Properties();
								p.putAll(map);
								PropUtil.saveProperties(p, new File(this.cache_filename));
							}
						} catch (IOException e) {
							logger.log(Level.SEVERE, e.getMessage(), e.getCause());
						}
						
						map.put(_PROPCACHE_SOURCE, this.endpoint_url);
						
						//
						Map<String, String> mapRef = new TreeMap<String, String>();
						mapRef.putAll(map);
						EVENT_configReloaded(this.endpoint_url, this.last_sync_timestamp, mapRef);
						//
						
					}
				}
			}
		}
		else
		{
			logger.log(Level.WARNING, "[Warning] "+SyncConfig.class.getName()+".fetchProperties() : Response from "+this.endpoint_url+"\n"+resp);
		}
		
		return map;
	}
	
	public void EVENT_configReloaded(String aURL, long aLastSyncTime, Map<String, String> mapConfig)
	{
	}
}
