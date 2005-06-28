/*
 * ExceptionUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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

	public static StringBuffer getSqlStateString(SQLException se)
	{
		StringBuffer result = new StringBuffer(30);
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
		StringBuffer result;
		try
		{
			if (th.getMessage() == null)
			{
				result = new StringBuffer(th.getClass().getName());
			}
			else
			{
				result = new StringBuffer(th.getMessage().trim());
			}
			
			if (th instanceof SQLException)
			{
				SQLException se = (SQLException)th;
				StringBuffer state = getSqlStateString(se);
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
			result = new StringBuffer("Exception: " + th.getClass().getName());
		}
		return result.toString();
	}


}
