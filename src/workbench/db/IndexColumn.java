/*
 * IndexColumn.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.Comparator;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class IndexColumn
{
	private String column;
	private String direction;
	private int sequence;

  public IndexColumn(String col, int colSequence)
  {
		this.column = col;
		this.sequence = colSequence;
  }

  public IndexColumn(String col, String dir)
  {
		this.column = StringUtil.trim(col);
		this.direction = dir;
  }

	public void setColumn(String newName)
	{
		this.column = StringUtil.trim(newName);
	}

	public String getColumn()
	{
		return this.column;
	}

	public void setDirection(String dir)
	{
		this.direction = dir;
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

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof IndexColumn)
		{
			IndexColumn otherCol = (IndexColumn)other;
			return StringUtil.equalString(column, otherCol.column) && StringUtil.equalStringIgnoreCase(getDirection(), otherCol.getDirection());
		}
		else
		{
			return false;
		}
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 73 * hash + (this.column != null ? this.column.hashCode() : 0);
		hash = 73 * hash + (this.direction != null ? this.direction.hashCode() : 0);
		return hash;
	}

	public static Comparator<IndexColumn> getSequenceSorter()
	{
		return new Comparator<IndexColumn>()
		{
			@Override
			public int compare(IndexColumn o1, IndexColumn o2)
			{
				return o1.sequence - o2.sequence;
			}
		};
	}

	@Override
	public String toString()
	{
		return column;
	}

}
