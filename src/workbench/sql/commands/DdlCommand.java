/*
 * DdlCommand.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.commands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.db.WbConnection;
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
	public static final SqlCommand CREATE = new DdlCommand("CREATE");
	public static final SqlCommand DROP = new DdlCommand("DROP");
	public static final SqlCommand ALTER = new DdlCommand("ALTER");
	public static final SqlCommand GRANT = new DdlCommand("GRANT");
	public static final SqlCommand REVOKE = new DdlCommand("REVOKE");

	public static final SqlCommand CHECKPOINT = new DdlCommand("CHECKPOINT");
	public static final SqlCommand SHUTDOWN = new DdlCommand("SHUTDOWN");

	// Firebird RECREATE VIEW command
	public static final SqlCommand RECREATE = new DdlCommand("RECREATE");
	
	public static final List DDL_COMMANDS = new ArrayList();

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

			if (aConnection.getMetadata().isPostgres() && "CREATE".equals(verb))
			{
				aSql = fixPgDollarQuote(aSql);
			}
			
			String msg = null;

			if ("DROP".equals(verb) && aConnection.getIgnoreDropErrors())
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
				this.currentStatement.execute(aSql);
				boolean schemaChanged = false;
				
				if ("ALTER".equals(verb) && aConnection.getMetadata().isOracle())
				{
					// check for schema change in oracle
					String regex = "alter\\s*session\\s*set\\s*current_schema\\s*=\\s*(\\p{Graph}*)";
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

				StringBuffer warnings = new StringBuffer();
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

			StringBuffer msg = new StringBuffer(150);
			msg.append(ResourceMgr.getString("MsgExecuteError") + "\n");
			int maxLen = 150;
			msg.append(StringUtil.getMaxSubstring(aSql.trim(), maxLen));
			msg.append("\n");

			result.addMessage(msg.toString());
			String ex = ExceptionUtil.getDisplay(e);
			result.addMessage(ex);

      this.addExtendErrorInfo(aConnection, aSql, result);
			result.setFailure();
			LogMgr.logDebug("DdlCommand.execute()", "Error executing statement " + ex,null);
		}
		finally
		{
			// we know that we don't need the statement any longer, so to make
			// sure everything is cleaned up, we'll close it here
			this.done();
		}

		return result;
	}

	/**
	 * Extract the type (function, package, procedure) of the created object.
	 * @see #addExtendErrorInfo(workbench.db.WbConnection, String, workbench.sql.StatementRunnerResult)
	 */
	private String getObjectType(String sql)
	{
		String regex = "CREATE\\s*(OR\\s*REPLACE|)\\s*(PROCEDURE|FUNCTION|PACKAGE\\s*BODY|PACKAGE)\\s*(\\p{Graph}*)";
		String type = null;
		Matcher m = Pattern.compile(regex,Pattern.CASE_INSENSITIVE).matcher(sql);
		if (m.find() && m.groupCount() > 1) 
		{
			type = m.group(2).toUpperCase();
		}
    return type;
	}

	/**
	 * Extract the name of the created object for Oracle stored procedures.
	 * @see #addExtendErrorInfo(workbench.db.WbConnection, String, workbench.sql.StatementRunnerResult)
	 */
	private String getObjectName(String sql)
	{
		String regex = "CREATE\\s*(OR\\s*REPLACE|)\\s*(PROCEDURE|FUNCTION|PACKAGE\\s*BODY|PACKAGE)\\s*(\\p{Graph}*)";
		String name = null;
		Matcher m = Pattern.compile(regex,Pattern.CASE_INSENSITIVE).matcher(sql);
		if (m.find() && m.groupCount() > 2)
		{
			name = m.group(3);
			int pos = name.indexOf('(');
			if (pos > -1)
			{
				name = name.substring(0,pos).toUpperCase();
			}
		}
		return name;
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
    //String cleanSql = SqlUtil.makeCleanSql(sql, false).toUpperCase();
    String sqlverb = SqlUtil.getSqlVerb(sql);
    if (!"CREATE".equals(sqlverb)) return false;
    String type = getObjectType(sql);
    String name = getObjectName(sql);

		if (type == null || name == null) return false;

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

	/**
	 * PG's documentation shows CREATE FUNCTION samples that use
	 * a "dollar quoting" to avoid the nested single quotes
	 * e.g. http://www.postgresql.org/docs/8.0/static/plpgsql-structure.html
	 * but the JDBC driver does not (yet) understand this as well (this 
	 * seems to be only implemented in the psql command line tool
	 * So we'll replace the "dollar quotes" with regular single quotes
	 * Every single quote inside the function body will be replaced with 
	 * two single quotes in properly "escape" them
	 */
	private String fixPgDollarQuote(String sql)
	{
		Pattern createFunc = Pattern.compile("CREATE.*FUNCTION.*AS\\s*\\$", Pattern.CASE_INSENSITIVE);
		Matcher m = createFunc.matcher(sql);
		if (!m.find()) return sql;
		
		int start = sql.indexOf('$');
		int end = sql.indexOf('$', start + 1);
		String quote = sql.substring(start, end + 1);
		
		int startQuote = sql.indexOf(quote);
		int endQuote = sql.lastIndexOf(quote);
		String body = sql.substring(startQuote + quote.length(), endQuote);
		body = body.replaceAll("'", "''");
		
		StringBuffer newSql = new StringBuffer(sql.length() + 10);
		newSql.append(sql.substring(0, startQuote));
		newSql.append('\'');
		newSql.append(body);
		newSql.append('\'');
		newSql.append(sql.substring(endQuote + quote.length()));
		return newSql.toString();
	}
	
	public String getVerb()
	{
		return verb;
	}

}
