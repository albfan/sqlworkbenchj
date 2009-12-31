/*
 * ColumnData.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import workbench.db.ColumnIdentifier;

/**
 * A wrapper class do hold the current value of a column
 * and it's definition.
 * 
 * The column definition is represented by a {@link workbench.db.ColumnIdentifier}
 * 
 * The value can be any Java object
 * This is used by {@link workbench.storage.DmlStatement} to store the values
 * when creating PreparedStatements
 * 
 * @author Thomas Kellerer
 */
public class ColumnData
{
	final private Object data;
	final private ColumnIdentifier id;
	
	/**
	 * Creates a new instance of ColumnData
	 * 
	 * @param value The current value of the column
	 * @param colid The definition of the column
	 */
	public ColumnData(Object value, ColumnIdentifier colid)
	{
		data = value;
		id = colid;
	}
	
	public Object getValue() { return data; }
	public ColumnIdentifier getIdentifier() { return id; }
	
	public boolean isNull()
	{
		return (data == null);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null) return false;
		if (obj instanceof ColumnData) 
		{
			final ColumnData other = (ColumnData) obj;
			return this.id.equals(other.id);
		}
		else if (obj instanceof ColumnIdentifier)
		{
			return this.id.equals((ColumnIdentifier)obj);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		return id.hashCode();
	}
	
	public String toString()
	{
		return id.getColumnName() + " = " + (data == null ? "NULL" : data.toString());
	}
	
}
