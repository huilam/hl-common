package hl.common;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;

import javax.imageio.ImageIO;

import hl.common.http.RestApiUtil;


public class ImgUtil {
	
	private static final String HTMLIMG_HEADER = ";base64,";
	
	public static String convertToBMP(String aJpgFileName) throws IOException
	{
		return convert(aJpgFileName, "bmp");
	}
	
	public static String convertToJPG(String aBmpFileName) throws IOException
	{
		return convert(aBmpFileName, "jpg");
	}
	
	public static BufferedImage loadImage(final String aSourceFolder, final String aSourceFileName) throws IOException
	{
		StringBuffer sb = new StringBuffer();
		if(aSourceFolder!=null && aSourceFolder.trim().length()>0)
		{
			if(aSourceFileName.startsWith("http"))
			{
				return loadImage(aSourceFileName);
			}
			
			sb.append(aSourceFolder.replaceAll("\\\\", "//"));
		}
		
		if(aSourceFileName!=null)
		{
			if(sb.length()>0)
			{
				if(!sb.toString().endsWith("/"))
				{
					sb.append("/");
				}
			}
			sb.append(aSourceFileName.replaceAll("\\\\", "//"));
		}
		
		return loadImage(sb.toString());
	}
	
	public static BufferedImage loadImage(final String aSourceURI) throws IOException
	{
		if(aSourceURI==null)
			return null;
		
		File fileSource = null;
		
		if(aSourceURI.toLowerCase().startsWith("http"))
		{
			try{
				fileSource = FileUtil.downloadWebImage(aSourceURI);
			}catch(Exception ex)
			{
				System.err.println(" Error downloading "+aSourceURI);
			}
		}
		else
		{
			fileSource= new File(aSourceURI);
		}
		
		if(fileSource!=null && fileSource.isFile())
		{
			return ImageIO.read(fileSource);
		}
		else
		{
			return null;
		}
	}
	
	private static String convert(final String aSourceFileName, final String aOutputFileFormat) throws IOException
	{
		
		File fileOutput = new File(
				aSourceFileName.substring(0, aSourceFileName.length()-4)+"."+aOutputFileFormat);

		BufferedImage image = loadImage(aSourceFileName);
		
		if(image==null)
			throw new IOException("Invalid image file ! - "+aSourceFileName);
		
		saveAsFile(image, aOutputFileFormat, fileOutput);
		
		if(fileOutput.isFile())
			return fileOutput.getPath();
		else
			return null;
	}
	
	public static String imageFileToBase64(final String aSourceURI, final String aImgFormat) throws IOException
	{
		BufferedImage img = loadImage(aSourceURI);
		return imageToBase64(img, aImgFormat);
	}
	
	public static String imageToBase64(final BufferedImage aBufferedImage, final String aImgFormat) throws IOException
	{
		String sBase64 = null;
		
		if(aBufferedImage!=null)
		{
			ByteArrayOutputStream outImg = null;
			
			try{
				outImg = new ByteArrayOutputStream();
				ImageIO.write(aBufferedImage, aImgFormat, outImg);
				sBase64 = Base64.getEncoder().encodeToString(outImg.toByteArray());
			}finally
			{
				if(outImg!=null)
					outImg.close();	
			}
		}
		return sBase64;
	}
	
	public static BufferedImage base64ToImage(String aImageBase64) throws IOException
	{
		ByteArrayInputStream byteImage 	= null;
		BufferedImage img 				= null;
		try{
			aImageBase64 = removeBase64Header(aImageBase64);
			byteImage = new ByteArrayInputStream(Base64.getDecoder().decode(aImageBase64));		
			img = ImageIO.read(byteImage);
		}finally
		{
			if(byteImage!=null)
				byteImage.close();
		}
		return img;
	}
	
	public static String removeBase64Header(String aImageBase64)
	{
		if(aImageBase64==null)
    		return null;
    	
		int ipos = aImageBase64.indexOf(HTMLIMG_HEADER);
		if(ipos>-1)
		{
			aImageBase64 = aImageBase64.substring(ipos + HTMLIMG_HEADER.length());
		}
		return aImageBase64;
	}
	
	public static void base64ToImageFile(final String aImageBase64, final String aOutputFormat, final String aImageFileName) throws IOException
	{	
		BufferedImage img = base64ToImage(aImageBase64);
		saveAsFile(img, aOutputFormat, new File(aImageFileName));
	}
	
    public static String getBase64FromFile(String aFileName) throws IOException
    {
    	File f = new File(aFileName);
    	
    	if(!f.exists())
    	{
    		aFileName = aFileName.replace("#", File.separator);
    		
    		if(aFileName.indexOf('/')==-1 && aFileName.indexOf('\\')==-1)
    		{
    			aFileName = "/"+aFileName;
    		}
    		URL url = RestApiUtil.class.getResource(aFileName);
    		if(url!=null)
    			f = new File(url.getFile());
    	}
    	
    	return getBase64FromFile(f);
    }
    
    public static String getBase64FromFile(File aFile) throws IOException
    {
    	if(aFile!=null && !aFile.isFile())
    	{
    		return null;
    	}
    	
    	BufferedReader rdr = null;	
    	StringBuffer sb = new StringBuffer();
		try {
			rdr = new BufferedReader(new FileReader(aFile));
			String sLine = null;
			while((sLine = rdr.readLine())!=null)
			{
				sb.append(sLine);
			}
		}finally
		{
			if(rdr!=null)
				rdr.close();
		}
		return sb.toString();
    }
	
