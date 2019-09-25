package hl.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipUtil{
	
	public static byte[] compress(String data) throws IOException {
		if(data==null)
			return null;
		return compress(data.getBytes());
	}
	
	public static byte[] compress(byte[] data) throws IOException {
		ByteArrayOutputStream bos 	= null;
		GZIPOutputStream gzip 		= null;
		byte[] compressed 			= null;
		
		try {
			bos = new ByteArrayOutputStream(data.length);
			gzip = new GZIPOutputStream(bos);
			gzip.write(data);
			gzip.finish();
			compressed = bos.toByteArray();
		}finally
		{
			if(gzip!=null)
				gzip.close();
			if(bos!=null)
				bos.close();
		}
		
		
		return compressed;
	}
	
	public static byte[] decompress(byte[] compressed) throws IOException {
		ByteArrayOutputStream bos = null;

		ByteArrayInputStream bis = null;
		GZIPInputStream gis = null;
		BufferedReader br = null;
		
		try {
			bis = new ByteArrayInputStream(compressed);
			gis = new GZIPInputStream(bis);
			
			bos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
            int len;
            while((len = gis.read(buffer)) != -1){
            	bos.write(buffer, 0, len);
            }
			
		}finally {
			if(br!=null)
				br.close();
			
			if(gis!=null)
				gis.close();
			
			if(bis!=null)
				bis.close();
		}

		return bos.toByteArray();
}
	
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
