package hl.common;

import static java.awt.Color.RGBtoHSB;

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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

import hl.common.http.RestApiUtil;


public class ImgUtil {
	
	public enum MOZAIC_STYLE { RANDOM_COLOR, RANDOM_TILES, TRANSPARENT }; 
	
	private static final String HTMLIMG_HEADER 		= ";base64,";
	private static final int DATA_EMBED_MIN_HEIGHT 	= 16;
	private static final int DATA_EMBED_MIN_WIDTH 	= 16;

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
	
	//mozaic 
	public static BufferedImage mozaic(BufferedImage aImage, int aTileSize, 
			MOZAIC_STYLE aMozaicStyle, boolean aIsOdd) throws IOException
	{
		if(aImage==null)
			return null;
		
		int maxw = aImage.getWidth();
		int maxh = aImage.getHeight();
		
		int cols = maxw / aTileSize;
		int rows = maxh / aTileSize;
		
		BufferedImage newImage = new BufferedImage(maxw, maxh, BufferedImage.TYPE_INT_ARGB);
		
		int px = 0;
		int py = 0;
		
		Graphics2D gfx 				= null;
		BufferedImage tmpImg 		= null;
		List<BufferedImage> listImg = new ArrayList<BufferedImage>();

		int adj = (aIsOdd?1:0);
		int x = adj;
        int y = 0;

		boolean isOddRow = aIsOdd;
        //Prepare Data
        switch(aMozaicStyle)
    	{
        	case RANDOM_TILES : 
    	        for(; y<rows; y++)
    	        {
    	        	py = y*aTileSize;
    		        for(;x<cols;)
    		        {
    		        	px = x*aTileSize;
    		        	tmpImg = aImage.getSubimage(px, py, aTileSize, aTileSize);
    		        	listImg.add(tmpImg);
    		        	x+=2;
    		        }
    		        isOddRow = !isOddRow;
    		        x = isOddRow?1:0;
    	        };
    	        break;
        	case TRANSPARENT :
        		return getImageAltTiles(aImage, aTileSize, !aIsOdd);
        		
        	default:;
    	}
        	

		try {
	        
	        gfx = newImage.createGraphics();
	        Random rand = new Random();
	        
	        isOddRow = aIsOdd;
	        x = adj; 
            y = 0;
            
	        for(; y<rows; y++)
	        {
	        	py 	= y*aTileSize;
		        for(;x<cols;)
		        {
		        	px = x*aTileSize;
		        	
		        	switch(aMozaicStyle)
		        	{
		        		case RANDOM_TILES :
		        		{
				        	gfx.drawImage(listImg.remove(rand.nextInt(listImg.size())), px, py, null);
		        			break;
		        		}
		        		case RANDOM_COLOR :
		        		{
		        			gfx.setColor(new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()));
		        			gfx.fillRect(px, py, aTileSize , aTileSize);
		        			break;
		        		}
		        		default:;
		        	}
		        	x+=2;
		        }
		        isOddRow = !isOddRow;
		        x = isOddRow?1:0;
	        }

	    }finally {
	    	if(gfx!=null)
	    		gfx.dispose();
	    }
        
