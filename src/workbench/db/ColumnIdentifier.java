/*
 * ColumnIdentifier.java
 *
 * Created on December 20, 2003, 1:18 PM
 */

package workbench.db;

import java.sql.Types;

/**
 * An object containing the definition for a table column.
 * @author  workbench@kellerer.org
 */
public class ColumnIdentifier
{
	public static final int NO_TYPE = Integer.MIN_VALUE;
	private String name;
	private int type;
	private boolean isPk = false;
	private boolean isExpression = false;
	private boolean isNullable = true;
	
	private int size; // for VARCHAR's etc
	private int digits; // for DECIMAL types
	
	public ColumnIdentifier()
	{
	}
	
	/**
	 *	Create a ColummIdentifier where the type is ignored.
	 */
	public ColumnIdentifier(String aName)
	{
		this(aName, NO_TYPE, false);
	}

	/**
	 *	Create a ColumnIdentifier with a given data type (from java.sql.Types)
	 */
	public ColumnIdentifier(String aName, int aType)
	{
		this(aName, aType, false);
	}

	
	/**
	 *	Create a ColumnIdentifier for a primary key column with a given data type (from java.sql.Types)
	 */
	public ColumnIdentifier(String aName, int aType, boolean isPkColumn)
	{
		if (aName == null) throw new IllegalArgumentException("Column name may not be null!");
		this.name = aName.trim().toLowerCase();
		this.type = aType;
		this.isPk = isPkColumn;
	}

	/**
	 *	Define the size for this column (e.g. for VARCHAR columns)
	 */
	public void setColumnSize(int aSize) { this.size = aSize; }
	public int getColumnSize() { return this.size; }
	
	/**
	 *	Define the decimal digits for this column (e.g. for DECIMAL columns)
	 */
	public void setDecimalDigits(int numDigits) { this.digits = numDigits; }
	public int getDecimalDigits() { return this.digits; }
	
	public void setIsPkColumn(boolean flag) { this.isPk = flag; }
	public boolean isPkColumn() { return this.isPk; }

	public void setIsNullable(boolean flag) { this.isNullable = flag; }
	public boolean isNullable() { return this.isNullable; }
	
	/**
	 *	Define this column to be an expression. 
	 *	The major difference to setColumnName() is, that the name will internally
	 *  not be stored in lowercase
	 *  (But can be used in a SELECT anyway)
	 */
	public void setExpression(String anExpression)
	{
		this.name = anExpression;
		this.isExpression = true;
		this.isPk = false;
		this.type = NO_TYPE;
	}
	
	public ColumnIdentifier createCopy()
	{
		ColumnIdentifier result = new ColumnIdentifier();
		result.name = this.name;
		result.digits = this.digits;
		result.isExpression = this.isExpression;
		result.isNullable = this.isNullable;
		result.isPk = this.isPk;
		result.size = this.size;
		result.type = this.type;
		return result;
	}
	
	public String getColumnName()
	{
		return this.name;
	}
	
	public void setColumnName(String aName)
	{
		this.name = aName;
		this.isExpression = false;
		this.isPk = false;
		this.isNullable = true;
	}
	
	public void setDataType(int aType)
	{
		this.type = aType;
	}
	
	public int getDataType()
	{
		if (type == NO_TYPE) return Types.OTHER;
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
			if (this.type == NO_TYPE || cd.type == NO_TYPE)
			{
				return this.name.equals(cd.name);
			}
			return this.type == cd.type && this.name.equals(cd.name);
		}
		return false;
	}
	
}