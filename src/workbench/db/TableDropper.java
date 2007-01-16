/*
 * TableDropper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author support@sql-workbench.net
 */
public class TableDropper
{
	private WbConnection dbConnection;
	
	public TableDropper(WbConnection con)
	{
		this.dbConnection = con;
	}
	
	/**
	 * Drop given table. If this is successful and the
	 * DBMS requires a COMMIT for DDL statements then
	 * the DROP will be commited (or rolled back in case
	 * of an error
	 */
	public void dropTable(TableIdentifier aTable)
		throws SQLException
	{
		Statement stmt = null;
		DbMetadata meta = this.dbConnection.getMetadata();
		DbSettings dbs = meta.getDbSettings();
		
		try
		{
			StringBuilder sql = new StringBuilder();
			sql.append("DROP TABLE ");
			sql.append(aTable.getTableExpression(this.dbConnection));
			String cascade = dbs.getCascadeConstraintsVerb("TABLE");
			if (cascade != null)
			{
				sql.append(' ');
				sql.append(cascade);
			}
			stmt = this.dbConnection.createStatement();
			stmt.executeUpdate(sql.toString());
			if (this.dbConnection.shouldCommitDDL())
			{
				this.dbConnection.commit();
			}
		}
		catch (SQLException e)
		{
			if (this.dbConnection.shouldCommitDDL())
			{
				try { this.dbConnection.rollback(); } catch (Throwable th) {}
			}
			throw e;
		}
		finally
		{
			try { stmt.close(); } catch (Throwable th) {}
		}
	}	
}
