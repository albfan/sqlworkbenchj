/*
 * ColumnIdentifier.java
 *
 * Created on December 20, 2003, 1:18 PM
 */

package workbench.db;

import java.sql.Types;

/**
 *
 * @author  workbench@kellerer.org
 */
public class ColumnIdentifier
{
	private String name;
	private int type;
	private boolean isPk = false;
	/**
	 *	Create a ColummIdentifier where the type is ignored.
	 */
	public ColumnIdentifier(String aName)
	{
		this.name = aName;
		this.type = Integer.MIN_VALUE;
		this.isPk = false;
	}
	
	public ColumnIdentifier(String aName, int aType)
	{
		this(aName, aType, false);
	}
	public ColumnIdentifier(String aName, int aType, boolean isPkColumn)
	{
		if (aName == null) throw new IllegalArgumentException("Column name may not be null!");
		this.name = aName.trim().toLowerCase();
		this.type = aType;
		this.isPk = isPkColumn;
	}
	
	public void setIsPkColumn(boolean flag) { this.isPk = flag; }
	public boolean isPkColumn() { return this.isPk; }
	
	public String getColumnName()
	{
		return this.name;
	}
	public int getDataType()
	{
		if (type == Integer.MIN_VALUE) return Types.OTHER;
		return this.type;
	}
	
	public String toString()
	{ 
		return this.name; 
	}
	
	public boolean equals(Object other)
	{
		if (other instanceof ColumnIdentifier)
		{
			ColumnIdentifier cd = (ColumnIdentifier)other;
			if (this.type == Integer.MIN_VALUE || cd.type == Integer.MIN_VALUE)
			{
				return this.name.equals(cd.name);
			}
			return this.type == cd.type && this.name.equals(cd.name);
		}
		return false;
	}
	
}