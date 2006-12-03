/*
 * ExceptionUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

/**
 *
 *	@author  support@sql-workbench.net
 */
public class ExceptionUtil
{

	private ExceptionUtil()
	{
	}

	public static StringBuilder getSqlStateString(SQLException se)
	{
		StringBuilder result = new StringBuilder(30);
		try
		{
			String state = se.getSQLState();
			if (state != null && state.length() > 0)
			{
				result.append("SQL State=");
				result.append(state);
			}
			int error = se.getErrorCode();
			if (error != 0)
			{
				if (result.length() > 0) result.append(", ");
				result.append("DB Errorcode=");
				result.append(Integer.toString(error));
			}
		}
		catch (Throwable th)
		{
			//result.append("(unknown)");
		}
		return result;
	}

	public static String getDisplay(Throwable th)
	{
		return getDisplay(th, false);
	}

	public static String getDisplay(Throwable th, boolean includeStackTrace)
	{
		StringBuilder result;
		try
		{
			if (th.getMessage() == null)
			{
				result = new StringBuilder(th.getClass().getName());
				if (!includeStackTrace) 
				{
					// always include Stacktrace for NPE
					// sometimes these are not properly logged, and this way
					// the stacktrace does at least show up in the front end
					// which should not happen anyway, but if it does, 
					// we have at least proper error information
					includeStackTrace = (th instanceof NullPointerException);
				} 
			}
			else
			{
				result = new StringBuilder(th.getMessage().trim());
			}
			
			if (th instanceof SQLException)
			{
				SQLException se = (SQLException)th;
				StringBuilder state = getSqlStateString(se);
				if (state.length() > 0)
				{
					result.append(" [");
					result.append(state);
					result.append("] ");
				}
			}

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
			LogMgr.logError("ExceptionUtil.getDisplay()", "Error while creating display string", th1);
			result = new StringBuilder("Exception: " + th.getClass().getName());
		}
		return result.toString();
	}


}
