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
					String sNewAttrName = aConvertMapping.get(sKey);
										
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
		
		return aJSONObject.opt(sAttrName);		
	}
	
	
	public static void main(String args[])
	{
		String sData = "{\"timestamp\": 1557109653046," + 
				"\"frameId\": 1," + 
				"\"sensorId\": 1," + 
				"\"feature\": \"VjAyMDAwMCAKa8cjIP…\"," + 
				"\"picture\": \"/9j/4AAQSkZJRgA…\"," + 
				"\"faceId\": -1," + 
				"\"face_area_x\": 158," + 
				"\"face_area_y\": 290," + 
				"\"face_area_width\": 278," + 
				"\"face_area_height\": 278," + 
				"\"head_area_x\": 108," + 
				"\"head_area_y\": 161," + 
				"\"head_area_width\": 375," + 
				"\"head_area_height\": 461," + 
				"\"left_eye_x\": 369," + 
				"\"left_eye_y\": 351," + 
				"\"right_eye_x\": 219," + 
				"\"right_eye_y\": 356," + 
				"\"pan\": -4.8475122451782226562," + 
				"\"roll\": 1.9091522693634033203," + 
				"\"tilt\": 1.3301063776016235352," + 
				"\"frontal_score\": 0.60400390625," + 
				"\"quality\": 0.7776355743408203125," + 
				"\"reliability\": 0.99921792745590209961," + 
				"\"has_feature\": true," + 
				"\"error\": \"\"" + 
				"}";
		JSONObject jsonData = new JSONObject(sData);

		Map<String, String> mapRename = new HashMap<String, String>();
		//mapRename.put("feature","feature");
		mapRename.put("face_area_x","faceRegion.left");
		mapRename.put("face_area_y","faceRegion.top");
		mapRename.put("face_area_width","faceRegion.width");
		mapRename.put("face_area_height","faceRegion.height");
		
		mapRename.put("head_area_x","headRegion.left");
		mapRename.put("head_area_y","headRegion.top");
		mapRename.put("head_area_width","headRegion.width");
		mapRename.put("head_area_height","headRegion.height");
		mapRename.put("left_eye_x","leftEye.left");
		mapRename.put("left_eye_y","leftEye.top");
		mapRename.put("right_eye_x","rightEye.left");
		mapRename.put("right_eye_y","rightEye.top");
		mapRename.put("pan","facePan");
		mapRename.put("roll","faceRoll");
		mapRename.put("tilt","faceTilt");
		mapRename.put("frontal_score","frontalFaceScore");
		mapRename.put("quality","faceQualityScore");
		mapRename.put("reliability","faceScore");
		
		JSONObject jsonConverted = JsonUtil.convert(jsonData, mapRename);
		System.out.println("converted="+jsonConverted.toString());
		
		System.out.println("value="+JsonUtil.getJsonObj(jsonConverted, "headRegion.height"));
	}
    
}
