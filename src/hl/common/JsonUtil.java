package hl.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonUtil {
	
	public static JSONObject convert(JSONObject aJSONObject, Map<String, String> aConvertMapping)
	{
		JSONObject jsonInput 	= new JSONObject(aJSONObject.toString());
		JSONObject jsonOutput 	= new JSONObject();
		
		if(aConvertMapping!=null)
		{
			Object objValue = null;
			
			for(String sKey : aConvertMapping.keySet())
			{
				//Navigate JSON value
				objValue = getJsonObj(jsonInput, sKey);
				
//System.out.println();System.out.println(sKey+"="+objValue);
				
				if(objValue!=null)
				{
					
					//Construct new structure
					String sNewAttrNames = aConvertMapping.get(sKey);
										
					StringTokenizer tkNewAttrNames = new StringTokenizer(sNewAttrNames, ",");
					
					String sNewAttrName = null;
					while(tkNewAttrNames.hasMoreTokens())
					{
						sNewAttrName = tkNewAttrNames.nextToken();
						
	//System.out.println("sNewAttrName==>"+sNewAttrName);
						Stack<String> stacks = new Stack<String>();
						StringTokenizer tk = new StringTokenizer(sNewAttrName, ".");
						while(tk.hasMoreTokens())
						{ 
							String sNewSubKey = tk.nextToken();
	//System.out.println(" - sNewSubKey="+sNewSubKey);
							stacks.push(sNewSubKey);
						}
		
						JSONObject jsonAttr = null;
						while(!stacks.isEmpty())
						{
							sNewAttrName = stacks.pop();
							
							if(jsonAttr==null)
							{
								jsonAttr = jsonOutput.optJSONObject(sNewAttrName);
								if(jsonAttr==null)
								{
									jsonAttr = new JSONObject();
								}
								jsonAttr.put(sNewAttrName, objValue);
							}
							else
							{
								JSONObject jsonTemp = jsonAttr.optJSONObject(sNewAttrName);
								if(jsonTemp==null)
								{
									jsonTemp = new JSONObject();
								}
								jsonTemp.put(sNewAttrName, jsonAttr);
								jsonAttr = jsonTemp;
							}
						}
						
						//merge
						jsonOutput = merge(jsonOutput, jsonAttr);
					}
				}
			}
		}
		
		return jsonOutput;
	}
	
	public static JSONObject merge(JSONObject json1, JSONObject json2)
	{
		if(json1==null)
			json1 = new JSONObject();
		
		if(json2==null)
			json2 = new JSONObject();
		
		if(json1.length()>json2.length())
		{
			JSONObject jsontmp = json1;
			json1 = json2;
			json2 = jsontmp;
		}
		
		for(String sKey : json2.keySet())
		{
			Object objTmp1 = json1.opt(sKey);
			Object objTmp2 = json2.opt(sKey);
			
			if(objTmp1!=null)
			{
				if(objTmp1 instanceof JSONObject)
				{
					if(objTmp2 instanceof JSONObject)
					{
						//same so merge
						JSONObject jsonM = merge((JSONObject)objTmp1, (JSONObject)objTmp2);
						json1.put(sKey, jsonM);
					}
				}
				else if(objTmp1 instanceof JSONArray)
				{
					if(objTmp2 instanceof JSONArray)
					{
						JSONArray jsonArrTmp1 = (JSONArray) objTmp1;
						JSONArray jsonArrTmp2 = (JSONArray) objTmp2;
						for(int i=0; i<jsonArrTmp2.length(); i++)
						{
							jsonArrTmp1.put(jsonArrTmp2.get(i));
						}
					}
				}
				
			}
			else if(objTmp1==null && objTmp2!=null)
			{
				json1.put(sKey, objTmp2);
			}
		}
		
		return json1;
	}
	
	public static Object getJsonObj(JSONObject aJSONObject, String aJsonPath)
	{
		//Navigate JSON value
		if(aJsonPath==null || aJsonPath.trim().length()==0)
			return null;
		
		StringTokenizer tk 	= new StringTokenizer(aJsonPath, ".");		
		String sAttrName = null;
		if(tk.countTokens()>1)
		{
			while(tk.hasMoreTokens())
			{
				sAttrName = tk.nextToken();
				Object jsonObj = aJSONObject.opt(sAttrName);
				if(jsonObj instanceof JSONObject)
				{
					aJSONObject = (JSONObject) jsonObj;
					sAttrName = null;
				}
				else
				{
					break;
				}
			}
		}
		else
		{
			sAttrName = tk.nextToken();
		}
		
		if(sAttrName!=null)
			return aJSONObject.opt(sAttrName);	
		else
			return aJSONObject;	
	}
	
	
	public static void main(String args[])
	{
	}
    
}
