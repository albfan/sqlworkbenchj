/*
 * DdlCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.db.WbConnection;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.ExceptionUtil;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class DdlCommand extends SqlCommand
{
	public static final DdlCommand CREATE = new DdlCommand("CREATE");
	public static final DdlCommand DROP = new DdlCommand("DROP");
	public static final DdlCommand ALTER = new DdlCommand("ALTER");
	public static final DdlCommand GRANT = new DdlCommand("GRANT");
	public static final DdlCommand REVOKE = new DdlCommand("REVOKE");

	public static final DdlCommand CHECKPOINT = new DdlCommand("CHECKPOINT");
	public static final DdlCommand SHUTDOWN = new DdlCommand("SHUTDOWN");

	// Firebird RECREATE VIEW command
	public static final SqlCommand RECREATE = new DdlCommand("RECREATE");

	public static final List DDL_COMMANDS = new LinkedList();

	static
	{
		DDL_COMMANDS.add(DROP);
		DDL_COMMANDS.add(CREATE);
		DDL_COMMANDS.add(ALTER);
		DDL_COMMANDS.add(GRANT);
		DDL_COMMANDS.add(REVOKE);
		DDL_COMMANDS.add(CHECKPOINT);
		DDL_COMMANDS.add(SHUTDOWN);
	}

	private String verb;

	private DdlCommand(String aVerb)
	{
		this.verb = aVerb;
		this.isUpdatingCommand = true;
	}

	public StatementRunnerResult execute(WbConnection aConnection, String aSql)
		throws SQLException
	{
		StatementRunnerResult result = new StatementRunnerResult();
		try
		{
			this.currentStatement = aConnection.createStatement();

			aSql = aConnection.getMetadata().filterDDL(aSql);

			String msg = null;

			if (isDropCommand(aSql) && aConnection.getIgnoreDropErrors())
			{
				try
				{
					this.currentStatement.executeUpdate(aSql);
					result.addMessage(ResourceMgr.getString("MsgDropSuccess"));
				}
				catch (Throwable th)
				{
					result.addMessage(ResourceMgr.getString("MsgDropWarning"));
					result.addMessage(ExceptionUtil.getDisplay(th));
				}
			}
			else
			{
				this.currentStatement.executeUpdate(aSql);
				boolean schemaChanged = false;

				if ("ALTER".equals(verb) && aConnection.getMetadata().isOracle())
				{
					// check for schema change in oracle
					String regex = "alter\\s+session\\s+set\\s+current_schema\\s*=\\s*(\\p{Graph}*)";
					Pattern p = Pattern.compile(regex,Pattern.CASE_INSENSITIVE);
					Matcher m = p.matcher(aSql);

					if (m.find() && m.groupCount() > 0)
					{
						String schema = m.group(1);
						aConnection.schemaChanged(null, schema);
						schemaChanged = true;
					}
				}

				if (schemaChanged)
				{
					msg = ResourceMgr.getString("MsgSchemaChanged");
				}
				else if ("DROP".equals(verb))
				{
					msg = ResourceMgr.getString("MsgDropSuccess");
				}
				else if ("CREATE".equals(verb) || "RECREATE".equals(verb))
				{
					msg = ResourceMgr.getString("MsgCreateSuccess");
				}
				else
				{
					msg = this.verb + " " + ResourceMgr.getString("MsgKnownStatementOK");
				}
				result.addMessage(msg);

				StringBuilder warnings = new StringBuilder();
				if (this.appendWarnings(aConnection, this.currentStatement, warnings))
				{
					result.addMessage(warnings.toString());
					this.addExtendErrorInfo(aConnection, aSql, result);
				}
			}
			result.setSuccess();
		}
		catch (Exception e)
		{
			result.clear();

			StringBuilder msg = new StringBuilder(150);
			msg.append(ResourceMgr.getString("MsgExecuteError") + "\n");
			if (reportFullStatementOnError)
			{
				msg.append(aSql);
			}
			else
			{
				int maxLen = 150;
				msg.append(StringUtil.getMaxSubstring(aSql.trim(), maxLen));
			}
			msg.append("\n");
			result.addMessage(msg.toString());
			result.addMessage(ExceptionUtil.getAllExceptions(e));

			this.addExtendErrorInfo(aConnection, aSql, result);
			result.setFailure();
			LogMgr.logSqlError("DdlCommand.execute()", aSql, e);
		}
		finally
		{
			// we know that we don't need the statement any longer, so to make
			// sure everything is cleaned up, we'll close it here
			this.done();
		}

		return result;
	}

	public boolean isDropCommand(String sql)
	{
		if ("DROP".equals(this.verb)) return true;
		Pattern p = Pattern.compile("DROP\\s+(PRIMARY\\s+KEY|CONSTRAINT)\\s+", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(sql);
		return m.find();
	}

	/**
	 * Extract the name of the created object for Oracle stored procedures.
	 * @see #addExtendErrorInfo(workbench.db.WbConnection, String, workbench.sql.StatementRunnerResult)
	 */
	protected String getObjectName(String sql)
	{
		SQLLexer l = new SQLLexer(sql);
		SQLToken t = l.getNextToken(false, false);
		if (t == null) return null;
		if (!t.getContents().equals("CREATE") && !t.getContents().equals("CREATE OR REPLACE")) return null;
		
		// next token must be the type
		t = l.getNextToken(false, false);
		if (t == null) return null;
		
		// the token after the type must be the object's name
		t = l.getNextToken(false, false);
		return t.getContents();		
	}

	/**
	 * Retrieve extended error information if the DBMS supports this.
	 * Currently this is only implemented for Oracle to read errors
	 * after creating a stored procedure from the ALL_ERRORS view.
	 *
	 * @see #getObjectName(String)
	 * @see #getObjectType(String)
	 */
  private boolean addExtendErrorInfo(WbConnection aConnection, String sql, StatementRunnerResult result)
  {
    String type = SqlUtil.getCreateType(sql);
		if (type == null) return false;
		
    String name = getObjectName(sql);
		if (name == null) return false;

    String msg = aConnection.getMetadata().getExtendedErrorInfo(null, name, type);
		if (msg != null && msg.length() > 0)
		{
			result.addMessage(msg);
			return true;
		}
		else
		{
			return false;
		}
  }

	public String getVerb()
	{
		return verb;
	}

}
