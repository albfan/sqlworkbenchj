/*
 * ColumnIdentifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An object containing the definition for a table column.
 * @author  Thomas Kellerer
 */
public class ColumnIdentifier
	implements DbObject, Comparable<ColumnIdentifier>
{
	private String name;
	private String alias;
	private int type = Types.OTHER;
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
	private String sourceTable;
	private boolean autoincrement;

	/**
	 * Stores the definition of "computed" columns (e.g. Firebird, SQL Server, DB2)
	 */
	private String expression;

	private int displaySize = -1;
	private int position;

	private int size; // for VARCHAR etc
	private int digits; // for DECIMAL types
	private int hashCode;

	public ColumnIdentifier()
	{
	}

	public ColumnIdentifier(String aName)
	{
		this(aName, Types.OTHER, false);
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

	public boolean isAutoincrement()
	{
		return autoincrement;
	}

	public void setIsAutoincrement(boolean flag)
	{
		autoincrement = flag;
	}

	/**
	 * Set the name of the source table if this column was used
	 * in a query.
	 * @param name
	 */
	public void setSourceTableName(String name)
	{
		sourceTable = name;
	}

	/**
	 * Returns the name of the table if this ColumnIdentifier was created
	 * from a query
	 */
	public String getSourceTableName()
	{
		return sourceTable;
	}

	public String getColumnAlias()
	{
		return alias;
	}

	public void setColumnAlias(String label)
	{
		alias = label;
	}

	public String getComputedColumnExpression()
	{
		return expression;
	}

	public void setComputedColumnExpression(String expr)
	{
		this.expression = expr;
	}

	/**
	 * Returns the column alias if not null, otherwise the column's name.
	 *
	 * @return the column display name.
	 */
	public String getDisplayName()
	{
		if (alias == null) return name;
		return alias;
	}

	public String getSchema()
	{
		return null;
	}

	public String getCatalog()
	{
		return null;
	}

	@Override
	public String getDropStatement(WbConnection con, boolean cascade)
	{
		return null;
	}

	@Override
	public String getObjectNameForDrop(WbConnection con)
	{
		return getObjectName(con);
	}

	@Override
	public String getObjectName(WbConnection conn)
	{
		if (conn == null) return SqlUtil.quoteObjectname(name);
		return conn.getMetadata().quoteObjectname(this.name);
	}

	public String getObjectExpression(WbConnection conn)
	{
		return getObjectName(conn);
	}

	public String getFullyQualifiedName(WbConnection conn)
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

	public CharSequence getSource(WbConnection con)
	{
		return this.name + " " + this.dbmsType;
	}

	/**
	 *	Define the size for this column (e.g. for VARCHAR columns)
	 */
	public void setColumnSize(int aSize)
	{
		this.size = aSize;
	}

	/**
	 * The data size of this columne (e.g. for VARCHAR columns)
	 */
	public int getColumnSize()
	{
		return this.size;
	}

	public void setDisplaySize(int size)
	{
		this.displaySize = size;
	}

	/**
	 * The display size of this column as reported by the JDBC driver.
	 *
	 * For some types (e.g Integer) some sensible sizes are used
	 * instead of the JDBC driver supplied values, as this method is
	 * currently only used to "format" the output for the Console interface
	 *
	 * @return the recommended display size of the column
	 */
	public int getDisplaySize()
	{
		if (SqlUtil.isIntegerType(type)) return 10;
		if (SqlUtil.isDecimalType(type, size, digits)) return 15;
		if (SqlUtil.isCharacterType(type)) return size;
		if (SqlUtil.isBlobType(type)) return 5;

		if (displaySize < 0) return size;
		return this.displaySize;
	}

	/**
	 *	Define the decimal digits for this column (e.g. for DECIMAL columns)
	 */
	public void setDecimalDigits(int numDigits)
	{
		this.digits = numDigits < 0 ? -1 : numDigits;
	}

	public int getDecimalDigits()
	{
		return this.digits;
	}

	public String getDigitsDisplay()
	{
		if (digits < 0) return "";
		return NumberStringCache.getNumberString(digits);
	}

	public void setIsPkColumn(boolean flag) { this.isPk = flag; }
	public boolean isPkColumn() { return this.isPk; }

	public void setIsNullable(boolean flag) { this.isNullable = flag; }
	public boolean isNullable() { return this.isNullable; }

	public void setDbmsType(String dbType) { this.dbmsType = dbType; }
	public String getDbmsType() { return this.dbmsType; }

	public boolean isIdentityColumn()
	{
		if (this.dbmsType == null) return false;
		return (dbmsType.indexOf("identity") > -1);
	}

	/**
	 *	Define this column to be an expression.
	 */
	public void setExpression(String anExpression)
	{
		this.name = anExpression;
		this.isExpression = true;
		this.isPk = false;
		this.type = Types.OTHER;
	}

	/**
	 * Creates a deep copy of this ColumnIdentifier.
	 * 
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
		result.columnClass = this.columnClass;
		result.columnTypeName = this.columnTypeName;
		result.position = this.position;
		result.displaySize = this.displaySize;
		result.expression = this.expression;
		result.alias = this.alias;
		
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
		return this.type;
	}

	public String toString()
	{
		return this.name;
	}

	/**
	 * Compare two identifiers.
	 * The comparison is only done on the name column and is case-insensitive.
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

	public void setComment(String cmt)
	{
		this.comment = cmt;
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public void setDefaultValue(String value)
	{
		this.defaultValue = value;
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

	public void setColumnClassName(String colClass)
	{
		if (colClass != null && colClass.endsWith("[]"))
		{
			// Workaround for long[]
			if (colClass.startsWith("long"))
			{
				this.columnClassName = "[J";
			}
			else if (Character.isLowerCase(colClass.charAt(0)))
			{
				// If it's a lower case class name we assume a native array type
				this.columnClassName = "[" + colClass.toUpperCase().charAt(0);
			}
		}
		else
		{
			this.columnClassName = colClass;
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

	public void setColumnTypeName(String colTypeName)
	{
		this.columnTypeName = colTypeName;
	}

	public boolean isUpdateable()
	{
		return isUpdateable;
	}

	public void setUpdateable(boolean update)
	{
		this.isUpdateable = update;
	}

	public void adjustQuotes(QuoteHandler source, QuoteHandler target)
	{
		if (source.isQuoted(this.name))
		{
			String newName = source.removeQuotes(name);
			this.name = target.quoteObjectname(newName);
		}
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

	public static int getMaxNameLength(List<ColumnIdentifier> columns)
	{
		int maxLength = 0;
		for (ColumnIdentifier col : columns)
		{
			if (col.getColumnName().length() > maxLength)
			{
				maxLength = col.getColumnName().length();
			}
		}
		return maxLength;
	}

}
