package hl.common;

import java.util.Properties;

public class PropUtilTest {
	
    public static void main(String args[]) throws Exception
    {
    	Properties prop = PropUtil.loadProperties("./test/resources/test2.properties");
    	
    	for(Object oKey : prop.keySet())
    	{
    		String sVal = prop.getProperty(oKey.toString());
    		sVal = sVal.replaceAll("\n", " ");
    		if(sVal.length()>=80)
    		{
    			sVal = sVal.substring(0,79)+" ... (truncated)";
    		}
    		System.out.println(oKey+" = "+sVal);
    	}
    	
    }
}
