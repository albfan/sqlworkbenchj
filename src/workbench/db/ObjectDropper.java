/*
 * ObjectDropper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import workbench.exception.NoConnectionException;
import workbench.log.LogMgr;

/**
 *
 * @author  info@sql-workbench.net
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
		this(names, types, false);
	}

	public ObjectDropper(List names, List types, boolean resolveDependencies)
		throws IllegalArgumentException
	{
		if (names == null || types == null) throw new IllegalArgumentException();
		if (names.size() == 0 || types.size() == 0) throw new IllegalArgumentException();
		if (names.size() != types.size()) throw new IllegalArgumentException();

		this.objectNames = names;
		this.objectTypes = types;
		if (resolveDependencies)
		{
			this.reorderObjects();
		}
	}

	public void setConnection(WbConnection aConn)
	{
		this.connection = aConn;
	}

	/**
	 *	This method reorders the element in objectNames (and in objectTypes)
	 *	so that any contraint dependencies can be resolved
	 */
	private void reorderObjects()
	{
	}

	public void execute()
		throws NoConnectionException, SQLException
	{
		if (this.connection == null) throw new NoConnectionException("Please specify a connection!");
		if (this.objectNames == null || this.objectNames.size() == 0) return;
		int count = this.objectNames.size();
		Statement stmt = this.connection.createStatement();
		String cascade = null;

		/*
		boolean doCascade = false;
		if (this.cascadeConstraints)
		{
			cascade = this.connection.getMetadata().getCascadeConstraintsVerb();
		}

		doCascade = (cascade != null && cascade.length() > 0);
		*/
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
			
			if (this.cascadeConstraints && "TABLE".equalsIgnoreCase(type))
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
		stmt.close();
		// Postgres and Firebird requires a COMMIT if autocommit is set to false...
		if (!this.connection.getAutoCommit() && this.connection.getDdlNeedsCommit())
		{
			try { this.connection.commit(); } catch (Throwable th) {}
		}
	}

	/** Getter for property cascadeConstraints.
	 * @return Value of property cascadeConstraints.
	 *
	 */
	public boolean isCascadeConstraints()
	{
		return cascadeConstraints;
	}

	/** Setter for property cascadeConstraints.
	 * @param aFlag New value of property cascadeConstraints.
	 *
	 */
	public void setCascadeConstraints(boolean aFlag)
	{
		this.cascadeConstraints = aFlag;
	}

}
