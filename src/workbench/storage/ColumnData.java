/*
 * ColumnData.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
 * The column definition is represented by a {@link workbench.db.ColumnIdentifier}
 * The value can be any Java object
 * @author support@sql-workbench.net
 */
public class ColumnData
{
	private Object data;
	private ColumnIdentifier id;
	
	/**
	 * Creates a new instance of ColumnData
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
}
