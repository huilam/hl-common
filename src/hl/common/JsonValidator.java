package hl.common;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonValidator {

	public boolean isDebugMode = true;
	
	public static String VALIDATION_NUM_ONLY 		= "<NUM-ONLY>";
	public static String VALIDATION_NOT_NULL 		= "<NOT-NULL>";
	public static String VALIDATION_ANALYTIC_SCORE 	= "<0.1-1.0>";
	
	public static String VALIDATION_LOOKUP_PREFIX	= "<LOOKUP:[";
	public static String VALIDATION_REGEX_PREFIX	= "<REGEX:[";
	public static String VALIDATION_RESTGET_PREFIX	= "<RESTAPI-GET:[";
	
	//Do the caching later
	//private Map<String,Pattern> mapPatt = null;
	private Map<String,List<String>> mapLookup = null;
	
	//public static JsonValidator instance = null;
	private Map<String, Map<String,String>> mapAlertType = null;
	private Map<String, String> mapAlertTypeErrMsg = null;
	
	
	public JsonValidator()
	{
		mapAlertType 		= new HashMap<String, Map<String,String>>();
		mapLookup 			= new HashMap<String,List<String>>();
		mapAlertTypeErrMsg 	= new HashMap<String,String>();
	}
	
	public void setIsDebugMode(boolean isDebug)
	{
		isDebugMode = isDebug;
	}
	
	public boolean getIsDebugMode()
	{
		return isDebugMode;
	}
	
	public void addErrMessage(String aAlertType, String aJsonAttrName, String aErrMsg)
	{
		log("addErrMessage() - type:["+aAlertType+"], attr:["+aJsonAttrName+"] err:["+aErrMsg+"]");
		mapAlertTypeErrMsg.put(aAlertType+"."+aJsonAttrName, aErrMsg);
	}
	
	public String getErrMessage(String aAlertType, String aJsonAttrName)
	{
		return mapAlertTypeErrMsg.get(aAlertType+"."+aJsonAttrName);
	}
	
	
	public void addValidation(String aAlertType, String aJsonAttrName, String aValidation)
	{
		log("addValidation() - type:["+aAlertType+"], attr:["+aJsonAttrName+"] rule:["+aValidation+"]");
		Map<String,String> mapAlertValidation = mapAlertType.get(aAlertType);
		if(mapAlertValidation==null)
		{
			mapAlertValidation = new HashMap<String,String>();
		}
		mapAlertValidation.put(aJsonAttrName, aValidation);
		mapAlertType.put(aAlertType, mapAlertValidation);
	}
	
	public void addAllValidations(String aAlertType, Map<String, String> mapAttrValidation)
	{
		Iterator<String> iter = mapAttrValidation.keySet().iterator();
		
		while(iter.hasNext())
		{
			String sAttrName 	= iter.next();
			String sValidation = mapAttrValidation.get(sAttrName);
			addValidation(aAlertType, sAttrName, sValidation);
		}
		
	}
	
	public void addLookup(String aLookupKey, String[] listLookupValues)
	{
		List<String> listTemp = new ArrayList<String>();
		for(String listVal : listLookupValues)
		{
			listTemp.add(listVal);
		}
		addLookup(aLookupKey, listTemp);
	}
	
	public void addLookup(String aLookupKey, List<String> listLookupValues)
	{
		List<String> listLookup = mapLookup.get(aLookupKey);
		if(listLookup==null)
		{
			listLookup = new ArrayList<String>();
		}
		listLookup.addAll(listLookupValues);
		mapLookup.put(aLookupKey, listLookup);
	}
	
	public void clear()
	{
		mapAlertType.clear();
		mapLookup.clear();
	}
	
	public Map<String, String> validateJsonData(String aAlertType,final String aJsonString, boolean isStopOnError)
	{
		return validateJsonData(aAlertType,new JSONObject(aJsonString), isStopOnError);
	}
	
	public Map<String, String> validateJsonData(String aAlertType,final JSONObject aJson, boolean isStopOnError)
	{
		Map<String, String> mapErrors = new HashMap<String, String>();
		
		Map<String,String> mapAlertValidation = mapAlertType.get(aAlertType);
		if(mapAlertValidation!=null)
		{
			for(String sValidateKey : mapAlertValidation.keySet())
			{
				JSONObject json 		= aJson;
				String sJsonAttrName 	= null;
				int iPos = sValidateKey.lastIndexOf(".");
				if(iPos>-1)
				{
					String sJsonNavPath = sValidateKey.substring(0, iPos);
					StringTokenizer tk = new StringTokenizer(sJsonNavPath,".");
					log("Navigate JSON ... ", false);
					
					boolean isOptional = false;
					String sCurPath = null;
							
					for(int i=0; i<=tk.countTokens(); i++)
					{
						sCurPath = tk.nextToken();
						
						if(sCurPath.endsWith("?"))
						{
							isOptional = true;
							sCurPath = sCurPath.substring(0, sCurPath.length()-1);
						}
						
						try{
							log(" "+sCurPath, false);
							json = json.getJSONObject(sCurPath);
						}
						catch(JSONException ex)
						{
							if(isOptional)
							{
								log(" - [skip] Missing optional attribute "+sCurPath);
								continue;
							}
							mapErrors.put(sValidateKey, "JSON path NOT found ! - "+sCurPath);
							log(" - JSON path NOT found !");
							log("");
							if(isStopOnError)
								return mapErrors;
							else
								continue;
						}
					}
					
					sJsonAttrName = sValidateKey.substring(iPos+1);
				}
				else
				{
					sJsonAttrName = sValidateKey;
				}
				
				boolean isOptional = false;
				if(sJsonAttrName.endsWith("?"))
				{
					isOptional = true;
					sJsonAttrName = sJsonAttrName.substring(0, sJsonAttrName.length()-1);
				}
				
				log(" ["+sJsonAttrName+"]", false);
				log("");
				log(" - Test String="+json.toString());
				String sValRule = mapAlertValidation.get(sValidateKey);
				String sValue 	= null;
				boolean isOK 	= false; 	
				try{
					sValue 	= json.get(sJsonAttrName).toString();
					isOK = validate(sValRule, sValue);
				}catch(JSONException ex)
				{
					if(isOptional)
					{
						log(" - [skip] Missing optional attribute "+sJsonAttrName);
						continue;
					}
					mapErrors.put(sValidateKey, "JSON path NOT found ! - "+sJsonAttrName);
					log(" - JSON path NOT found !");
					log("");
					if(isStopOnError)
						return mapErrors;
					else
						continue;
				}
				log(" - validate("+sValRule+","+sValue+")="+isOK);
				log("");
				if(!isOK)
				{
					String sErrMsg = getErrMessage(aAlertType, sValidateKey);
					if(sErrMsg==null)
					{
						sErrMsg = "Invalid Value !";
					}
					mapErrors.put(sValidateKey, sErrMsg+" - "+sValue);
					if(isStopOnError)
						return mapErrors;
				}
			}
		}
		return mapErrors;
	}
	
	public boolean validate(String aValidateRule, String aTestValue)
	{
		if(aValidateRule==null || aTestValue==null)
			return false;
		
		if(VALIDATION_ANALYTIC_SCORE.equalsIgnoreCase(aValidateRule))
		{
			try{
				float f = Float.parseFloat(aTestValue);
				return (f>=0.1 && f<=1.0);
			}catch(NumberFormatException ex)
			{
				log("[ERROR] "+ex.toString());
				return false;
			}
		}
		else if(VALIDATION_NUM_ONLY.equalsIgnoreCase(aValidateRule))
		{
			try{
				Long.parseLong(aTestValue);
				return true;
			}catch(NumberFormatException ex)
			{
				log("[ERROR] "+ex.toString());
				return false;
			}
		}
		else if(VALIDATION_NOT_NULL.equalsIgnoreCase(aValidateRule))
		{
			return (aTestValue.trim().length()>0);
		}
		else if(aValidateRule.startsWith(VALIDATION_REGEX_PREFIX))
		{
			int iStartPos 	= VALIDATION_REGEX_PREFIX.length();
			int iEndPos 	= aValidateRule.length()-2;
			String sRegEx 	= aValidateRule.substring(iStartPos,iEndPos);
			Pattern pat 	= Pattern.compile(sRegEx);
			Matcher m 		= pat.matcher(aTestValue);
			return m.find();
		}
		
		else if(aValidateRule.startsWith(VALIDATION_LOOKUP_PREFIX))
		{
			int iStartPos 	= VALIDATION_LOOKUP_PREFIX.length();
			int iEndPos 	= aValidateRule.length()-2;
			String sLookupKey = aValidateRule.substring(iStartPos,iEndPos);
			List<String> listLookup = mapLookup.get(sLookupKey);
			if(listLookup!=null)
			{
				return listLookup.contains(aTestValue);
			}
			else
			{
				if(sLookupKey.indexOf(",")>-1)
				{
					StringTokenizer tk = new StringTokenizer(sLookupKey,",");
					while(tk.hasMoreTokens())
					{
						String sToken = tk.nextToken();
						if(sToken.equalsIgnoreCase(aTestValue))
							return true;
					}
				}
				
				throw new RuntimeException("Invalid lookup name : "+sLookupKey);
			}
		}
		
		else if(aValidateRule.startsWith(VALIDATION_RESTGET_PREFIX))
		{
			int iStartPos 	= VALIDATION_RESTGET_PREFIX.length();
			int iEndPos 	= aValidateRule.length()-2;
			String sURL = aValidateRule.substring(iStartPos,iEndPos);
			boolean isOK;
			try {
				isOK = doGetURL(sURL+aTestValue);
			} catch (IOException e) {
				e.printStackTrace();
				isOK = false;
			}
			
			return isOK;
		}
		
		return false;
	}
	
	
	private boolean doGetURL(String aURL) throws IOException
	{
		URL url = new URL(aURL);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setRequestMethod("GET");
		conn.connect();
		boolean isOK =  conn.getResponseCode()==HttpURLConnection.HTTP_OK;
		if(!isOK)
			return false;
		
		//Check for empty json object or array
		return conn.getContentLength()>2;  
	}
	
	private void log(String aMessage)
	{
		log(aMessage, true);
	}
	private void log(String aMessage, boolean isNewLine)
	{
		if(getIsDebugMode())
		{
			if(isNewLine)
				System.out.println(aMessage);
			else
				System.out.print(aMessage);
		}
	}
	
	public static void main(String args[])
	{
		JsonValidator v = new JsonValidator();
		//
		v.addLookup("POILIST", new String[]{"whitelist","blacklist"});
		//
	//	v.addValidation("poi", "poi.invalid", "<NOT-NULL>");
	//	v.addValidation("poi", "poi.elected.score", "<0.1-1.0>");
	//	v.addValidation("poi", "processingResolution.left", "<NUM-ONLY>");
		v.addValidation("poi", "processingResolution.left_2?", "<NUM-ONLY>");
	//	v.addValidation("poi", "poi.elected.list", "<LOOKUP:[POILIST]>");
	//	v.addValidation("poi", "originalResolution.height", "<REGEX:[[0-9]+]>");
		//v.addValidation("poi.detail", "poi.elected.personId", "<RESTAPI-GET:[http://203.127.252.53/commonservice/v1/persons/]>");
		//
		String sTest = "{\"processingResolution\":{\"top\":100,\"left_2\":a100,\"left\":100,\"height\":200,\"width\":200},\"originalResolution\":{\"height\":200,\"width\":200},\"poi\":{\"faceQualityScore\":0.7225696,\"frontalFaceScore\":0.7225696,\"faceRegion\":{\"top\":100,\"left\":100,\"height\":200,\"width\":200},\"elected\":{\"personId\":\"1224\",\"score\":0.787264,\"list\":\"blacklist\",\"category\":\"high\"},\"candidates\":[{\"personId\":\"1224\",\"score\":0.787264,\"list\":\"blacklist\",\"category\":\"high\"},{\"personId\":\"12247\",\"score\":0.7872624,\"list\":\"blacklist\",\"category\":\"high\"}]}}";

		Map<String, String> mapErrs = v.validateJsonData("poi", sTest, false);
		
		Iterator<String> iter = mapErrs.keySet().iterator();
		while(iter.hasNext())
		{
			String sKey = iter.next();
			System.out.println(sKey+" - "+mapErrs.get(sKey));
		}
	}
	
}
