/*
 * DependencyCycleException.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db.importer;

/**
 *
 * @author support@sql-workbench.net
 */
public class DependencyCycleException
	extends Exception
{
	public String tablename;
	public DependencyCycleException(String causedBy)
	{
		super("Cyclic dependency deteteted for table: " + causedBy);
		tablename = causedBy;
	}
	
	public String getOffendingTablename()
	{
		return tablename;
	}
}
