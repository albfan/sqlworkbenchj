/*
 * IndexColumn.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
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

	public String getColumn() { return this.column; }
	public String getDirection() { return this.direction; }
	
	public String getExpression()
	{
		if (StringUtil.isEmptyString(direction))
		{
			return this.column;
		}
		else
		{
			return this.column + " " + this.direction;
		}
	}
}
