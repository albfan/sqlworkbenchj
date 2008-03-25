/*
 * AlterSessionCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import workbench.db.DbMetadata;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class AlterSessionCommand
	extends SqlCommand
{
	public static final String VERB = "ALTER SESSION";
	
	public AlterSessionCommand()
	{
		super();
	}

	public String getVerb() { return VERB; }
	
	public StatementRunnerResult execute(String sql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		result.setSuccess();
		
		String oldSchema = null;
		SQLLexer lexer = new SQLLexer(sql);
		
		DbMetadata meta = currentConnection.getMetadata();
		
		// Skip the ALTER SESSION verb
		SQLToken token = lexer.getNextToken(false, false);
		
		token = lexer.getNextToken(false, false);
		if (token.getContents().equals("SET"))
		{
			// check for known statements
			token = lexer.getNextToken(false, false);
			String parm = (token != null ? token.getContents() : null);
			if ("CURRENT_SCHEMA".equalsIgnoreCase(parm))
			{
				oldSchema = meta.getCurrentSchema();
			}
			else if ("TIME_ZONE".equalsIgnoreCase(parm) && meta.isOracle())
			{
				// this should be the = sign, skip it
				token = lexer.getNextToken(false, false);
				
				// this is the parameter for the new timezone
				token = lexer.getNextToken(false, false);
				if (token != null)
				{
					if (changeOracleTimeZone(currentConnection, result, token.getContents()))
					{
						return result;
					}
				}
			}
		}

		try
		{
			this.currentStatement = currentConnection.createStatement();
			this.currentStatement.executeUpdate(sql);
			if (oldSchema == null)
			{
				String msg = VERB + " " + ResourceMgr.getString("MsgKnownStatementOK");
				result.addMessage(msg);
			}
			else 
			{
				String schema = meta.getCurrentSchema();
				if (!oldSchema.equalsIgnoreCase(schema))
				{
					currentConnection.schemaChanged(oldSchema, schema);
					result.addMessage(ResourceMgr.getFormattedString("MsgSchemaChanged", schema));
				}
			}
			
			result.setSuccess();
		}
		catch (Exception e)
		{
			addErrorInfo(result, sql, e);
			LogMgr.logSqlError("AlterSessionCommand.execute()", sql, e);
		}
		
		return result;
	}
	
	private boolean changeOracleTimeZone(WbConnection con, StatementRunnerResult result, String tz)
	{
		Connection sqlCon = currentConnection.getSqlConnection();
		Method setTimezone = null;
		
		try
		{
			Class cls = currentConnection.getSqlConnection().getClass();
			setTimezone = cls.getMethod("setSessionTimeZone", new Class[] {String.class} );
		}
		catch (Exception e)
		{
			// Ignore
			return false;
		}
		
		if (setTimezone != null)
		{
			try
			{
				String zone = StringUtil.trimQuotes(tz);
				LogMgr.logDebug("AlterSessionCommand.changeOracleTimeZone()", "Calling Oracle's setSessionTimeZone");
				setTimezone.setAccessible(true);
				setTimezone.invoke(sqlCon, new Object[] { zone });
				result.addMessage(ResourceMgr.getString("MsgTimezoneChanged") + " " + zone);
				result.setSuccess();
				return true;
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		return false;
	}
}
