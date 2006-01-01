/*
 * ObjectDropper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import workbench.log.LogMgr;

/**
 * A help class to drop different types of objects
 * @author  support@sql-workbench.net
 */
public class ObjectDropper
{
	private List objectNames;
	private List objectTypes;
	private WbConnection connection;
	private boolean cascadeConstraints;

	public ObjectDropper(List names, List types)
		throws IllegalArgumentException
	{
		if (names == null || types == null) throw new IllegalArgumentException();
		if (names.size() == 0 || types.size() == 0) throw new IllegalArgumentException();
		if (names.size() != types.size()) throw new IllegalArgumentException();

		this.objectNames = names;
		this.objectTypes = types;
	}

	public void setConnection(WbConnection aConn)
	{
		this.connection = aConn;
	}

	public void execute()
		throws SQLException
	{
		Statement stmt = null;
		try
		{
			if (this.connection == null) throw new NullPointerException("No connection!");
			if (this.objectNames == null || this.objectNames.size() == 0) return;
			int count = this.objectNames.size();
			stmt = this.connection.createStatement();
			String cascade = null;

			for (int i=0; i < count; i++)
			{
				String name = (String)this.objectNames.get(i);
				String type = (String)this.objectTypes.get(i);
				// we assume that names with special characters are already quoted!

				StringBuffer sql = new StringBuffer(120);
				sql.append("DROP ");
				sql.append(type);
				sql.append(' ');
				sql.append(name);

				if (this.cascadeConstraints)
				{
					cascade = this.connection.getMetadata().getCascadeConstraintsVerb(type);
					if (cascade != null)
					{
						sql.append(' ');
						sql.append(cascade);
					}
				}
				LogMgr.logDebug("ObjectDropper.execute()", "Using SQL: " + sql);
				stmt.execute(sql.toString());
			}

			// Check if we need to commit the DDL statements
			if (!this.connection.getAutoCommit() && this.connection.getDdlNeedsCommit())
			{
				try { this.connection.commit(); } catch (Throwable th) {}
			}
		}
		catch (SQLException e)
		{
			if (!this.connection.getAutoCommit() && this.connection.getDdlNeedsCommit())
			{
				try { this.connection.rollback(); } catch (Throwable th) {}
			}
			throw e;
		}
		finally
		{
			try { stmt.close(); } catch (Throwable th) {}
		}
	}

	public void setCascadeConstraints(boolean flag)
	{
		this.cascadeConstraints = flag;
	}

}
