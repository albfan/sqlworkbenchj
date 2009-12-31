/*
 * IndexColumn.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class IndexColumn 
{
	private String column;
	private String direction;
	
  public IndexColumn(String col, String dir) 
  {
		this.column = col;
		this.direction = dir;
  }

	public void setColumn(String newName)
	{
		this.column = newName;
	}
	
	public String getColumn() 
	{ 
		return this.column; 
	}
	
	public String getDirection() 
	{ 
		if (this.direction == null) return null;
		
		// Map JDBC direction info to SQL standard
		if (direction.equalsIgnoreCase("a")) return "ASC";
		if (direction.equalsIgnoreCase("d")) return "DESC";
		
		return this.direction; 
	}
	
	public String getExpression()
	{
		if (StringUtil.isEmptyString(direction))
		{
			return this.column;
		}
		else
		{
			return this.column + " " + getDirection();
		}
	}
}
