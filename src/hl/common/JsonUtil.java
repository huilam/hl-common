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
				if(sKey.trim().length()==0)
					continue;
				
				StringTokenizer tk 		= new StringTokenizer(sKey, ".");
				
				if(tk.countTokens()>1)
				{
					for(int i=0; i<tk.countTokens(); i++)
					{
						JSONObject jsonTemp = jsonInput.optJSONObject(tk.nextToken());
						if(jsonTemp!=null)
						{
							jsonInput = jsonTemp;
						}
					}
				}
				
				String sAttrName = tk.nextToken();
				objValue = jsonInput.opt(sAttrName);
				
//System.out.println();System.out.println(sKey+"="+objValue);
				
				if(objValue!=null)
				{
					//Construct new structure
					String sNewAttrName = aConvertMapping.get(sKey);
										
//System.out.println("sNewAttrName==>"+sNewAttrName);
					Stack<String> stacks = new Stack<String>();
					tk = new StringTokenizer(sNewAttrName, ".");
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
						objTmp1 = jsonM;
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

	public static void main(String args[])
	{
		JSONObject jsonInner2 = new JSONObject();
		jsonInner2.put("inner2","aaaa");
		
		JSONObject jsonInner1 = new JSONObject();
		jsonInner1.put("test1","111");
		jsonInner1.put("test2","222");
		jsonInner1.put("innerjson",jsonInner2);
		
		JSONObject json = new JSONObject();
		json.put("timestamp","1551677822971");
		json.put("frameId",1);
		json.put("sensorId",1);
		json.put("feature","VjAyMDAwM");
		json.put("picture","/9j/4AAQSk");
		json.put("faceId",-1);
		json.put("face_area_x",168);
		json.put("face_area_y",26);
		json.put("face_area_width",52);
		json.put("face_area_height",-63);
		json.put("json", jsonInner1);
		
		
		Map<String, String> mapRename = new HashMap<String, String>();
		mapRename.put("feature", "feature");
		mapRename.put("picture", "thumbnail");
		mapRename.put("face_area_x", "face.x");
		mapRename.put("face_area_y", "face.y");
		mapRename.put("face_area_width", "face.width");
		mapRename.put("face_area_height", "face.height");
		mapRename.put("json.innerjson.inner2", "test");
		
		System.out.println("converted="+JsonUtil.convert(json, mapRename));
	}
    
}
