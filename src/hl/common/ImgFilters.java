package hl.common;

import static java.awt.Color.RGBtoHSB;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;


public class ImgFilters {
	
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
			BufferedImage imgTmp = ImgUtil.resizeImg(aBufferedImage, aWidth, aHeight, true);
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
	

    public static BufferedImage pixelize(final BufferedImage aImgOrig, float aPixelizeThreshold) throws IOException
	{
		if(aImgOrig==null)
			return null;
		
		float fPixelPercent = 1-aPixelizeThreshold;
		float iWidth 	= (aImgOrig.getWidth()/2) * fPixelPercent;
		float iHeight 	= (aImgOrig.getHeight()/2) * fPixelPercent;
		
		if(iWidth<1)
			iWidth = 1;
		if(iHeight<1)
			iHeight = 1;
		
		BufferedImage imgPixelized = ImgUtil.resizeImg(aImgOrig, (long)iWidth, (long)iHeight, false);
		
		return ImgUtil.resizeImg(imgPixelized, aImgOrig.getWidth(), aImgOrig.getHeight(), false);
	}
    
	public static BufferedImage pixelize(final BufferedImage aImgOrig) throws IOException
	{
		return pixelize(aImgOrig, 1);
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
}
