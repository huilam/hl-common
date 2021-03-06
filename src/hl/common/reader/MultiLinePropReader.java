package hl.common.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiLinePropReader {

	public interface EventListener {
		public void onReadStart();
		public void onReadConfigSet(long aStartLineNo, long aEndLineNo, Map<String, String> aConfigSet);
		public void onReadEnd(long aTotalLineRead);
	}
	
	private static Logger logger = Logger.getLogger(MultiLinePropReader.class.getName());
	
	private File prop_file 				= null;
	private String comment_prefix 		= "#";
	private String keyvalue_separator 	= "=";
	
	private boolean isPreserveLineBreak 	= false;
	private boolean isPreserveComment 		= false;
	private boolean isEnableDynamicComment  = false;
	
	private Pattern patt_set_start 		= null;
	private EventListener event   		= null;
	
	private Map<String, String> mapConfig 		= null;
	private Map<String, String> mapBlockChar 	= null;
	
	public MultiLinePropReader(File aFile)
	{
		this.prop_file = aFile;
		this.mapConfig = new LinkedHashMap<String, String>();
		
		this.mapBlockChar = new HashMap<String, String>();
	}
	
	public void setKeyValueSeparator(char aSeparator)
	{
		this.keyvalue_separator = String.valueOf(aSeparator);
	}
	
	public void setConfigSetStartRegex(String aRegex)
	{
		this.patt_set_start = Pattern.compile(aRegex);
	}
	
	public void setCommentPrefix(String aCommentPrefix)
	{
		this.comment_prefix = aCommentPrefix;
	}
	
	public void addBlockCharacterSupport(char aStartBlock, char aEndBlock)
	{
		this.mapBlockChar.put(String.valueOf(aStartBlock), String.valueOf(aEndBlock));
	}
	
	public void setIsPreserveLineBreak(boolean aIsPreverseLineBreak)
	{
		this.isPreserveLineBreak = aIsPreverseLineBreak;
	}
	
	public void setIsPreserveComments(boolean aIsPreverseComments)
	{
		this.isPreserveComment = aIsPreverseComments;
	}
	
	public void setIsEnableDynamicComment(boolean aIsEnableDynamicComment)
	{
		this.isEnableDynamicComment = aIsEnableDynamicComment;
	}
	
	
	
	
	public void start() throws IOException
	{
		start(null);
	}
	
	private String checkBlockStart(String aString)
	{
		if(aString!=null)
		{
			String sTrimLine = aString.trim();
			
			for(String sBlockStart : mapBlockChar.keySet())
			{
				if(sTrimLine.startsWith(sBlockStart) || sTrimLine.endsWith(sBlockStart))
				{
					return sBlockStart;
				}			
			}
		}
		return null;
	}
	
	private String checkBlockEnd(String aString, String aBlockStart)
	{
		if(aString!=null && aBlockStart!=null)
		{
			String sTrimLine = aString.trim();
			
			String sBlockEnd = mapBlockChar.get(aBlockStart);
			if(sTrimLine.startsWith(sBlockEnd) || sTrimLine.endsWith(sBlockEnd))
			{
				return sBlockEnd;
			}			
			
		}
		return null;
	}
	
	private String removePartialComment(String aLineData)
	{
		int iPos = aLineData.indexOf(this.comment_prefix);
		if(iPos>-1)
		{
			if(!this.isPreserveComment)
			{
				if(iPos>=0)
				{
					//remove 'partial' comment
					String sTmp = aLineData.substring(0, iPos);
					if(sTmp.trim().length()>0)
					{
						if(this.isEnableDynamicComment)
						{
							aLineData = sTmp;
						}
					}
					else
					{
						aLineData = sTmp;
					}
				}
			}
		}
		return aLineData;
	}
	
	public void start(EventListener aEvent) throws IOException
	{
		BufferedReader rdr = null;
		Matcher m = null;
		try
		{
			this.event = aEvent;
			
			rdr = new BufferedReader(new FileReader(this.prop_file));
			
			if(rdr.ready())
			{
				if(this.event!=null)
				{
					this.event.onReadStart();
				}
				
				long lLineNo = 0;
				long lStartReadLineNo 	= 1;
				long lSetLineCount 		= 1;
				String sLine = null;
				String sKey  = null;
				String sVal  = null;
				String sWithinBlock = null;
				
				StringBuffer sb = new StringBuffer();
				
				boolean isConfigSetStart = true; 
				while((sLine = rdr.readLine())!=null)
				{
					lLineNo++;
					////
					
					logger.log(Level.FINE, "[READ] "+lLineNo+"-"+sLine);
					
					String sTrimLine = removePartialComment(sLine.trim());

					if(sTrimLine.length()==0)
						continue;
					
					if(sWithinBlock==null && sTrimLine.length()>0)
					{
						sWithinBlock = checkBlockStart(sTrimLine);
					}
					
					if(sWithinBlock==null)
					{
						int iPos = sLine.indexOf(this.keyvalue_separator);
						if(iPos>-1)
						{
							if(sb.length()>0)
							{
								//previous
								String sTmp = sb.toString();
								sb.setLength(0);
								iPos = sTmp.indexOf(this.keyvalue_separator);
								if(iPos>0)
								{
									sKey = sTmp.substring(0, iPos);
									sVal = sTmp.substring(iPos+1);
									if(sKey.startsWith(this.comment_prefix))
									{
										if(!this.isPreserveComment)
										{
											continue;
										}
									}
									
									sVal = removePartialComment(sVal);
									mapConfig.put(sKey, sVal);
								}
							}
						}
					}
					
					if(sLine.length()>0)
					{
						if(this.isPreserveLineBreak && sb.length()>0)
						{
							sb.append("\n");
						}
						sb.append(sLine);
						lSetLineCount++;
						if(sWithinBlock!=null)
						{
							if(checkBlockEnd(sTrimLine, sWithinBlock)!=null)
							{
								sWithinBlock = null;
							}
						}
					}
					
					if(mapConfig.size()>0)
					{
						if(this.patt_set_start!=null)
						{
							m = this.patt_set_start.matcher(sLine);
							if(m.find())
							{
								if(!isConfigSetStart && sWithinBlock==null)
								{
									onNewConfigSet(lStartReadLineNo, lSetLineCount, mapConfig);
									mapConfig.clear();
									lStartReadLineNo = lLineNo;
									isConfigSetStart = true;
									lSetLineCount = 1;
								}
							}
							else
							{
								isConfigSetStart = false;
							}
						}
						else
						{
							isConfigSetStart = false;
						}
					}
				}
				
				if(sb.length()>0)
				{
					//previous
					String sTmp = sb.toString();
					sb.setLength(0);
					int iPos = sTmp.indexOf(this.keyvalue_separator);
					if(iPos>0)
					{
						boolean isSkip = false;
						sKey = sTmp.substring(0, iPos);
						sVal = sTmp.substring(iPos+1);
						
						if(sKey.startsWith(this.comment_prefix))
						{
							isSkip = !this.isPreserveComment;
						}
						
						if(!isSkip)
						{
							sVal = removePartialComment(sVal);
							mapConfig.put(sKey, sVal);
						}
					}
				}
				
				if(mapConfig.size()>0)
				{
					onNewConfigSet(lStartReadLineNo, lSetLineCount, mapConfig);
				}
				
				if(this.event!=null)
				{
					this.event.onReadEnd(lLineNo);
				}
			}

		}
		finally
		{
			try {
				if(rdr!=null)
				rdr.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void onNewConfigSet(long aStartLineNo, long aTotalLine, Map<String, String> aConfigSet)
	{
		if(this.event!=null)
		{
			this.event.onReadConfigSet(aStartLineNo, aStartLineNo+aTotalLine-1, aConfigSet);
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////
	
    public static void main(String args[]) throws IOException
    {
    	logger.setLevel(Level.INFO);
    	
    	File f = new File(new File(".").getAbsolutePath()+"/test/test.config");
    	
    	MultiLinePropReader.EventListener e = new MultiLinePropReader.EventListener() {
			
			@Override
			public void onReadStart() {
			}
			
			@Override
			public void onReadEnd(long aTotalLines) {
				System.out.println("TotalLines:"+aTotalLines);
			}
			
			@Override
			public void onReadConfigSet(long aStartLineNo, long aEndLineNo, Map<String, String> aConfigSet) 
			{

				for(String sCfgKey : aConfigSet.keySet())
				{
					System.out.println("["+aStartLineNo+","+aEndLineNo+"] "+sCfgKey+"="+aConfigSet.get(sCfgKey));
				}
			}
		};
		
    	MultiLinePropReader prop = new MultiLinePropReader(f);
       	prop.setKeyValueSeparator('='); //Default '='
       	prop.setCommentPrefix("#"); //Default '#'
       	prop.setIsPreserveComments(false); //Default false
       	prop.setIsPreserveLineBreak(false); //Default false
       	prop.setIsEnableDynamicComment(false); //Default false
		prop.addBlockCharacterSupport('[', ']');
		prop.addBlockCharacterSupport('{', '}');
    	prop.setConfigSetStartRegex("^[VAR|URL]");
    	prop.start(e); //with Event listener
    }
}
