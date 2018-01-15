package hl.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil{
	
	public static void unZip(String aZipFile, String aDestFolder) throws IOException
	{
		File folderDest = new File(aDestFolder);
		
		//if not exits create the dest folder
		if(!folderDest.exists())
		{
			folderDest.mkdirs();
		}
		
		//if dest folder not found , throw exception
		if(!folderDest.isDirectory())
		{
			throw new IOException("Invalid destination folder ! - "+aDestFolder);
		}
		
		
		File fileZip = new File(aZipFile);
		ZipInputStream zipIn = null;
		
		try{
			
			zipIn = new ZipInputStream(new FileInputStream(fileZip));
			
			ZipEntry entry = null;
			while((entry = zipIn.getNextEntry())!=null)
			{
	            String filePath = folderDest.getPath() + File.separator + entry.getName();
	            if (!entry.isDirectory()) {
	                // if the entry is a file, extracts it
	            	FileUtil.inputStreamToFile(zipIn, filePath);
	            } else {
	                // if the entry is a directory, make the directory
	                File dir = new File(filePath);
	                dir.mkdir();
	            }
	            zipIn.closeEntry();
	        }
			
		}
		finally
		{
			if(zipIn!=null)
				zipIn.close();
		}
	}

	
	
	public static void main(String args[]) throws Exception
	{
		
	}
	
	
	

}
