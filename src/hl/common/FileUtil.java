package hl.common;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

public class FileUtil {

	private static Logger logger = Logger.getLogger(FileUtil.class.getName());
	private static final int BUFFER_SIZE = 32768;
	private static final String DEF_REMARKS = "#";
	
	
	public static File getJavaClassPath(Class<?> aClass)
	{
		File folder = null;
		if(aClass!=null)
		{
			ProtectionDomain pd = aClass.getClass().getProtectionDomain();
			if(pd!=null)
			{
				CodeSource cs = pd.getCodeSource();
				if(cs!=null)
				{
					folder = new File(cs.getLocation().getFile()).getParentFile();
				}
			}
		}
		
		if(folder==null)
		{
			URL url = aClass.getResource(".");
			if(url!=null)
			{
				String sPath = url.getFile();
				
				String sPackageName = aClass.getPackage().getName();
				if(sPackageName!=null)
				{
					sPackageName = "/"+sPackageName.replaceAll("\\.", "/")+"/";
				}
				
				if(sPath.endsWith(sPackageName))
				{
					int iLen = sPath.length() - sPackageName.length(); 
					folder = new File(sPath.substring(0, iLen));
				}
				else
				{
					folder = new File(sPath);
				}
			}
		}
		
		return folder;
	}
	
	public static String loadContent(String aResourcePath)
	{
		return loadContent(getCallerClass(), aResourcePath);
	}
	
	public static String loadContent(Class<?> aCallerClass, String aResourcePath)
	{
		return loadContent(aCallerClass, aResourcePath, DEF_REMARKS);
	}
	
