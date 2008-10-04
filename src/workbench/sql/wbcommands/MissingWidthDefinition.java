/*
 * MissingWidthDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

/**
 *
 * @author support@sql-workbench.net
 */
public class MissingWidthDefinition
	extends Exception
{
	private String colname;

	public MissingWidthDefinition(String col)
	{
		super();
		colname = col;
	}

	public String getColumnName()
	{
		return colname;
	}

	@Override
	public String getMessage()
	{
		return "Missing or invalid width definition for: " + colname;
	}
}
