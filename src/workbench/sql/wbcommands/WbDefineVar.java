/*
 * WbDefineVar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;

import workbench.db.WbConnection;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.VariablePool;
import workbench.sql.StatementRunnerResult;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 *
 * @author  support@sql-workbench.net
 */
public class WbDefineVar 
	extends SqlCommand
{
	public static final String VERB_DEFINE_LONG = "WBVARDEFINE";
	public static final String VERB_DEFINE_SHORT = "WBVARDEF";
	public static final WbDefineVar DEFINE_LONG = new WbDefineVar(VERB_DEFINE_LONG);
	public static final WbDefineVar DEFINE_SHORT = new WbDefineVar(VERB_DEFINE_SHORT);

	private String verb = null;
	private WbDefineVar(String aVerb)
	{
		this.verb = aVerb;
	}

	public String getVerb() { return verb; }

	protected boolean isConnectionRequired() { return false; }
	
	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String sql = stripVerb(aSql);

		WbStringTokenizer tok = new WbStringTokenizer("=", true, "\"'", false);
		tok.setSourceString(sql);
		String value = null;
		String var = null;

		if (tok.hasMoreTokens()) var = tok.nextToken();

		if (var == null)
		{
			result.addMessage(ResourceMgr.getString("ErrVarDefWrongParameter"));
			result.setFailure();
			return result;
		}

		var = var.trim();
		
		if (tok.hasMoreTokens()) value = tok.nextToken();

		if ("-file".equalsIgnoreCase(var))
		{
			if (value != null)
			{
				try
				{
					String filename = this.evaluateFileArgument(value);
					File f = new File(filename);
					if (f.exists())
					{
						VariablePool.getInstance().readFromFile(filename);
						String msg = ResourceMgr.getString("MsgVarDefFileLoaded");
						msg = StringUtil.replace(msg, "%file%", filename);
						result.addMessage(msg);
						result.setSuccess();
					}
					else
					{
						String msg = ResourceMgr.getString("ErrFileNotFound");
						msg = StringUtil.replace(msg, "%file%", filename);
						result.addMessage(msg);
						result.setFailure();
					}
				}
				catch (Exception e)
				{
					File f = new File(value);
					LogMgr.logError("WbDefineVar.execute()", "Error reading definition file: " + value, e);
					String msg = ResourceMgr.getString("ErrReadingVarDefFile");
					msg = StringUtil.replace(msg, "%file%", f.getAbsolutePath());
					msg = msg + " " + ExceptionUtil.getDisplay(e);
					result.addMessage(msg);
					result.setFailure();
				}
			}
			else
			{
				result.addMessage(ResourceMgr.getString("ErrReadingVarDefFile"));
				result.setFailure();
			}
			return result;
		}

		String msg = null;
		result.setSuccess();

		if (value != null)
		{
			value = value.trim();
			if (value.startsWith("@"))
			{
				String valueSql = null;
				try
				{
					valueSql = value.substring(1);
					value = this.evaluateSql(aConnection, valueSql);
				}
				catch (Exception e)
				{
					LogMgr.logError("WbDefineVar.execute()", "Error retrieving variable value using SQL: " + valueSql, e);
					String err = ResourceMgr.getString("ErrReadingVarSql");
					err = StringUtil.replace(err, "%sql%", valueSql);
					err = err + "\n\n" + ExceptionUtil.getDisplay(e);
					result.addMessage(err);
					result.setFailure();
					return result;
				}
			}
			
			msg = ResourceMgr.getString("MsgVarDefVariableDefined");
			try
			{
				VariablePool.getInstance().setParameterValue(var, value);
				msg = StringUtil.replace(msg, "%var%", var);
				msg = StringUtil.replace(msg, "%value%", value);
				msg = StringUtil.replace(msg, "%varname%", VariablePool.getInstance().buildVarName(var, false));
			}
			catch (IllegalArgumentException e)
			{
				result.setFailure();
				msg = ResourceMgr.getString("ErrVarDefWrongName");
				result.setFailure();
			}
		}
		else
		{
			msg = ResourceMgr.getString("MsgVarDefVariableRemoved");
			msg = msg.replaceAll("%var%", var);
		}

		result.addMessage(msg);

		return result;
	}
	
	/**
	 *	Return the result of the given SQL string and return 
	 *	the value of the first column of the first row 
	 *	as a string value.
	 *
	 *	If the SQL gives an error, an empty string will be returned
	 */
	private String evaluateSql(WbConnection conn, String sql)
		throws SQLException
	{
		ResultSet rs = null;
		String result = StringUtil.EMPTY_STRING;

		try
		{
			this.currentStatement = conn.createStatement();
			
			if (sql.endsWith(";"))
			{
				sql = sql.substring(0, sql.length() - 1);
			}
			rs = this.currentStatement.executeQuery(sql);
			if (rs.next())
			{
				Object value = rs.getObject(1);
				if (value != null)
				{
					result = value.toString();
				}
			}
			
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
		}
		
		return result;
	}

}