    public static void writeBase64ToFile(File aFile, final String aBase64Content) throws IOException
    {
    	if(aFile.exists())
    	{
    		throw new IOException("File exists: "+aFile);
    	}
    	
    	BufferedWriter wrt = null;
		try {
			wrt = new BufferedWriter(new FileWriter(aFile));
			wrt.write(aBase64Content);
		}finally
		{
			if(wrt!=null)
				wrt.close();
		}
    }

    public static void saveAsFile(BufferedImage aBufferedImage, String aOutputFileFormat, File aOutputFile) throws IOException
	{
		ImageIO.write(aBufferedImage, aOutputFileFormat, aOutputFile);
	}
	
	public static String resizeBase64ImgByHeight(String aImageBase64, long aNewHeight) throws IOException
	{
		return resizeBase64Img(aImageBase64, 0, aNewHeight, true);
	}
	
	public static String resizeBase64ImgByWidth(String aImageBase64, long aNewWidth) throws IOException
	{
		return resizeBase64Img(aImageBase64, aNewWidth, 0, true);
	}
	
	public static String resizeBase64Img(String aImageBase64, long aNewWidth, long aNewHeight, boolean isMaintainAspectRatio) throws IOException
	{
		BufferedImage img = null;
		
		try {
			img = ImgUtil.base64ToImage(aImageBase64);
			AffineTransform at = new AffineTransform();
			double iWidth = img.getWidth();
			double iHeight = img.getHeight();
			
			double dWidthScale 	= 1;
			double dHeightScale = 1;
			
			if(aNewWidth>0)
			{
				dWidthScale = ((double)aNewWidth) / iWidth;
				
				if(aNewHeight<=0)
					dHeightScale = dWidthScale;
			}
			
			if(aNewHeight>0)
			{
				dHeightScale = ((double)aNewHeight) / iHeight;
				
				if(aNewWidth<=0)
					dWidthScale = dHeightScale;
			}
				
			if(isMaintainAspectRatio)
			{
				if(dWidthScale < dHeightScale)
				{
					dHeightScale = dWidthScale;
				}
				else
				{
					dWidthScale = dHeightScale;
				}
			}
			
			at.setToScale(dWidthScale, dHeightScale);
	        return ImgUtil.imageToBase64(tranformImage(img, at), "JPG");
		}
		finally
		{
			if(img!=null)
	    		img.flush();
		}
	}
	
	private static BufferedImage tranformImage(BufferedImage aBufferedImage, AffineTransform aAffineTransform) throws IOException
	{
		BufferedImage newImage = null;
		try {
			
			double dScaleX = aAffineTransform.getScaleX();
			double dScaleY = aAffineTransform.getScaleY();
			
			double dImgWidth = aBufferedImage.getWidth();
			double dImgHeight = aBufferedImage.getHeight();
			
			if(dScaleX<=0)
				dScaleX = 1;
			
			if(dScaleY<=0)
				dScaleY = 1;
			
			int lWidth = (int)(dImgWidth * dScaleX);
			int lHeight = (int)(dImgHeight * dScaleY);
			
	        newImage = new BufferedImage(lWidth, lHeight, aBufferedImage.getType());
	        Graphics2D g = null;
	        try {
		        g = newImage.createGraphics();
		        g.transform(aAffineTransform);
		        g.drawImage(aBufferedImage, 0, 0, null);
	        }finally {
	        	if(g!=null)
	        		g.dispose();
	        }
	        return newImage;
		}
		finally
		{
			if(newImage!=null)
	    		newImage.flush();
		}
	}
	
	public static String flipBase64Img(String aImageBase64, boolean isFlipHorizontal) throws IOException
	{
		BufferedImage img = null;
		
		try {
			img = ImgUtil.base64ToImage(aImageBase64);
	        AffineTransform at = new AffineTransform();
	        if(isFlipHorizontal)
	        {
		        at.concatenate(AffineTransform.getScaleInstance(-1, 1));
		        at.concatenate(AffineTransform.getTranslateInstance(-img.getWidth(), 0));
	        }
	        else
	        {
		        at.concatenate(AffineTransform.getScaleInstance(1, -1));
		        at.concatenate(AffineTransform.getTranslateInstance(0, -img.getHeight()));
	        }
	        return ImgUtil.imageToBase64(tranformImage(img, at), "JPG");
		}
		finally
		{
	    	if(img!=null)
	    		img.flush();
		}
	}
	
	public static void main(String args[]) throws Exception
	{
		
		String sbase64 = ImgUtil.getBase64FromFile("C:/NLS/huifan.base64");
		
		BufferedImage img = ImgUtil.base64ToImage(sbase64);
		ImgUtil.saveAsFile(img, "JPG", new File("C:/NLS/huifan.base64.jpg"));
		
		sbase64 = ImgUtil.resizeBase64ImgByWidth(sbase64, 180);
		img = ImgUtil.base64ToImage(sbase64);
        ImgUtil.saveAsFile(img, "JPG", new File("C:/NLS/huifan.base64.resized1-"+img.getWidth()+"x"+img.getHeight()+".jpg"));
        
        sbase64 = ImgUtil.resizeBase64ImgByHeight(sbase64, 180);
		img = ImgUtil.base64ToImage(sbase64);
        ImgUtil.saveAsFile(img, "JPG", new File("C:/NLS/huifan.base64.resized2-"+img.getWidth()+"x"+img.getHeight()+".jpg"));
        
        
	}
	
}
