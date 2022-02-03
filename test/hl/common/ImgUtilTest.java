package hl.common;

import java.awt.image.BufferedImage;
import java.io.File;


public class ImgUtilTest extends ImgUtil {
	
	public static void main(String args[]) throws Exception
	{
		String sTestDir = new File(".").getAbsoluteFile()+"//test//";
		
		File fileImg1080p = new File(sTestDir+"//X-Men-1920x1080.jpg");
		File fileImg720p = new File(sTestDir+"//X-Men-1080x720.jpg");

		
		System.out.println();
		
		
		File[] files = new File[] {fileImg1080p, fileImg720p};
		
		for(File f : files)
		{
			BufferedImage img =  ImgUtil.loadImage(f.getPath());
			
			System.out.println("img="+img);
			
			//DataBufferInt databuf = (DataBufferInt) img.getRaster().getDataBuffer();
			

			//int[] iImgData = databuf.getData();
			
			byte[] byteChecksum = ImgUtil.getChecksum(img);
			String sMD5 = CryptoUtil.toHexString(byteChecksum);
			BufferedImage img2 = ImgUtil.embedData(img, "nec-nls-matrix", sMD5);
			
			String sData = ImgUtil.getEmbededData(img2, "nec-nls-matrix");
			
			System.out.println("sMD5="+sMD5);
			System.out.println("sData="+sData);

			String sImgAscii = img2Ascii(img, 192, 108);
			//System.out.println(sImgAscii);
			
			
			BufferedImage imgTransparent = ImgUtil.adjOpacity(img, 0.20f);
			ImgUtil.saveAsFile(imgTransparent, new File(sTestDir+"//"+f.getName()+"_transparent.jpg"));
			ImgUtil.saveAsFile(imgTransparent, new File(sTestDir+"//"+f.getName()+"_transparent.png"));
			
		}
		
		
	}
	
}
