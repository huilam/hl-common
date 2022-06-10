/*
 Copyright (c) 2017 onghuilam@gmail.com
 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.
 The Software shall be used for Good, not Evil.
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 
 */

package hl.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class HLFileWriter{
	
	private String filename	 	= null;
	
	private boolean auto_split 	= true;
	
	private boolean auto_roll 	= true;
	private long auto_roll_maxcount 		= 5;
	private long auto_roll_threshold_bytes 	= 10000000;
	
	private BufferedWriter writer 	= null;
	private File file				= null;
	
	private long last_line_repeat_count		= 0;
	private long repeat_silent_threshold	= 100;
	private String last_line				= null;
	private boolean isWithTimestamp			= true;
	
	private SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS ");
	
	
	public HLFileWriter(String aFileName)
	{
		this.filename = aFileName;
	}
	
	public HLFileWriter(String aFileName, long aAutoRollBytes, int aAutoRollFileCount)
	{
		this.filename = aFileName;
		if(aAutoRollBytes>-1)
		{
			this.auto_roll_threshold_bytes = aAutoRollBytes;
		}
		
		if(aAutoRollFileCount>-1)
		{
			this.auto_roll_maxcount = aAutoRollFileCount;
		}
	}
	
	public boolean isAuto_split() {
		return auto_split;
	}

	public void setAuto_split(boolean auto_split) {
		this.auto_split = auto_split;
	}

	public boolean isAuto_roll() {
		return auto_roll;
	}

	public void setAuto_roll(boolean auto_roll) {
		this.auto_roll = auto_roll;
	}
	
	public long getAuto_roll_maxcount() {
		return this.auto_roll_maxcount;
	}

	public void setAuto_roll_maxcount(long roll_maxcount) {
		this.auto_roll_maxcount = roll_maxcount;
	}

	public long getAuto_roll_threshold_in_bytes() {
		return this.auto_roll_threshold_bytes;
	}

	public void setAuto_roll_threshold_in_bytes(long roll_threshold_bytes) {
		this.auto_roll_threshold_bytes = roll_threshold_bytes;
	}


	private BufferedWriter initWritter() throws IOException
	{
		file = new File(this.filename);
		if(!file.exists())
		{
			if(file.getAbsoluteFile().getParentFile()!=null)
			{
				file.getAbsoluteFile().getParentFile().mkdirs();
			}
		}
		
		return new BufferedWriter(new FileWriter(file, true));
	}
	
	public void writeln(String aLine) throws IOException
	{
		if(write(aLine))
		{
			writer.newLine();
			writer.flush();
		}
	}
	
	public boolean write(String aLine) throws IOException
	{
		if(file!=null && this.auto_split)
		{
			//System.out.println("size="+file.length());;
			if(file.length()>this.auto_roll_threshold_bytes)
			{
				File newFile = null;
				closeWriter();
				
				if(this.auto_roll)
				{
					Map<Long, File> mapFileList = new TreeMap<Long, File>();
					
					File folder = file.getAbsoluteFile().getParentFile();
					
					for(File f : folder.listFiles())
					{
						if(f.getName().startsWith(this.filename))
						{
							mapFileList.put(f.lastModified(), f);
						}
					}
					
					//autoroll deletion
					long lPendingDel = mapFileList.size()-this.auto_roll_maxcount;
					if(lPendingDel>0)
					{
						for(File f : mapFileList.values())
						{
							lPendingDel--;
							//System.out.println("deleting "+f.getAbsolutePath());
							f.delete();
							
							if(lPendingDel<=0)
								break;
						}
					}
					
					mapFileList.clear();
					mapFileList = null;
				}
				SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
				newFile = new File(this.filename+"_"+df.format(new Date()));
				file.renameTo(newFile);
			}
		}

		if(writer==null)
		{
			writer = initWritter();
		}
		
		if(aLine.equals(this.last_line))
		{
			this.last_line_repeat_count++;
			long iVal = (this.last_line_repeat_count+1) % this.repeat_silent_threshold;
			if(iVal!=0)
			{
				return false; //silent
			}
		}
		else
		{
			this.last_line = aLine;
		}
		checkRepeat();
		
		if(isWithTimestamp)
		{
			writer.write(df.format(System.currentTimeMillis())+" ");
		}

		writer.write(aLine);
		
		return true;
	}
	
	private void checkRepeat() throws IOException
	{
		if(writer!=null && this.last_line_repeat_count>0)
		{
			writer.write(" [repeated x"+(this.last_line_repeat_count+1)+"]");
			writer.newLine();
			this.last_line_repeat_count = 0;
		}
	}
	
	public void flush() throws IOException
	{
		checkRepeat();
		if(writer!=null)
		{
			writer.flush();
		}
	}
	
	public void newLine() throws IOException
	{
		if(writer!=null)
		{
			writer.newLine();
		}
	}
	
	public void close()
	{
		closeWriter();
		this.file = null;
		this.last_line_repeat_count = 0;
		this.last_line = null;
	}
	
	public void closeWriter()
	{
		if(writer!=null)
		{
			try {
				flush();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			writer	 = null;
		}
	}
}