	public static String loadContent(Class<?> aCallerClass, String aResourcePath, String aRemarks)
	{
		String sData = null;
		if(aResourcePath!=null)
		{
			File f = new File(aResourcePath);
			if(f.exists())
			{
				try {
					sData = getContent(new FileReader(f), aRemarks);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			
			if(sData==null)
			{
				//resource bundle				
				if(!aResourcePath.startsWith("/"))
				{
					aResourcePath = "/"+aResourcePath;
				}

				InputStream in = null;
				try {
					
					if(aCallerClass==null)
						aCallerClass = getCallerClass();
					
					in = aCallerClass.getResourceAsStream(aResourcePath);
					if(in!=null)
					{
						sData = getContent(new InputStreamReader(in), aRemarks);
					}
					else
					{
						// Attempt to load from caller class package
						String sResPath = "/"+aCallerClass.getPackageName().replace('.','/')+aResourcePath;
						
						in = aCallerClass.getResourceAsStream(sResPath);
						if(in!=null)
						{
							sData = getContent(new InputStreamReader(in), aRemarks);
						}
					}
					
				}
				finally
				{
					if(in!=null)
						try {
							in.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
			}
		}
		return sData;
	}
	
	private static String getContent(Reader aReader, String aRemarks) 
	{
		String sData = null;
		
		boolean isRemoveRemark = (aRemarks!=null && aRemarks.trim().length()>0);
		
		if(aReader!=null)
		{
			StringBuffer sb = new StringBuffer();
			BufferedReader rdr = null;
			try {
				rdr = new BufferedReader(aReader);
				String sLine = null;
				
				while((sLine = rdr.readLine())!=null)
				{
					if(sb.length()>0)
					{
						sb.append("\n");
					}
					
					if(isRemoveRemark)
					{
						int iRemarkPos = sLine.indexOf(aRemarks);
						if(iRemarkPos>-1)
						{
							if(iRemarkPos==0) //whole line is remarks
								continue;
							
							sLine = sLine.substring(0, iRemarkPos);
							
							if(sLine.trim().length()==0)
							{
								continue;
							}
						}
					}
					
					sb.append(sLine.trim());
				}
				
				if(sb.length()>0)
				{
					sData = sb.toString();
				}
			}catch(IOException ex)
			{
				ex.printStackTrace();
			}
		}
		return sData;
	}
	
	public static File downloadWebImage(String aImageURL) throws IOException
	{
		File tempFile = File.createTempFile("temp", "");
		tempFile.delete();
		return downloadWebImage(aImageURL, tempFile.getPath());
	}
		
	public static File downloadWebImage(String aImageURL, String aLocalTargetFileName) throws IOException
	{
		URL url = new URL(aImageURL);
		InputStream in = null;
		try{
			
			HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
			httpConn.setRequestMethod("GET");
			
			if(httpConn.getResponseCode()==200)
			{
				inputStreamToFile(httpConn.getInputStream(), aLocalTargetFileName);
			}
			else
			{
				throw new IOException("[download failed] "+aImageURL);
			}
		}
		finally
		{
			if(in!=null)
				in.close();
		}
		File fileImage = new File(aLocalTargetFileName);
		
		if(fileImage.isFile())
			return fileImage;
		else
			return null;
	}
	
    public static void inputStreamToFile(
    		InputStream aInputStream, String aOutputFileName) throws IOException {
    	
        BufferedOutputStream bos = null;
        try{
	        bos = new BufferedOutputStream(new FileOutputStream(aOutputFileName));
	        byte[] bytesIn = new byte[BUFFER_SIZE];
	        int read = 0;
	        while ((read = aInputStream.read(bytesIn)) != -1) {
	            bos.write(bytesIn, 0, read);
	        }
        }finally
        {
	        if(bos!=null)
	        	bos.close();
        }
    }
    
	public static List<String[]> loadCSV(File aCSVFile) throws IOException
	{
		List<String[]> list = new ArrayList<String[]>();
		
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(aCSVFile));
			
			String sLine = null;
			
			while((sLine = rdr.readLine())!=null)
			{
				Vector<String> vStr = new Vector<String>();
				
				StringBuffer sb = new StringBuffer();
				boolean isQuoted = false;
				for(int i=0; i<sLine.length(); i++)
				{
					if(i==0)
					{
						isQuoted = sLine.charAt(0)=='\"';
						if(isQuoted)
							continue;
					}
					
					if(isQuoted)
					{
						if(sLine.charAt(i)=='\"')
						{
							if(sLine.length()>i && sLine.charAt(i+1)=='\"')
							{
								//Escape
								i++;
								sb.append(sLine.charAt(i));
								continue;
							}
							
							isQuoted = false;
							continue;
						}
						else
						{
							sb.append(sLine.charAt(i));
						}
					}
					else
					{
						if(sLine.charAt(i)==',')
						{
							vStr.add(sb.toString());
							sb.setLength(0);
							continue;
						}
						else
						{
							sb.append(sLine.charAt(i));
						}
					}
					
				}
				if(sb.length()>0)
				{
					vStr.add(sb.toString());
				}
				
				list.add(vStr.toArray(new String[vStr.size()]));
			}
		}
		finally
		{
			try {
				if(rdr!=null);
				rdr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return list;
	}
    
	public static String getBase64(File aFile) throws IOException
	{
		byte[] bytes = getBytes(aFile);
		return Base64.getEncoder().encodeToString(bytes);
	}
	
	public static byte[] getBytes(File aFile) throws IOException
	{
		ByteArrayOutputStream byteOut	= null;
		BufferedOutputStream out 		= null;
		BufferedInputStream in 			= null;
		
		try {
			byteOut		= new ByteArrayOutputStream();
			out 		= new BufferedOutputStream(byteOut);
			in 			= new BufferedInputStream(new FileInputStream(aFile));
			
			byte[] buff = new byte[8*1024];
			int n = 0;
		    while ((n = in.read(buff)) >= 0) {
		        out.write(buff, 0, n);
		    }
		    out.flush();
		    return byteOut.toByteArray();
		}
		finally
		{
			if(in!=null)
			{
				in.close();
			}
			if(out!=null)
			{
				out.close();
			}
			if(byteOut!=null)
			{
				byteOut.close();
			}
		}
	}
	
	public static byte[] getBytesFromBase64(String aBase64) throws IOException
	{
		ByteArrayOutputStream byteOut	= null;
		BufferedOutputStream out 		= null;
		ByteArrayInputStream in 		= null;
		
		try {
			byteOut		= new ByteArrayOutputStream();
			out 		= new BufferedOutputStream(byteOut);
			in 			= new ByteArrayInputStream(Base64.getDecoder().decode(aBase64));
			
			byte[] buff = new byte[64*1024];
			int n = 0;
		    while ((n = in.read(buff)) >= 0) {
		        out.write(buff, 0, n);
		    }
		    return byteOut.toByteArray();
		}
		finally
		{
			if(in!=null)
			{
				in.close();
			}
			if(out!=null)
			{
				out.close();
			}
			if(byteOut!=null)
			{
				byteOut.close();
			}
		}
	}
	
	public static boolean loadNativeLib(String aLibraryName, String aLibPath) throws Exception
    {
    	boolean isLoaded = false;
    	Throwable exception = null;
    	String sLoadFrom = "";
    	
    	try {
    		System.loadLibrary(aLibraryName);
    		isLoaded = true;
    		sLoadFrom = "System : "+aLibraryName;
    	}
    	catch(Throwable e)
    	{
    		isLoaded = false;
    		exception = e;
    	}
    	
    	if(!isLoaded && aLibPath!=null && aLibPath.trim().length()>0)
    	{
			StringBuffer sbLibFileName = new StringBuffer(aLibPath);
			
			if(!aLibPath.endsWith("/"))
				sbLibFileName.append("/");
			
			String sOSName = System.getProperty("os.name");
			if(sOSName==null)
				sOSName = "";
			
			boolean isWindows = sOSName.toLowerCase().indexOf("win")>-1;
			boolean isMac = sOSName.toLowerCase().indexOf("mac")>-1;
			
			if(!isWindows)
			{
				sbLibFileName.append("lib");
			}
			
			sbLibFileName.append(aLibraryName);
			
			if(isWindows)
			{
				sbLibFileName.append(".dll");
			}
			else if(isMac)
			{
				sbLibFileName.append(".dylib");
			}
			else
			{
				sbLibFileName.append(".so");
			}
			
			String sLibLoadPath = URLEncoder.encode(sbLibFileName.toString(), "UTF-8");
			
			sLibLoadPath = sLibLoadPath.replaceAll("%2F", "/");
			sLibLoadPath = sLibLoadPath.replaceAll("%5C", "/");
			
			//System.out.println("### Encoded URL : "+sLibLoadPath);
			
			//Trying to load it from achieve (WAR etc)
	    	URL url = getCallerClass().getResource(sLibLoadPath);
	    	sLoadFrom = "Archive";
	    	if(url==null)
	    	{
	    		try {
	    			//Trying to load it from physical path
					url = new URL("file://"+sLibLoadPath);
					sLoadFrom = "File";
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
	    	}
	    	
	    	sLibLoadPath = URLDecoder.decode(url.getPath(), "UTF-8");
	    	sLoadFrom += " : "+sLibLoadPath;
	    	
	    	try {
	    		//System.out.println("### Trying to load from "+sLibLoadPath);
	    		System.load(sLibLoadPath);
	        	isLoaded = true;
	        } catch (Throwable e) {
	        	exception = e;
	    		isLoaded = false;
	        }
    	}
    	
    	if(!isLoaded && exception!=null)
    	{
    		throw new Exception(exception.getMessage()+"- libname:"+aLibraryName+" libpath:"+aLibPath);
    	}
    	
    	if(isLoaded)
    	{
    		System.out.println("Successfully loaded native libraries "+aLibraryName+" from "+sLoadFrom);
    	}
    	
    	return isLoaded;
    }
	
	private static Class<?> getCallerClass() 
	{
		StackTraceElement[] stacks = Thread.currentThread().getStackTrace();
		Class<?> classCaller = FileUtil.class;
		
		for(int i=0; i<stacks.length; i++)
		{
			String sCallerClassName = stacks[i].getClassName();
			
			if("jakarta.servlet.http.HttpServlet".equals(sCallerClassName))
			{
				return FileUtil.class;
			}
			//System.out.println(" - "+i+" ) "+sCallerClassName);
		}
		
		for(int i=0; i<stacks.length; i++)
		{
			String sCallerClassName = stacks[i].getClassName();
			
			//System.out.println("++getCallerClass()="+sCallerClassName);
			
			if(sCallerClassName.equals(FileUtil.class.getName())
				|| sCallerClassName.equals(Thread.class.getName()))
				continue;
			
			try {
				Class.forName(sCallerClassName);
				classCaller = Class.forName(sCallerClassName);
				break;
			} catch (ClassNotFoundException e) {
			}			
		}
		
		return classCaller;
	}
	
	public static File[] getFilesWithExtensions(File aFolder, String[] aFileExts)
	{
		File[] files = new File[]{};
		
		if(aFolder!=null && aFolder.isDirectory())
		{
			if(aFileExts!=null && aFileExts.length>0)
			{
				files = aFolder.listFiles(
							new FilenameFilter() {
					
							@Override
							public boolean accept(File dir, String name) {
								String sFName = name.toUpperCase();
								for(String sExt : aFileExts)
								{
									if(sFName.endsWith(sExt.toUpperCase()))
									{
										return true;
									}
								}
								return false;
							}
						});
			}
			else
			{
				files = aFolder.listFiles();
			}
		}
		
		return files;
	}
	
    public static void main(String args[]) throws Exception
    {
    	
    	System.out.println(URLEncoder.encode("\\", "UTF-8"));
    	
    	
    }
}
