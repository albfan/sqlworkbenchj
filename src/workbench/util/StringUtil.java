/*
 * StringUtil.java
 *
 * Created on December 2, 2001, 9:35 PM
 */
package workbench.util;


/**
 *
 *	@author  thomas.kellerer@web.de
 */
public class StringUtil
{
	

	public static String getStartingWhiteSpace(final String aLine)
	{
		if (aLine == null) return null;
		int pos = 0;
		int len = aLine.length();
		if (len == 0) return "";
		
		char c = aLine.charAt(pos);
		while (c <= ' ' && pos < len)
		{
			pos ++;
			c = aLine.charAt(pos);
		}
		String result = aLine.substring(0, pos);
		return result;
	}
			
}
