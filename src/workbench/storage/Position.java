/*
 * Position.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

/**
 * A class to identify the position of a row/column in a datastore
 * 
 * @author support@sql-workbench.net
 */
public class Position
{
	/**
	 * A Position instance identifying a non-existing position
	 */
	public static final Position NO_POSITION = new Position(-1, -1);
	
	private final int row;
	private final int column;
	
	public Position(int line, int col)
	{
		this.row = line;
		this.column = col;
	}
	
	public int getRow() { return row; }
	public int getColumn() { return column; }

	/**
	 * Check if this object identifies a valid position inside
	 * a DataStore or table. If either row or column are &lt; 0 
	 * this method returns false.
	 * @return true if row and column identify a non negative location
	 */
	public boolean isValid()
	{
		return (this.column > -1 && this.row > -1);
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Position)
		{
			Position op = (Position)other;
			return (op.column == this.column && op.row == this.row);
		}
		return false;
	}
	
	@Override
	public String toString()
	{
		return "[" + Integer.toString(row) + ","  + Integer.toString(column) + "]";
	}
	
	@Override
	public int hashCode()
	{
		int result = 37 * column;
		result ^= 37 * row;
		return result;
	}
	
}