		return newImage;
	}
	
	public static BufferedImage getImageAltTiles(BufferedImage aImage, int aTileSize, boolean aIsOdd) throws IOException
	{
		if(aImage==null)
			return null;
		
		int maxw = aImage.getWidth();
		int maxh = aImage.getHeight();
		
		int cols = maxw / aTileSize;
		int rows = maxh / aTileSize;
		
		BufferedImage newImage = new BufferedImage(maxw, maxh, BufferedImage.TYPE_INT_ARGB);
		
		int px = 0;
		int py = 0;
		
		BufferedImage tmpImg = null;
		Graphics2D gfx = null;
		
        try {
	        gfx = newImage.createGraphics();
	        
	        int x = aIsOdd?1:0;
	        int y = 0;
			boolean isOddRow = aIsOdd;
			
	        for(; y<rows; y++)
	        {
	        	py = y*aTileSize;
		        for(;x<cols;)
		        {
		        	px = x*aTileSize;
		        	tmpImg = aImage.getSubimage(px, py, aTileSize, aTileSize);
		        	gfx.drawImage(tmpImg, px, py, null);
		        	x+=2;
		        }
		        isOddRow = !isOddRow;
		        x = isOddRow?1:0;
	        }
	    }finally {
	    	if(gfx!=null)
	    		gfx.dispose();
	    }
        
		return newImage;
	}
	
	public static String img2Ascii(BufferedImage aBufferedImage, long aWidth, long aHeight) throws IOException
	{
		StringBuffer sb = new StringBuffer();
		
		String sChArt = ".,oaOB#@";
		
		if(aBufferedImage!=null)
		{
			BufferedImage imgTmp = resizeImg(aBufferedImage, aWidth, aHeight, true);
			imgTmp = grayscale(imgTmp);
			
			long imgW = imgTmp.getWidth();
			long imgH = imgTmp.getHeight();
			
			for(int y=0; y<imgH; y++)
			{
				for(int x=0; x<imgW; x++)
				{
					Color c = new Color(imgTmp.getRGB(x, y));
					int iVal = (int)((c.getRed()*0.33f)+(c.getGreen()*0.33f)+(c.getBlue()*0.34f))/32;
					sb.append(sChArt.charAt(iVal));
				}
				sb.append("\n");
			}
		}
		return sb.toString();
	}
	

	public static BufferedImage toThermal(final BufferedImage imgInput)
	{
		BufferedImage imgOut = new BufferedImage(
				imgInput.getWidth(), imgInput.getHeight(), imgInput.getType());
		
		int pixel;
        int red;
        int blue;
        int green;
        float[] hsbvals = new float[3];
        
		BufferedImage imgTmp = new BufferedImage(
				imgInput.getWidth(), imgInput.getHeight(), imgInput.getType());;

        for(int x = 0; x < imgInput.getWidth(); x++){
            for (int y = 0; y < imgInput.getHeight(); y++){
                pixel = imgInput.getRGB(x,y);
                red = (pixel & 0x00ff0000) >> 16;
                blue = (pixel & 0x0000ff00) >> 8;
                green = (pixel & 0x000000ff);
                hsbvals = RGBtoHSB(red, green, blue, hsbvals);
                if(hsbvals[2] > 0.7){
                	imgTmp.setRGB(x,y,Color.red.getRGB());
                }else if(hsbvals[2] >=0.2 && hsbvals[2] < 0.5){
                	imgTmp.setRGB(x, y, Color.blue.getRGB());
                }else if(hsbvals[2] >= 0.5 && hsbvals[2] < 0.7){
                	imgTmp.setRGB(x, y, Color.green.getRGB());
                }
            }
        }
        
        Graphics2D graphics = imgOut.createGraphics();
        graphics.drawImage(imgTmp,0,0,null);
        graphics.dispose();
        
		return imgOut;
	}
	
	public static BufferedImage grayscale(BufferedImage aBufferedImage) throws IOException
	{
		if(aBufferedImage==null)
			return null;
		
		BufferedImage newImage = new BufferedImage(
				(int) aBufferedImage.getWidth(), 
				(int) aBufferedImage.getHeight(),
				aBufferedImage.getType());
		
	        int h = aBufferedImage.getHeight();
	        int w = aBufferedImage.getWidth();
	        for(int x=0; x<w; x++)
	        {
		        for(int y=0; y<h; y++)
		        {
		        	int p = aBufferedImage.getRGB(x, y);
		        	int a = (p>>24)&0xff;
		        	
		        	int r = (p>>16)&0xff;
		        	int g = (p>>8)&0xff;
		        	int b = p&0xff;
		        	
		        	int rgb_sum = r+g+b;

		        	//
		        	r = rgb_sum/3;
		        	g = r;
		        	b = r;
		        	//
		        	p = (a<<24) | (r<<16) | (g<<8) | b;
		        	newImage.setRGB(x, y, p);
		        }
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

    public static BufferedImage pixelize(final BufferedImage aImgOrig, float aPixelizeThreshold) throws IOException
	{
		if(aImgOrig==null)
			return null;
		
		float fPixelPercent = 1-aPixelizeThreshold;
		float iWidth 	= aImgOrig.getWidth() * fPixelPercent;
		float iHeight 	= aImgOrig.getHeight() * fPixelPercent;
		
		if(iWidth<1)
			iWidth = 1;
		if(iHeight<1)
			iHeight = 1;
		
		BufferedImage imgPixelized = resizeImg(aImgOrig, (long)iWidth, (long)iHeight, false);
		
		return resizeImg(imgPixelized, aImgOrig.getWidth(), aImgOrig.getHeight(), false);
	}
    
	public static BufferedImage pixelize(final BufferedImage aImgOrig) throws IOException
	{
		return pixelize(aImgOrig, 1);
	}
	
	public static BufferedImage cloneImage(final BufferedImage source){
	    
		if(source==null)
			return null;
		
		int iType = source.getType();
		
		if(iType == BufferedImage.TYPE_CUSTOM)
		{
			iType = BufferedImage.TYPE_INT_RGB;
		}
		
		BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), iType);
	    Graphics g = null;
	    try {
		    g = b.getGraphics();
		    g.drawImage(source, 0, 0, null);
	    }
	    finally
	    {
	    	if(g!=null)
	    	{
	    		g.dispose();
	    	}
	    }
	    return b;
	}
	
	public static BufferedImage pixelize(final BufferedImage aImgOrig, int aFilterLoop) throws IOException
	{
		float fPixelise = 0.90f;
		
		BufferedImage imgTmp = pixelize(aImgOrig, fPixelise);
	
		if(aFilterLoop>1)
		{
			for(int i=0; i<aFilterLoop-1; i++)
			{
				imgTmp = pixelize(imgTmp, fPixelise);
			}
		}
		return imgTmp;
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
	
	private static int byte2int(byte aByte)
	{
		return aByte & 0xff;
	}
	
	public static String getEmbededData(
			BufferedImage aBufferedImage,
			String aSignature) throws IOException, NoSuchAlgorithmException
	{
		String sEncodedData = null;
		
		if(aBufferedImage!=null)
		{
			long lImgH = aBufferedImage.getHeight();
			long lImgW = aBufferedImage.getWidth();
			
			if((lImgH<DATA_EMBED_MIN_HEIGHT)||(lImgW<DATA_EMBED_MIN_WIDTH))
			{
				return null;
			}
			
			byte[] byteSignature = CryptoUtil.getMD5Checksum(aSignature.getBytes());
			
			
			int y1 = 0;
			int y2 = aBufferedImage.getHeight()-1;
        	int yLoop = new Color(aBufferedImage.getRGB(0, 0)).getGreen();
        	if(yLoop<=0)
        		yLoop = 1;
        	
        	int colW = byteSignature.length;
        	
			byte[] byteData = new byte[colW*yLoop];

			for(int i=0; i<colW; i+=2)
			{
				Color c = new Color(aBufferedImage.getRGB(i, y1));
	        	int v1 = byte2int(byteSignature[i]);
	        	int v2 = byte2int(byteSignature[i+1]);
	        	
	        	if(v1!=c.getRed() || v2!=c.getBlue())
	        	{
	        		System.out.println("break!");
	        		System.out.println("v1="+v1+":"+c.getRed());
	        		System.out.println("v2="+v2+":"+c.getBlue());
	        		return null;
	        	}
	        	
				////////////////////////////////////
				for(int l=0; l<yLoop; l++)
				{
		        	c = new Color(aBufferedImage.getRGB(i, y2-l));
		        	int idx = i+(l*(colW));
					//System.out.println("getEncodedData.idx:"+idx);
		        	
		        	byteData[idx] = (byte) c.getRed();
		        	byteData[idx+1] = (byte) c.getBlue();
				}
			}
			
			sEncodedData = new String(byteData);
			
		}
		return sEncodedData; 
	}
	
	public static BufferedImage embedData(
			BufferedImage aBufferedImage,
			String aSignature,
			String aData) throws IOException, NoSuchAlgorithmException
	{
		if(aBufferedImage!=null)
		{
			long lImgH = aBufferedImage.getHeight();
			long lImgW = aBufferedImage.getWidth();
			
			if((lImgH<DATA_EMBED_MIN_HEIGHT)||(lImgW<DATA_EMBED_MIN_WIDTH))
			{
				throw new IOException("Image width and heigh is smaller than minimum data embeding size !");
			}
			
			byte[] byteSignature = CryptoUtil.getMD5Checksum(aSignature.getBytes());
			byte[] byteData = aData.getBytes();
			
			int iPaddingCount = byteData.length % byteSignature.length;
			if(iPaddingCount !=0)
			{
				//data padding
				for(int i=0; i<iPaddingCount; i++)
				{
					aData += "-";
				}
				byteData = aData.getBytes();
				//throw new IOException("signature:"+byteSignature.length+" data:"+byteData.length);
			}
			
			int y1 = 0;
			int y2 = aBufferedImage.getHeight()-1;
			
			int colW = byteSignature.length;
			int yLoop = byteData.length / colW;
			
			if(yLoop<=0)
				yLoop = 1;
			
			for(int i=0; i<colW; i+=2)
			{
				Color c = new Color(aBufferedImage.getRGB(i, y1));
	        	
	        	int v1 = byte2int(byteSignature[i]);
	        	int v2 = byte2int(byteSignature[i+1]);
	        	int g  = i==0?(yLoop):c.getGreen();
	        	
	        	
	        	Color c2 = new Color(v1, g, v2);
				aBufferedImage.setRGB(i, y1, c2.getRGB());
				
				////////////////////////////////////
				for(int l=0; l<yLoop; l++)
				{
					int idx = i+(l*(colW));
					//System.out.println("encode.idx:"+idx);
					
					c = new Color(aBufferedImage.getRGB(i, y2-l));
		        	
		        	v1 = byte2int(byteData[idx]);
		        	v2 = byte2int(byteData[idx+1]);
		        	//
		        	c2 = new Color(v1, c.getGreen(), v2);
		        	aBufferedImage.setRGB(i, y2-l, c2.getRGB());
				}
			}
		}
		return aBufferedImage;
	}
}
