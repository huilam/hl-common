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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;


public class PropUtil{
	
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
					System.err.println("Invalid value ! ["+aPropKey+"]=["+sValue+"]");
				}
			}
			
		}
		return lReturnValue;
	}
	
	public static void saveProperties(Properties aProp, String aPropFolderName, String aPropFileName) throws IOException
	{
		FileOutputStream fileOut = null;
		
		try{
			File fileProp = new File(aPropFolderName+"/"+aPropFileName);
			fileProp.delete();
			fileOut = new FileOutputStream(fileProp);
			aProp.store(fileOut, null);
		}finally
		{
			if(fileOut!=null)
				fileOut.close();
		}
	}
	
	public static Properties loadProperties(String aPropFolderName, String aPropFileName) throws IOException
	{
		return loadProperties(aPropFolderName+"/"+aPropFileName);
	}
	
	public static Properties loadProperties(String aPropFileName) throws IOException 
	{
		Properties prop = null;
		InputStream in 	= null;
		try{
			
			//To support tomcat webapp '#'
			aPropFileName = aPropFileName.replace("#", File.separator);
			
			File fileProp = new File(aPropFileName);
			if(!fileProp.isFile())
			{
				URL url = PropUtil.class.getResource("/"+aPropFileName);
				if(url!=null)
				{
					fileProp = new File(url.getPath());
				}
			}
			
				
			if(fileProp.isFile())
			{
				try {
					System.out.println("Trying to load from file ... "+fileProp.getAbsolutePath());
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
					System.out.println("Trying to load from resource ... "+"/" + aPropFileName);
					//
					is = PropUtil.class.getResourceAsStream("/" + aPropFileName);				
					prop = new Properties();
					prop.load(is);
				}catch(Exception e){
					 throw new IOException("Properties file NOT found ! - "+aPropFileName);
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
		System.out.println("Loaded properties size - "+ prop.size());
		return prop;
	}
	
	public static void main(String args[]) throws Exception
	{
	}
}
