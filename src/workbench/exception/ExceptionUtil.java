/*
 * ExceptionUtil.java
 *
 * Created on December 1, 2001, 6:28 PM
 */
package workbench.exception;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.sql.SQLException;

/**
 *
 *	@author  sql.workbench@freenet.de
 */
public class ExceptionUtil
{

	private ExceptionUtil()
	{
	}

	public static String getDisplay(Throwable th)
	{
		return getDisplay(th, false);
	}

	public static StringBuffer getSqlStateString(SQLException se)
	{
		StringBuffer result = new StringBuffer("SQL State=");
		String state = se.getSQLState();
		if (state != null && state.length() > 0)
		{
			result.append(state);
			result.append(", ");
		}
		int error = se.getErrorCode();
		result.append("Errorcode=");
		result.append(Integer.toString(error));
		return result;
	}

	public static String getDisplay(Throwable th, boolean includeStackTrace)
	{
		StringBuffer result;
		if (th.getMessage() == null)
		{
			result = new StringBuffer(th.getClass().getName());
		}
		else
		{
			result = new StringBuffer(th.getMessage());
		}
		if (th instanceof SQLException)
		{
			SQLException se = (SQLException)th;
			result.append("\r\n(");
			result.append(getSqlStateString(se));
			result.append(") ");
		}

		try
		{
			if (includeStackTrace)
			{
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				th.printStackTrace(pw);
				result.append("\r\n");
				result.append(sw.getBuffer());
			}
		}
		catch (Throwable th1)
		{
			System.err.println("Error while creating display string");
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
