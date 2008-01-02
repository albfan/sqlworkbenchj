/*
 * ProcedureCreator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import workbench.db.oracle.OraclePackageParser;
import workbench.log.LogMgr;
import workbench.sql.formatter.SQLLexer;
import workbench.sql.formatter.SQLToken;
import workbench.util.SqlUtil;

/**
 * @author support@sql-workbench.net
 */
public class ProcedureCreator
{
	private String sourceSql;
	private WbConnection dbConnection;
	private String procedureSchema;
	private String procedureName;
	private boolean isReplaceScript;
	
	public ProcedureCreator(WbConnection con, String procSchema, String procName, String procSource)
	{
		this.sourceSql = procSource;
		this.dbConnection = con;
		this.procedureSchema = procSchema;
		this.procedureName = procName;
	}
	
	private void dropIfNecessary()
		throws SQLException
	{
		if (this.isReplaceScript) return;
		
		StringBuilder sql = new StringBuilder(50);
		sql.append("DROP ");
		sql.append(getType());
		sql.append(' ');
		
		if (procedureSchema != null)
		{
			sql.append(procedureSchema);
			sql.append('.');
		}
		
		sql.append(procedureName);
		Statement stmt = null;
		try
		{
			stmt = this.dbConnection.createStatement();
			stmt.executeUpdate(sql.toString());
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
	}
	
	private String getType()
	{
		String type = null;
		try
		{
			SQLLexer lexer = new SQLLexer(this.sourceSql);
			SQLToken t = lexer.getNextToken(false, false);
			boolean nextTokenIsType = false;
			while (t != null)
			{
				String v = t.getContents();
				if ("CREATE".equals(v))
				{
					nextTokenIsType = true;
				}
				else if (nextTokenIsType && "OR".equalsIgnoreCase(v))
				{
					// waiting for REPLACE 
					nextTokenIsType = false;
				}
				else if ("REPLACE".equalsIgnoreCase(v))
				{
					isReplaceScript = true;
					nextTokenIsType = true;
				}
				else if (nextTokenIsType)
				{
					type = t.getContents();
					break;
				}
				t = lexer.getNextToken(false, false);
			}
		}
		catch (Exception e)
		{
			type = null;
		}
		return type;
	}
	
	public void recreate()
		throws SQLException
	{
		if (this.dbConnection.isBusy()) return;
		
		Statement stmt = null;
		try
		{
			this.dbConnection.setBusy(true);
			dropIfNecessary();
			stmt = this.dbConnection.createStatement();

			List<String> sqls = this.parseScript();
			Iterator itr = sqls.iterator();
			while (itr.hasNext())
			{
				String sql = this.dbConnection.getMetadata().filterDDL((String)itr.next());
				stmt.executeUpdate(sql);
			}

			if (dbConnection.shouldCommitDDL())
			{
				this.dbConnection.commit();
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError("ProcedureCreator.recreate()", "Error when recreating procedure", e);
			if (dbConnection.shouldCommitDDL())
			{
				try { this.dbConnection.rollback(); } catch (Throwable th) {}
			}
			throw e;
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
			this.dbConnection.setBusy(false);
		}
	}
	
	private List<String> parseScript()
	{
		List<String> result = new LinkedList<String>();
		if (this.dbConnection.getMetadata().isOracle())
		{
			OraclePackageParser parser = new OraclePackageParser(this.sourceSql);
			String sql = null;
		
			sql  = parser.getPackageDeclaration();
			if (sql != null) result.add(sql);
			sql = parser.getPackageBody();
			if (sql != null) result.add(sql);
			if (result.size() == 0)
			{
				// not package detected, use the whole script
				result.add(this.sourceSql);
			}
		}
		else
		{
			result.add(this.sourceSql);
		}
		return result;
	}

}
