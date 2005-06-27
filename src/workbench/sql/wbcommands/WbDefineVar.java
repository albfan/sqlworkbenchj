/*
 * WbDefineVar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
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
public class WbDefineVar extends SqlCommand
{
	public static final WbDefineVar DEFINE_LONG = new WbDefineVar("WBVARDEFINE");
	public static final WbDefineVar DEFINE_SHORT = new WbDefineVar("WBVARDEF");

	private String verb = null;
	private WbDefineVar(String aVerb)
	{
		this.verb = aVerb;
	}

	public String getVerb() { return verb; }

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		String sql = aSql.trim().substring(this.getVerb().length()).trim();

		String msg = null;

		WbStringTokenizer tok = new WbStringTokenizer("=", true, "\"'", false);
		tok.setSourceString(sql);
		String value = null;
		String var = null;

		if (tok.hasMoreTokens()) var = tok.nextToken();

		if (var == null)
		{
			result.addMessage(ResourceMgr.getString("ErrorVarDefWrongParameter"));
			result.setFailure();
			return result;
		}

		if (tok.hasMoreTokens()) value = tok.nextToken();

		if ("-file".equalsIgnoreCase(var))
		{
			if (value != null)
			{
				try
				{
					File f = new File(value);
					VariablePool.getInstance().readFromFile(value);
					msg = ResourceMgr.getString("MsgVarDefFileLoaded");
					msg = StringUtil.replace(msg, "%file%", f.getAbsolutePath());
					result.addMessage(msg);
					result.setSuccess();
				}
				catch (Exception e)
				{
					File f = new File(value);
					LogMgr.logError("WbDefineVar.execute()", "Error reading definition file: " + value, e);
					msg = ResourceMgr.getString("ErrorReadingVarDefFile");
					msg = StringUtil.replace(msg, "%file%", f.getAbsolutePath());
					msg = msg + " " + ExceptionUtil.getDisplay(e);
					result.addMessage(msg);
					result.setFailure();
				}
			}
			else
			{
				result.addMessage(ResourceMgr.getString("ErrorReadingVarDefFile"));
				result.setFailure();
			}
			return result;
		}

		result.setSuccess();

		if (value != null)
		{
			if (value.startsWith("@"))
			{
				String valueSql = null;
				try
				{
					valueSql = value.substring(1);
					value = this.evaluateSql(aConnection, valueSql);
				}
				catch (SQLException e)
				{
					LogMgr.logError("WbDefineVar.execute()", "Error retrieving variable value using SQL: " + valueSql, e);
					msg = ResourceMgr.getString("ErrorReadingVarSql");
					msg = StringUtil.replace(msg, "%sql%", valueSql);
					msg = msg + "\n\n" + ExceptionUtil.getDisplay(e);
					result.addMessage(msg);
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
				msg = ResourceMgr.getString("ErrorVarDefWrongName");
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
		VariablePool parameterPool = VariablePool.getInstance();
		String realSql = parameterPool.replaceAllParameters(sql);

		try
		{
			this.currentStatement = conn.createStatement();
			// HSQL until 1.7.2 has a bug when group functions are used 
			// together with setMaxRows(). The group function will only
			// evaluate as many rows as defined via setMaxRows.
			if (!conn.getMetadata().isHsql()) this.currentStatement.setMaxRows(1);
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
			this.currentStatement.setMaxRows(0);
			try { rs.close(); } catch (Throwable th) {}
		}
		
		return result;
	}

}
