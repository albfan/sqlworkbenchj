/*
 * ViewDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

/**
 * @author Thomas Kellerer
 */
public class ViewDefinition
	extends TableIdentifier
{
	public ViewDefinition(String name)
	{
		super(name);
		this.setType("VIEW");
	}

	public ViewDefinition(String schema, String name)
	{
		super(schema, name);
		this.setType("VIEW");
	}
	
}
