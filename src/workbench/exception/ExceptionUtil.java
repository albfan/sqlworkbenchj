/*
 * ExceptionUtil.java
 *
 * Created on December 1, 2001, 6:28 PM
 */
package workbench.exception;

import java.io.StringWriter;
import java.io.PrintWriter;

/**
 *
 *	@author  thomas.kellerer@web.de
 */
public class ExceptionUtil
{
	
	public ExceptionUtil()
	{
	}
	
	public static String getDisplay(Throwable th)
	{
		return getDisplay(th, false);
	}
	
	public static String getDisplay(Throwable th, boolean includeStackTrace)
	{
		StringBuffer result = new StringBuffer(th.getMessage());
		
		if (includeStackTrace) 
		{
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			th.printStackTrace(pw);
			result.append("\r\n");
			result.append(sw.getBuffer());
			
		}
		return result.toString();
	}
	
	public static void main(String args[])
	{
		Exception e = new NullPointerException("Testing");
		System.out.println("e=" + getDisplay(e, true));
		System.out.println("*****");
	}
	
}
