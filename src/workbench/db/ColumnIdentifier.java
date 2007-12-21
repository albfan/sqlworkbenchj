/*
 * ColumnIdentifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An object containing the definition for a table column.
 * @author  support@sql-workbench.net
 */
public class ColumnIdentifier
	implements DbObject, Comparable<ColumnIdentifier>
{
	private static final int NO_TYPE = Integer.MIN_VALUE;
	private String name;
	private int type = NO_TYPE;
	private boolean isPk;
	private boolean isExpression;
	private boolean isNullable = true;
	private boolean isUpdateable = true;
	private String dbmsType;
	private String comment;
	private String defaultValue;
	private String columnClassName;
	private Class columnClass;
	private String columnTypeName;

	private int position;

	private int size; // for VARCHAR etc
	private int digits; // for DECIMAL types
	private int hashCode;
	
	public ColumnIdentifier()
	{
	}

	public ColumnIdentifier(String aName)
	{
		this(aName, NO_TYPE, false);
	}

	public ColumnIdentifier(String aName, int aType)
	{
		this(aName, aType, false);
	}


	public ColumnIdentifier(String aName, int aType, boolean isPkColumn)
	{
		if (aName == null) throw new IllegalArgumentException("Column name may not be null!");
		setColumnName(aName.trim());
		this.type = aType;
		this.isPk = isPkColumn;
	}

	public String getSchema()
	{
		return null;
	}
	
	public String getCatalog()
	{
		return null;
	}
	
	public String getObjectName(WbConnection conn)
	{
		return conn.getMetadata().quoteObjectname(this.name);
	}
	
	public String getObjectExpression(WbConnection conn)
	{
		return getObjectName(conn);
	}
	
	public String getObjectType()
	{
		return "COLUMN";
	}
	
	public String getObjectName()
	{
		return getColumnName();
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
	
	public boolean isIdentityColumn()
	{
		if (this.dbmsType == null) return false;
		return (dbmsType.indexOf("identity") > -1);
	}

	/**
	 *	Define this column to be an expression.
	 * 
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

	/**
	 * Creates a deep copy of this ColumnIdentifier.
	 * @return a copy of this identifier
	 */
	public ColumnIdentifier createCopy()
	{
		ColumnIdentifier result = new ColumnIdentifier();
		result.name = this.name;
		result.hashCode = this.hashCode;
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

	public String getColumnName(WbConnection con)
	{
		if (con == null) return getColumnName();
		return con.getMetadata().quoteObjectname(name);
	}
	
	public String getColumnName()
	{
		return this.name;
	}

	/**
	 * Define the name of this column. 
	 * 
	 * This will also reset the PK and Nullable attributes. isPkColumn() 
	 * and isNullable() will return false after setting the name.
	 * 
	 * @param aName the (new) name for this identifier
	 */
	public void setColumnName(String aName)
	{
		this.name = aName;
		this.isExpression = false;
		this.isPk = false;
		this.isNullable = true;
		this.hashCode = (name == null ? -1 : StringUtil.trimQuotes(name).toLowerCase().hashCode());
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
	 * 
	 * @return the current datatype
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

	/**
	 * Compare two identifiers. 
	 * The comparison is only done on the name column and is case-insesitive. 
	 * 
	 * If the object is not a ColumnIdentifier it returns false
	 * 
	 * @param other the object to compare
	 * @return true if the other ColumnIdentifier has the same name
	 */
	public boolean equals(Object other)
	{
		try
		{
			ColumnIdentifier col = (ColumnIdentifier)other;
			return StringUtil.equalStringIgnoreCase(StringUtil.trimQuotes(this.name), StringUtil.trimQuotes(col.name));
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public int hashCode()
	{
		return hashCode;
	}
	
	public String getComment()
	{
		return comment;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	public int getPosition()
	{
		return position;
	}

	public void setPosition(int pos)
	{
		this.position = pos;
	}

	public String getColumnClassName()
	{
		return columnClassName;
	}

	public void setColumnClassName(String columnClass)
	{
		if (columnClass != null && columnClass.endsWith("[]"))
		{
			// Workaround for long[] 
			if (columnClass.startsWith("long"))
			{
				this.columnClassName = "[J";
			}
			else if (Character.isLowerCase(columnClass.charAt(0)))
			{
				// If it's a lower case class name we assume a native array type
				this.columnClassName = "[" + columnClass.toUpperCase().charAt(0);
			}
		}
		else
		{
			this.columnClassName = columnClass;
		}
		this.columnClass = null;
		if (this.columnClassName == null) return;

		try
		{
			this.columnClass = Class.forName(this.columnClassName);
		}
		catch (Exception e)
		{
			//LogMgr.logDebug("ColumnIdentifier.setColumnClassName()", "Could not obtain column class", e);
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

	public String getColumnTypeName()
	{
		if (this.columnTypeName == null)
		{
			return SqlUtil.getTypeName(this.type);
		}
		return this.columnTypeName;
	}

	public void setColumnTypeName(String columnTypeName)
	{
		this.columnTypeName = columnTypeName;
	}

	public boolean isUpdateable()
	{
		return isUpdateable;
	}

	public void setUpdateable(boolean isUpdateable)
	{
		this.isUpdateable = isUpdateable;
	}

	public int compareTo(ColumnIdentifier other)
	{
		if (other == null) return 1;
		if (this.name == null) return -1;
		return StringUtil.trimQuotes(name).compareToIgnoreCase(StringUtil.trimQuotes(other.name));
	}
	
	public static void sortByPosition(List<ColumnIdentifier> columnList)
	{
		Comparator<ColumnIdentifier> c = new Comparator<ColumnIdentifier>()
		{
			public int compare(ColumnIdentifier o1, ColumnIdentifier o2)
			{
				int pos1 = o1.getPosition();
				int pos2 = o2.getPosition();
				return pos1 - pos2;
			}
		};
		Collections.sort(columnList, c);
	}
}
