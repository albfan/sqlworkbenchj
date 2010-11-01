/*
 * TableNotFoundException
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.sql.SQLException;

/**
 *
 * @author Thomas Kellerer
 */
public class TableNotFoundException
	extends SQLException
{
	private String tableName;

	public TableNotFoundException(String name)
	{
		super("Table " + name + " not found");
		tableName = name;
	}

	public String getTableName()
	{
		return tableName;
	}
}
