/*
 * ExceptionUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
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

/**
 *
 *	@author  Thomas Kellerer
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

	public static StringBuilder getAllExceptions(Throwable th)
	{
		if (th instanceof SQLException)
		{
			return getAllExceptions((SQLException)th);
		}
		StringBuilder result = new StringBuilder(100);
		getDisplayBuffer(result, th, false);
		Throwable cause = th.getCause();
		while (cause != null)
		{
			result.append("\nCaused by: ");
			getDisplayBuffer(result, cause, false);
			cause = cause.getCause();
		}
		return result;
	}
	
	public static StringBuilder getAllExceptions(SQLException th)
	{
		StringBuilder result = new StringBuilder(100);
		getDisplayBuffer(result, th, false);
		SQLException next = th.getNextException();
		while (next != null)
		{
			result.append("\nNext: ");
			getDisplayBuffer(result, next, false);
			next = next.getNextException();
		}
		return result;
	}
	
	public static String getDisplay(Throwable th)
	{
		if (th instanceof SQLException)
		{
			return getAllExceptions((SQLException)th).toString();
		}
		else
		{
			return getDisplay(th, false);
		}
	}

	public static String getDisplay(Throwable th, boolean includeStackTrace)
	{
		return getDisplayBuffer(null, th, includeStackTrace).toString();
	}
	
	public static StringBuilder getDisplayBuffer(StringBuilder result, Throwable th, boolean includeStackTrace)
	{
		if (result == null) result = new StringBuilder(50);
		try
		{
			if (th.getMessage() == null)
			{
				result.append(th.getClass().getName());
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
				result.append(th.getMessage().trim());
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
			result.append("Exception: " + th.getClass().getName());
		}
		return result;
	}

	public static String getStackTrace(Throwable th)
	{
		StringWriter sw = new StringWriter(250);
		PrintWriter pw = new PrintWriter(sw);
		th.printStackTrace(pw);
		return sw.toString();
	}
}
