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

public class CommonException extends Exception {

	private static final long serialVersionUID = -1068899117288657750L;
	private String error_code 		= null;
	private String error_subject 	= null;
	private String error_reason 	= null;
	
	private String error_debuginfo 	= null;
	
	private Throwable throwable 	= null;
	private String throwable_cause 	= null;
	
	public static boolean debug_mode = false;

	public CommonException(String aErrCode, String aErrReason)
	{
		super(aErrReason);
		init(aErrCode, aErrReason, null, null);
	}
	
	public CommonException(String aErrCode, String aErrReason, Throwable aThrowable)
	{
		super(aErrReason, aThrowable);
		init(aErrCode, aErrReason, null, aThrowable);
	}
	
	public CommonException(String aErrCode, String aErrReason, String aErrDebugInfo, Throwable aThrowable)
	{
		super(aErrReason, aThrowable);
		init(aErrCode, aErrReason, aErrDebugInfo, aThrowable);
	}
	
	
	public CommonException(String aErrCode, Throwable aThrowable)
	{
		super(aThrowable);
		init(aErrCode, null, null, aThrowable);
	}
	
	public void setErrSubjectAndReason(String aErrCode, String aErrSubject, String aErrReason)
	{
		this.error_code 	= aErrCode;
		this.error_subject 	= aErrSubject;
		this.error_reason 	= aErrReason; 
	}
	
	public String getErrorSubject()
	{
		return this.error_subject;
	}
	
	public void setErrorSubject(String aErrSubject)
	{
		this.error_subject = aErrSubject;
	}


	public String getErrorReason()
	{
		return this.error_reason;
	}

	public void setErrorReason(String aErrReason)
	{
		this.error_reason = aErrReason;
	}

	private void init(String aErrCode, String aErrReason, String aErrDebugInfo, Throwable aThrowable)
	{
		error_code = aErrCode;
		throwable_cause = getCauseErrMsg(aThrowable);
		error_reason = aErrReason;
		error_debuginfo = aErrDebugInfo;
		throwable = aThrowable;
		
		if(error_reason==null)
		{
			error_reason = throwable_cause;
			throwable_cause = null;
		}
		
		if(aThrowable!=null)
		{
			aThrowable.printStackTrace();
		}
	}

	
	public String getErrorCode()
	{
		return error_code;
	}
	
	public String getErrorMsg()
	{
		StringBuffer sb = new StringBuffer();
		
		if(getErrorSubject()!=null)
		{
			sb.append(getErrorSubject());
		}
		
		if(getErrorReason()!=null)
		{
			if(sb.length()>0)
				sb.append(" - ");
			sb.append(getErrorReason());
		}
		
		if(sb.length()==0 && throwable!=null)
		{
			sb.append(throwable.getMessage());
		}
		
		return sb.toString();
	}
	
	public Throwable getThrowable()
	{
		return throwable;
	}
	
	public String getErrorCause()
	{
		return throwable_cause;
	}
	
	public String getErrorDebugInfo()
	{
		return error_debuginfo;
	}
	
	public void setErrorCause(String aErrorCause)
	{
		throwable_cause = aErrorCause;
	}
	
	public void setErrorDebugInfo(String aErrorDebugInfo)
	{
		error_debuginfo = aErrorDebugInfo;
	}
	
	public void setDebugMode(boolean isDebug)
	{
		debug_mode = isDebug;
	}

	public String getMessage()
	{
		StringBuffer sbErr = new StringBuffer();
		sbErr.append(getErrorMsg());
		//
		if(debug_mode)
		{
			sbErr.append("  ").append(getErrorDebugInfo());
		}
		return sbErr.toString();
	}
	
	private String getCauseErrMsg(Throwable aThrowable)
	{
		if(aThrowable==null)
			return null;
		
		if(aThrowable instanceof CommonException)
		{
			return ((CommonException)aThrowable).throwable_cause;
		}
		
		Throwable t = aThrowable.getCause();
		if(t!=null && t.getMessage()!=null)
		{
			return  t.getMessage();
		}
		
		return aThrowable.getMessage();
	}
}