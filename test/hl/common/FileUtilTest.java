package hl.common;

import java.io.File;

public class FileUtilTest {
	
    public static void main(String args[]) throws Exception
    {
    	String sTest = FileUtil.loadContent("resources/test.txt");
    	System.out.println(sTest);
    	
    	System.out.println(new File(".").getCanonicalPath());
    }
}
