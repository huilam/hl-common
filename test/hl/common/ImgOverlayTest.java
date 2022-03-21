package hl.common;

import java.awt.image.BufferedImage;
import java.io.File;


public class ImgOverlayTest {
	
	public static void main(String args[]) throws Exception
	{
		String sTestDir = new File(".").getAbsoluteFile()+"//test";
		File fileTestOutputDir = new File("./test/output");
		fileTestOutputDir.mkdirs();
		
		BufferedImage img1 = ImgUtil.loadImage(sTestDir+"/X-Men-1920x1080.jpg");
		BufferedImage img2 = ImgUtil.loadImage(sTestDir+"/friends.jpg");
		BufferedImage img3 = ImgUtil.loadImage(sTestDir+"/friends-head.png");
		
		BufferedImage imgFinal = ImgUtil.overlayImage(img1, img2, 150, 200, 0.8);
		ImgUtil.saveAsFile(imgFinal, new File(fileTestOutputDir+"/overlay-jpg-"+System.currentTimeMillis()+".jpg"));
		
		imgFinal = ImgUtil.overlayImage(img1, img3, 150, 200, 1);		
		ImgUtil.saveAsFile(imgFinal, new File(fileTestOutputDir+"/overlay-png-"+System.currentTimeMillis()+".jpg"));
	}
	
}
