/*
 * ColumnIdentifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 * An object containing the definition for a table column.
 * @author  support@sql-workbench.net
 */
public class ColumnIdentifier
	implements Comparable
{
	public static final int NO_TYPE = Integer.MIN_VALUE;
	private String name;
	private int type;
	private boolean isPk = false;
	private boolean isExpression = false;
	private boolean isNullable = true;
	private boolean isUpdateable = true;
	private String dbmsType = null;
	private String comment = null;
	private String defaultValue = null;
	private String columnClassName = null;
	private Class columnClass;
	private String columnTypeName = null;

	private int position;

	private int size; // for VARCHAR etc
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
		this.name = aName.trim();
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

	public void setDbmsType(String type) { this.dbmsType = type; }
	public String getDbmsType() { return this.dbmsType; }

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
		result.dbmsType = this.dbmsType;
		result.isUpdateable = this.isUpdateable;
		result.comment = this.comment;
		result.defaultValue = this.defaultValue;
		result.columnClassName = this.columnClassName;
		result.columnTypeName = this.columnTypeName;
		result.position = this.position;
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

	/**
	 *	Set the JDBC datatype.
	 *
	 *	@see java.sql.Types
	 */
	public void setDataType(int aType)
	{
		this.type = aType;
	}

	/**
	 *	Returns the java.sql.Types data type as returned
	 *  by the jdbc driver. If no type has been defined
	 *  Types.OTHER will be returned
	 */
	public int getDataType()
	{
		if (type == NO_TYPE) return Types.OTHER;
		return this.type;
	}

	public String toString()
	{
		return this.name;
	}

	public boolean equals(Object o)
	{
		if (o instanceof ColumnIdentifier)
		{
			ColumnIdentifier other = (ColumnIdentifier)o;
			if (this.type == NO_TYPE || other.type == NO_TYPE)
			{
				return this.name.equalsIgnoreCase(other.name);
			}
			return (this.type == other.type && this.name.equalsIgnoreCase(other.name));
		}
		return false;
	}

	public int hashCode()
	{
		return this.name.hashCode();
	}
	
	/**
	 * Getter for property comment.
	 * @return Value of property comment.
	 */
	public String getComment()
	{
		return comment;
	}

	/**
	 * Setter for property comment.
	 * @param comment New value of property comment.
	 */
	public void setComment(String comment)
	{
		this.comment = comment;
	}

	/**
	 * Getter for property defaultValue.
	 * @return Value of property defaultValue.
	 */
	public String getDefaultValue()
	{
		return defaultValue;
	}

	/**
	 * Setter for property defaultValue.
	 * @param defaultValue New value of property defaultValue.
	 */
	public void setDefaultValue(String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	/**
	 * Getter for property position.
	 * @return Value of property position.
	 */
	public int getPosition()
	{
		return position;
	}

	/**
	 * Setter for property position.
	 * @param position New value of property position.
	 */
	public void setPosition(int position)
	{
		this.position = position;
	}

	/**
	 * Getter for property columnClassName.
	 * 
	 * @return Value of property columnClassName.
	 */
	public String getColumnClassName()
	{
		return columnClassName;
	}

	/**
	 * Setter for property columnClassName.
	 * 
	 * @param columnClass New value of property columnClassName.
	 */
	public void setColumnClassName(String columnClass)
	{
		this.columnClassName = columnClass;
		this.columnClass = null;
		if (this.columnClassName == null) return;

		try
		{
			this.columnClass = Class.forName(this.columnClassName);
		}
		catch (Exception e)
		{
			LogMgr.logError("ColumnIdentifier.setColumnClassName()", "Could not obtain column class", e);
			this.columnClass = null;
		}

	}
	
	public Class getColumnClass()
	{
		if (this.columnClass != null) return this.columnClass;
		
		switch (this.type)
		{
			case Types.BIGINT:
			case Types.INTEGER:
				return Long.class;
				
			case Types.SMALLINT:
				return Integer.class;
				
			case Types.NUMERIC:
			case Types.DECIMAL:
				return BigDecimal.class;
				
			case Types.DOUBLE:
				return Double.class;
				
			case Types.REAL:
			case Types.FLOAT:
				return Float.class;

			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				return String.class;
				
			case Types.DATE:
				return java.sql.Date.class;
				
			case Types.TIMESTAMP:
				return Timestamp.class;
				
			default:
				return Object.class;
		}

	}

	/**
	 * Getter for property columnTypeName.
	 * @return Value of property columnTypeName.
	 */
	public String getColumnTypeName()
	{
		if (this.columnTypeName == null)
		{
			return SqlUtil.getTypeName(this.type);
		}
		return this.columnTypeName;
	}

	/**
	 * Setter for property columnTypeName.
	 * @param columnTypeName New value of property columnTypeName.
	 */
	public void setColumnTypeName(String columnTypeName)
	{
		this.columnTypeName = columnTypeName;
	}

	/**
	 * Getter for property isUpdateable.
	 * @return Value of property isUpdateable.
	 */
	public boolean isUpdateable()
	{
		return isUpdateable;
	}

	/**
	 * Setter for property isUpdateable.
	 * @param isUpdateable New value of property isUpdateable.
	 */
	public void setUpdateable(boolean isUpdateable)
	{
		this.isUpdateable = isUpdateable;
	}

	public int compareTo(Object other)
	{
		if (other == null) return 1;
		if (this.name == null) return -1;
		if (other instanceof ColumnIdentifier)
		{
			ColumnIdentifier c = (ColumnIdentifier)other;
			return this.name.compareToIgnoreCase(c.name);
		}
		return 1;
	}
}
