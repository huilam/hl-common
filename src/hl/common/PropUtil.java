/*
 Copyright (c) 2017 onghuilam@gmail.com
 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.
 The Software shall be used for Good, not Evil.
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 
 */

package hl.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PropUtil{
	
	private static Pattern pattEnvParam = Pattern.compile("(\\$\\{env\\:(.*?)\\})");
	private static Pattern pattFileParam = Pattern.compile("(\\$\\{file\\:(.*?)\\})");

	private static Logger logger = Logger.getLogger(PropUtil.class.getName());
	
	public static String getValue(Properties aProp, String aPropKey, String aDefaultValue)
	{
		String sReturnValue = aDefaultValue;
		if(aProp!=null && aPropKey!=null)
		{
			String sValue = aProp.getProperty(aPropKey);
			if(sValue!=null)
				sReturnValue = sValue;
		}
		return sReturnValue;
	}

	public static boolean getValueAsBoolean(Properties aProp, String aPropKey, boolean bDefaultValue)
	{
		boolean bReturnValue = bDefaultValue;
		if(aProp!=null && aPropKey!=null)
		{
			String sValue = aProp.getProperty(aPropKey);
			if(sValue!=null)
			{
				sValue = sValue.toLowerCase();
				if("true".equalsIgnoreCase(sValue))
				{
					bReturnValue = true;
				}
				else if("false".equalsIgnoreCase(sValue))
				{
					bReturnValue = false;
				}
			}
		}
		return bReturnValue;
	}
	
	public static long getValueAsLong(Properties aProp, String aPropKey, long aDefaultValue)
	{
		long lReturnValue = aDefaultValue;
		if(aProp!=null)
		{
			String sValue = getValue(aProp, aPropKey, null);
			if(sValue!=null)
			{
				try{
					lReturnValue = Long.parseLong(sValue);
				}catch(NumberFormatException ex)
				{
					logger.log(Level.SEVERE, "Invalid value ! ["+aPropKey+"]=["+sValue+"]", ex);
				}
			}
			
		}
		return lReturnValue;
	}
	
	public static void saveProperties(Properties aProp, String aPropFolderName, String aPropFileName) throws IOException
	{
		File fileProp = new File(aPropFolderName+File.separator+aPropFileName);
		saveProperties(aProp, fileProp);
	}
	
	public static void saveProperties(Properties aProp, File aPropFileName) throws IOException
	{
		BufferedWriter wrt = null;
		
		if(aPropFileName==null)
			return;
		
		try{
			aPropFileName.delete();
			wrt = new BufferedWriter(new FileWriter(aPropFileName));
			
			SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.ms");
			String sCurDateTime = df.format(new Date(System.currentTimeMillis()));
			
			wrt.write("# "+sCurDateTime);
			wrt.newLine();

			Map<String, String> map = new TreeMap<String, String>((Map) aProp);
			for(String sKey : map.keySet())
			{
				String sVal = map.get(sKey);
				if(sVal==null)
					sVal = "";
				
				wrt.write(sKey+"="+sVal);
				wrt.newLine();
			}
			
			wrt.flush();
		}finally
		{
			if(wrt!=null)
				wrt.close();
		}
	}
	
	public static Properties loadProperties(String aPropFolderName, String aPropFileName) throws IOException
	{
		return loadProperties(aPropFolderName+"/"+aPropFileName);
	}
	
	public static Properties loadProperties(String aPropFileName) throws IOException 
	{
		return loadProperties(PropUtil.class, aPropFileName);
	}
	
	public static Properties loadProperties(Class<?> aClass, String aPropFileName) throws IOException 
	{
		Properties prop = null;
		InputStream in 	= null;
		try{
			
			//To support tomcat webapp '#'
			aPropFileName = aPropFileName.replace("#", File.separator);
			
			File fileProp = new File(aPropFileName);
			if(aClass!=PropUtil.class)
			{
				URL url = aClass.getResource("/"+aPropFileName);
				if(url!=null)
				{
					File fileTmp = new File(url.getPath());
					if(fileTmp.isFile())
						fileProp = fileTmp;
				}
			}
			
				
			if(fileProp.isFile())
			{
				try {
					logger.log(Level.FINEST, "Trying to load from file ... "+fileProp.getAbsolutePath());
					//
					in = new FileInputStream(fileProp);
					prop = new Properties();
					prop.load(in);
				} catch (Exception e) {
					throw new IOException("Properties file NOT found ! - "+fileProp.getAbsolutePath());
				}
			}
			else
			{
				InputStream is = null;
				try{
					logger.log(Level.FINEST, "Trying to load from resource ... "+aPropFileName);
					//
					try{
						is = aClass.getResourceAsStream(aPropFileName);	
					}catch(Exception e){
					}
					
					if(is==null)
						is = aClass.getResourceAsStream("/"+aPropFileName);
					
					prop = new Properties();
					prop.load(is);
				}catch(Exception e){
					 throw new IOException("Properties file NOT found ! - "+aPropFileName+"  aClass:"+aClass.getName());
				}
				finally
				{
					if(is!=null)
						is.close();
				}
			}
		}
		finally
		{
			if(in!=null)
				in.close();
		}
		logger.log(Level.FINEST, "Loaded properties size - "+ prop.size());
		
		
		if(prop!=null && prop.size()>0)
		{
			prop = replacePropVar(new Class[]{aClass, PropUtil.class}, prop);
		}
		
		return prop;
	}
	
	private static Properties replacePropVar(Class[] aClasses, Properties aProps)
	{
		int iVarCount = 0;
		for(Entry<Object, Object> e : aProps.entrySet())
		{
			String sPropVal = (String) e.getValue();
			
			//System Env
			Matcher m = pattEnvParam.matcher(sPropVal);
			while(m.find())
			{
				iVarCount++;
				String sEnvParamName 	= m.group(2);
				String sEnvParamValue 	= getEnvParamValue(sEnvParamName);
				if(sEnvParamValue!=null)
				{
					iVarCount--;
					sPropVal = sPropVal.replace(m.group(1), sEnvParamValue);
				}
			}
			
			// File Content
			m = pattFileParam.matcher(sPropVal);
			while(m.find())
			{
				iVarCount++;
				String sFileName 	= m.group(2);
				String sFileContent = loadFileContentAsText(aClasses, sFileName);
				if(sFileContent!=null)
				{
					iVarCount--;
					sPropVal = sPropVal.replace(m.group(1), sFileContent);
				}
			}
			
			//
			e.setValue(sPropVal);
		}
		//
		if(iVarCount>0)
		{
			System.err.println("unReplaced-Vars="+iVarCount);
		}
		//
		return aProps;
	}
	
	private static String loadFileContentAsText(Class[] aClasses, String aFilePath)
	{
		String sContent = null;
		
		for(Class aClass : aClasses)
		{
			sContent = FileUtil.loadContent(aClass, aFilePath);
			if(sContent!=null && sContent.trim().length()>0)
				break;
		}
		return sContent;
	}
	
	private static String getEnvParamValue(String aSysParamName)
	{
		String sSysParamVal = System.getProperty(aSysParamName);
		if(sSysParamVal==null)
		{
			sSysParamVal = System.getenv(aSysParamName);
		}
		return sSysParamVal;
	}
	
	public static void main(String args[]) throws Exception
	{
	}
}
