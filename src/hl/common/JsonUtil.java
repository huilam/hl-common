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
		String sData = "{\"priorityName\":\"HIGH\",\"markDuplicatedAs\":null,\"alertTypeId\":1,\"resourceId\":9,\"thumbnailType\":\"url\",\"subjectType\":\"PERSON\",\"subjectId\":\"17\",\"alertCategoryId\":4,\"priorityId\":1,\"videoUri\":null,\"alertStatusId\":1,\"originalImageUri\":\"/scc/data/DEFAULT/MATRIX_VMS/20190506/1223/CAMERA_08/1557116623418.jpg\",\"credibility\":0.83564126,\"alertTimestamp\":1557116623418,\"alertId\":7978,\"alertStatusName\":\"OPEN\",\"lastUpdatedBy\":null,\"alertLng\":\"103.80217552185059\",\"lastUpdatedTimestamp\":1557116624598,\"sourceSystemId\":1,\"videoType\":null,\"createdTimestamp\":1557116624598,\"vmsDeviceId\":\"38c3e712-1641-42fe-b0bf-395e480d5e70\",\"resourceName\":\"CAMERA_08\",\"alertLat\":\"1.2979064848506108\",\"videoEndTimestamp\":null,\"alertCategoryName\":\"TAG_N_TRACK.DEFAULT\",\"analyticEventId\":\"e4d35067-7577-4166-b7de-0ce0479cce48\",\"alertTypeName\":\"PERSON_DETECTION\",\"thumbnailData\":\"/scc/data/DEFAULT/MATRIX_VMS/20190506/1223/CAMERA_08/1557116623418_231_1330_184_212_THUMBNAIL.jpg\",\"createdBy\":null,\"videoStartTimestamp\":null,\"sourceSystemName\":\"MATRIX_VMS\",\"detail\":{\"processingResolution\":{\"top\":0,\"left\":0,\"width\":1920,\"height\":1080},\"originalResolution\":{\"width\":1920,\"height\":1080},\"poi\":{\"elected\":{\"personFaceId\":\"17\",\"personExtReference\":\"NEC_KUANYI\",\"score\":0.83564126,\"minThreshold\":0.6,\"personId\":\"17\",\"category\":\"DEFAULT\",\"repository\":\"TAG_N_TRACK\"},\"faceRegion\":{\"top\":300,\"left\":1371,\"width\":99,\"height\":100},\"headRegion\":{\"top\":231,\"left\":1330,\"width\":184,\"height\":212},\"idmPersonMeta\":{\"idmPersonId\":\"10eb55a3-5565-42dc-b5e5-28320f6dee7b\"},\"frontalFaceScore\":0.5859375,\"faceQualityScore\":0.84173197}},\"incidentId\":3,\"thumbnailRegion\":{\"top\":231,\"left\":1330,\"width\":184,\"height\":212}}";

		sData = "{\"timestamp\":1551677822971,\"frameId\":1,\"sensorId\":1,\"feature\":\"VjAyMDAwM...\",\"picture\":\"/9j/4AAQSkZ...\",\"faceId\":-1,\"face_area_x\":175,\"face_area_y\":43,\"face_area_width\":38,\"face_area_height\":38,\"head_area_x\":168,\"head_area_y\":26,\"head_area_width\":52,\"head_area_height\":63,\"left_eye_x\":204,\"left_eye_y\":51,\"right_eye_x\":183,\"right_eye_y\":52,\"pan\":2.5689017772674560547,\"roll\":2.7263109683990478516,\"tilt\":-3.9310748577117919922,\"frontal_score\":0.60546875,\"quality\":0.88555473089218139648,\"reliability\":0.990497589111328125,\"has_feature\":true,\"error\":\"\"}";
		
		JSONObject jsonData = new JSONObject(sData);

		
		Map<String, String> mapRename = new HashMap<String, String>();
		//mapRename.put("feature","feature");
		mapRename.put("face_area_x","faceRegion.left");
		mapRename.put("face_area_y","faceRegion.top");
		mapRename.put("face_area_width","faceRegion.width");
		mapRename.put("face_area_height","faceRegion.height");
		/**
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
		**/
		mapRename.put("reliability","xxx.yyy.faceScore,xxx.facefaceScore");

		String sAttrName = "detail.processingResolution.width";
		System.out.println(sAttrName+"="+JsonUtil.getJsonObj(jsonData, sAttrName));
		
		
		JSONObject jsonConverted = JsonUtil.convert(jsonData, mapRename);
		System.out.println("converted="+jsonConverted.toString());
		
		
	}
    
}
