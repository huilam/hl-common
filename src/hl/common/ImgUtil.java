package hl.common;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.imageio.ImageIO;

import hl.common.http.RestApiUtil;


public class ImgUtil {
	
	private static final String HTMLIMG_HEADER 		= ";base64,";

	public static String convertToBMP(String aJpgFileName) throws IOException
	{
		return convert(aJpgFileName, "BMP");
	}
	
	public static String convertToJPG(String aBmpFileName) throws IOException
	{
		return convert(aBmpFileName, "JPG");
	}
	
	public static String convertToPNG(String aBmpFileName) throws IOException
	{
		return convert(aBmpFileName, "PNG");
	}
	
	public static byte[] toBytes(BufferedImage aBufferedImage, String aImageFormat) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		if(aImageFormat==null || aImageFormat.trim().length()==0)
		{
			aImageFormat = "PNG";
		}
		
		try {
			
			if(hasAlpha(aBufferedImage) && !aImageFormat.equalsIgnoreCase("PNG"))
			{
				aBufferedImage = removeAlpha(aBufferedImage);
			}
			ImageIO.write(aBufferedImage, aImageFormat, out);
		}
		catch(IOException ioEx)
		{
			String supportedFormatNames[] = ImageIO.getWriterFormatNames();
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<supportedFormatNames.length; i++)
			{
				if(sb.length()>0)
					sb.append(",");
				sb.append(supportedFormatNames[i]);
			}
			Exception ex = new Exception("Supported Format :["+sb.toString()+"]");
			ioEx.addSuppressed(ex);
			throw ioEx;
		}
		
		
		return out.toByteArray();
	}
	
	private static boolean hasAlpha(final BufferedImage aImage)
	{
		boolean foundAlpha = false;
		if(aImage!=null)
		{
			foundAlpha = (aImage.getColorModel().hasAlpha());
		}
		return foundAlpha;
	}
	
	public static BufferedImage removeAlpha(BufferedImage aImage)
	{
		if(!hasAlpha(aImage))
			return aImage;
		
		BufferedImage imgNew = null;
		switch(aImage.getType())
		{
			case BufferedImage.TYPE_INT_ARGB:
				imgNew = new BufferedImage(
						aImage.getWidth(), 
						aImage.getHeight(), 
						BufferedImage.TYPE_INT_RGB);
				break;
			case BufferedImage.TYPE_4BYTE_ABGR:
				imgNew = new BufferedImage(
						aImage.getWidth(), 
						aImage.getHeight(), 
						BufferedImage.TYPE_3BYTE_BGR);
				break;
		}
		
		return copyImageTo(aImage, imgNew);
	}
	
	public static BufferedImage addAlpha(BufferedImage aImage)
	{
		if(hasAlpha(aImage))
			return aImage;
		
		BufferedImage imgNew = null;
		switch(aImage.getType())
		{
			case BufferedImage.TYPE_INT_RGB:
				imgNew = new BufferedImage(
						aImage.getWidth(), 
						aImage.getHeight(), 
						BufferedImage.TYPE_INT_ARGB);
				break;
			case BufferedImage.TYPE_3BYTE_BGR:
				imgNew = new BufferedImage(
						aImage.getWidth(), 
						aImage.getHeight(), 
						BufferedImage.TYPE_4BYTE_ABGR);
				break;
		}
		
		return copyImageTo(aImage, imgNew);
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
	
	public static String imageFileToBase64(final String aSourceURI) throws IOException
	{
		String sFormat = "JPG";
		
		int iPos = aSourceURI.indexOf(".");
		if(iPos>-1)
		{
			sFormat = aSourceURI.substring(iPos).toUpperCase();
		}
		
		BufferedImage img = loadImage(aSourceURI);
		return imageToBase64(img, sFormat);
	}
	
	public static String imageFileToBase64(final String aSourceURI, final String aImgFormat) throws IOException
	{
		BufferedImage img = loadImage(aSourceURI);
		return imageToBase64(img, aImgFormat);
	}
	
	public static String imageToBase64(BufferedImage aBufferedImage, final String aImgFormat) throws IOException
	{
		String sBase64 = null;
		
		if(aBufferedImage!=null)
		{
			ByteArrayOutputStream outImg = null;
			
			try{
				outImg = new ByteArrayOutputStream();
				
				if(!aImgFormat.equalsIgnoreCase("PNG"))
				{
					if(aBufferedImage.getType()!=BufferedImage.TYPE_INT_RGB)
					{
						BufferedImage newRGBImage = new BufferedImage(
								aBufferedImage.getWidth(), 
								aBufferedImage.getHeight(), 
								BufferedImage.TYPE_INT_RGB);
				        Graphics2D g = null;
				        try {
					        g = newRGBImage.createGraphics();
					        g.drawImage(aBufferedImage, 0, 0, null);
					        
				        }finally {
				        	if(g!=null)
				        		g.dispose();
				        }
				        aBufferedImage = newRGBImage;
					}
				}
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
	
	public static boolean base64ToImageFile(final String aImageBase64, final String aOutputFormat, final String aImageFileName) throws IOException
	{	
		BufferedImage img = base64ToImage(aImageBase64);
		return saveAsFile(img, aOutputFormat, new File(aImageFileName));
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
	
    public static boolean writeBase64ToFile(File aFile, final String aBase64Content) throws IOException
    {
    	if(aFile.exists())
    	{
    		throw new IOException("File exists: "+aFile);
    	}
    	else
    	{
    		createFoldersIfNotExist(aFile);
    	}
    	
		File tmpOutputFile = new File(aFile.getCanonicalPath()+".wip");
    	BufferedWriter wrt = null;
		try {
			wrt = new BufferedWriter(new FileWriter(tmpOutputFile));
			wrt.write(aBase64Content);
		}finally
		{
			if(wrt!=null)
				wrt.close();
		}
		return tmpOutputFile.renameTo(aFile);
    }

    public static boolean saveAsFile(BufferedImage aBufferedImage, File aOutputFile) throws IOException
	{
    	String sImgFormat = "JPG";
    	String sFileName = aOutputFile.getName();
    	int iExtPos = sFileName.lastIndexOf(".");
    	if(iExtPos>-1)
    	{
    		sImgFormat = sFileName.substring(iExtPos+1).toUpperCase();
    	}
    	
    	return saveAsFile(aBufferedImage, sImgFormat, aOutputFile);
	}
    
    public static boolean saveAsFile(BufferedImage aBufferedImage, String aOutputFileFormat, File aOutputFile) throws IOException
	{
    	if(aOutputFileFormat!=null)
    	{
    		if(!aOutputFileFormat.equalsIgnoreCase("PNG"))
    		{
    			//remove transparency 
    			BufferedImage newImage = new BufferedImage(
    					aBufferedImage.getWidth(), 
    					aBufferedImage.getHeight(), 
    					BufferedImage.TYPE_INT_RGB);
    	        Graphics2D g = null;
    	        try {
    		        g = newImage.createGraphics();
    		        g.drawImage(aBufferedImage, 0, 0, null);
    	        }finally {
    	        	if(g!=null)
    	        		g.dispose();
    	        }
    	        aBufferedImage = newImage;
    		}
    	}
    	
		createFoldersIfNotExist(aOutputFile);
		String sWipFileName = aOutputFile.getPath()+"_"+System.nanoTime()+".wip";
		
		File tmpOutputFile = new File(sWipFileName);
		boolean isCreated = ImageIO.write(aBufferedImage, aOutputFileFormat, tmpOutputFile);
		
		if(aOutputFile.exists())
		{
			aOutputFile.delete();
		}
		
		return isCreated && tmpOutputFile.renameTo(aOutputFile);
	}
    
    private static boolean createFoldersIfNotExist(File aOutputFile)
    {
    	if(aOutputFile!=null && aOutputFile.getParentFile()!=null)
    	{
    		return aOutputFile.getParentFile().mkdirs();
    	}
    	return false;
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
		BufferedImage img = base64ToImage(aImageBase64);
		img = resizeImg(img, aNewWidth, aNewHeight, isMaintainAspectRatio);
		return imageToBase64(img, "JPG");
	}
	
	public static BufferedImage resizeImg(BufferedImage aBufferedImage, long aNewWidth, long aNewHeight, boolean isMaintainAspectRatio) throws IOException
	{
		BufferedImage img = aBufferedImage;
		
		try {
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
	        return tranformImage(img, at);
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
			
			if(lWidth<=0)
				lWidth = 1;
			
			if(lHeight<=0)
				lHeight = 1;

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
		BufferedImage img = base64ToImage(aImageBase64);
		img = flipImg(img, isFlipHorizontal);
		return ImgUtil.imageToBase64(img, "JPG");
	}
	
	public static BufferedImage flipImg(BufferedImage aImage, boolean isFlipHorizontal) throws IOException
	{
		BufferedImage img = aImage;
		
		try {
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
	        return tranformImage(img, at);
		}
		finally
		{
	    	if(img!=null)
	    		img.flush();
		}
	}
	
	public static BufferedImage extractImage(BufferedImage aBufferedImage, Rectangle aRect) throws IOException
	{
		if(aBufferedImage==null)
			return null;
		
		BufferedImage img = aBufferedImage.getSubimage(
				(int)aRect.getX(),
				(int)aRect.getY(),
				(int)aRect.getWidth(),
				(int)aRect.getHeight());
		
		return img;
	}
	
	public static BufferedImage convertImageType(BufferedImage aInputImg, int aNewImageType)
	{
		BufferedImage imgNew = new BufferedImage(aInputImg.getWidth(), aInputImg.getHeight(), aNewImageType);
		
		Graphics2D g = null;
		try {
			g = (Graphics2D) imgNew.createGraphics();
	        g.drawImage(aInputImg,0,0,null);
		}
		finally
		{
			if(g!=null)
				 g.dispose();
		}
		
		return imgNew;
	}

	public static BufferedImage adjOpacity(BufferedImage aBufferedImage, float aOpacity)
	{
		if(aBufferedImage==null)
			return null;
		
		if(aOpacity<0)
			aOpacity = 0;
		else if(aOpacity>1)
			aOpacity = 1;
		
		int iImageType = BufferedImage.TYPE_4BYTE_ABGR;

		switch(aBufferedImage.getType())
		{
			case BufferedImage.TYPE_INT_BGR:;
			case BufferedImage.TYPE_INT_RGB:;
			case BufferedImage.TYPE_INT_ARGB:
				iImageType = BufferedImage.TYPE_INT_ARGB;;
				break;
		}
	
		BufferedImage newImage = new BufferedImage(
				aBufferedImage.getWidth(), 
				aBufferedImage.getHeight(),
				iImageType);
		
		
		Graphics2D g = null;
		try {
			g = (Graphics2D) newImage.getGraphics();
	
			AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, aOpacity);
			g.setComposite(composite); 
			g.drawImage(aBufferedImage, 0,0, null);
			
		}finally
		{
			if(g!=null)
				g.dispose();
		}
        
		return newImage;
	}	
	
	public static BufferedImage overlayImage(BufferedImage aBufferedImage, BufferedImage aSubImage, int x, int y) throws IOException
	{
		if(aBufferedImage==null)
			return null;
		
		if(aSubImage==null)
			return aBufferedImage;
	
		//with transparency 
		BufferedImage newImage = new BufferedImage(
				(int) aBufferedImage.getWidth(), 
				(int) aBufferedImage.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		
		Graphics2D g = null;
        try {
	        g = newImage.createGraphics();
	        g.drawImage(aBufferedImage, 0, 0, null);
	        g.drawImage(aSubImage, x, y, null);
        }finally {
        	if(g!=null)
        		g.dispose();
        }
        
		return newImage;
	}
	
    public static BufferedImage cropImage(BufferedImage inputImg, int cropLeft, int cropTop, int cropWidth, int cropHeight) 
    {
        BufferedImage croppedImage = null;
        if (inputImg != null && cropLeft >= 0 && cropTop >= 0 && cropWidth > 0 && cropHeight > 0) {
        	
        	int iWidth 	= inputImg.getWidth();
        	int iHeight = inputImg.getHeight();
        	
        	int iCropMaxX = cropLeft + cropWidth;
        	int iCropMaxY = cropTop + cropHeight;
        	
        	if(iCropMaxX>iWidth)
        	{
        		int iAdjW = iCropMaxX-iWidth;
        		cropWidth -= iAdjW;
        	}
        	
        	if(iCropMaxY>iHeight)
        	{
        		int iAdjH = iCropMaxY-iHeight;
        		cropHeight -= iAdjH;
        	}
        	
             //Force ARBG to RBG only
             BufferedImage img = new BufferedImage(iWidth, iHeight, BufferedImage.TYPE_INT_RGB);
             img.createGraphics().drawImage(inputImg, 0, 0, Color.WHITE, null);
             
             croppedImage = img.getSubimage(cropLeft, cropTop, cropWidth, cropHeight);
        }
        return croppedImage;
    }
	
	public static BufferedImage cloneImage(final BufferedImage source){
	    
		if(source==null)
			return null;
		
		int iType = source.getType();
		
		if(iType == BufferedImage.TYPE_CUSTOM)
		{
			iType = BufferedImage.TYPE_INT_RGB;
		}
		
		BufferedImage dest = new BufferedImage(source.getWidth(), source.getHeight(), iType);
		
		return copyImageTo(source, dest);
	}
	
	public static BufferedImage copyImageTo(final BufferedImage source, BufferedImage dest){
	    
		if(source==null)
			return null;
		
		BufferedImage imgReturn = dest;
		if(imgReturn==null) 
		{
			imgReturn = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
		}
		
	    Graphics g = null;
	    try {
		    g = imgReturn.getGraphics();
		    g.drawImage(source, 0, 0, null);
	    }
	    finally
	    {
	    	if(g!=null)
	    	{
	    		g.dispose();
	    	}
	    }
	    return imgReturn;
	}
	
	public static byte[] getChecksum(final BufferedImage aBufferedImage) throws IOException, NoSuchAlgorithmException
	{
		if(aBufferedImage!=null)
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(aBufferedImage, "PNG", baos);
			byte[] bytes = baos.toByteArray();
			return CryptoUtil.getMD5Checksum(bytes);
		}
		return null;
	}
	
	
	////////////////////////////////////////////////////////////////////////////////////v
	///     Filters
	////////////////////////////////////////////////////////////////////////////////////v

	@Deprecated
	public static BufferedImage pixelize(final BufferedImage aImgOrig, float aPixelizeThreshold) throws IOException
	{
		return ImgFilters.pixelize(aImgOrig, aPixelizeThreshold);
	}
    
	@Deprecated
	public static BufferedImage pixelize(final BufferedImage aImgOrig) throws IOException
	{
		return ImgFilters.pixelize(aImgOrig);
	}
	
	@Deprecated
	public static BufferedImage grayscale(BufferedImage aBufferedImage) throws IOException
	{
		return ImgFilters.grayscale(aBufferedImage);
	}
	
	@Deprecated
	public static BufferedImage getImageAltTiles(BufferedImage aImage, int aTileSize, boolean aIsOdd) throws IOException
	{
		return ImgFilters.getImageAltTiles(aImage, aTileSize, aIsOdd);
	}
	
	@Deprecated
	public static String img2Ascii(BufferedImage aBufferedImage, long aWidth, long aHeight) throws IOException
	{
		return ImgFilters.img2Ascii(aBufferedImage, aWidth, aHeight);
	}
	
	@Deprecated
	public static BufferedImage toThermal(final BufferedImage imgInput)
	{
		return ImgFilters.toThermal(imgInput);
	}
	
}
