/*
 * ViewDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

/**
 * @author support@sql-workbench.net
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
