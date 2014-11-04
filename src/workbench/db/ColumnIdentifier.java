/*
 * ColumnIdentifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import workbench.db.objectcache.DbObjectCacheFactory;

import workbench.storage.ResultInfo;

import workbench.util.NumberStringCache;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An object containing the definition for a table or resultset column.
 *
 * @author  Thomas Kellerer
 */
public class ColumnIdentifier
	implements ComparableDbObject, Comparable<ColumnIdentifier>, Serializable
{
	private static final long serialVersionUID = DbObjectCacheFactory.CACHE_VERSION_UID;

	public static final int NO_TYPE_INFO = Integer.MIN_VALUE;

	private String name;
	private String alias;
	private int type = Types.OTHER;
	private boolean isPk;
	private boolean isExpression;
	private boolean isNullable = true;
	private boolean isUpdateable = true;
	private boolean readOnly;
	private String dbmsType;
	private String comment;
	private String defaultValue;
	private String columnClassName;
	private Class columnClass;
	private String columnTypeName;
	private String sourceTable;
	private boolean autoincrement;
	private String collation;
	private String collationExpression;
	private String generatorExpression;
	private String columnConstraint;

	// For procedure columns (=arguments)
	private String argumentMode;

	// an additional "modifier" to be used instead of the "DEFAULT" keyword
	// currently only used for Oracle's "DEFAULT ON NULL"
	private String defaultClause;
	/**
	 * Stores the definition of "computed" columns (e.g. Oracle, Firebird, SQL Server, DB2)
	 */
	private String expression;

	/**
	 * Used to identify the storage type for a column in Postgres
	 */
	private int pgStorage;

	private int displaySize = -1;

	/** The position of the column in the table or result set */
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

	public void setPgStorage(int type)
	{
		this.pgStorage = type;
	}

	public int getPgStorage()
	{
		return pgStorage;
	}

	/**
	 * Returns the SQL for a constraint defined on this column.
	 */
	public String getConstraint()
	{
		return columnConstraint;
	}

	public void setConstraint(String constraint)
	{
		this.columnConstraint = constraint;
	}


	/**
	 * Returns the keyword that should be used to define the default value.
	 *
	 * Usually this is "DEFAULT", but can be overwritten to e.g. support Oracle 12c's "DEFAULT ON NULL" option.
	 *
	 * @return the SQL clause to define a default value
	 * @see #setDefaultClause(java.lang.String)
	 */
	public String getDefaultClause()
	{
		if (defaultClause == null) return "DEFAULT";
		return defaultClause;
	}

	public void setArgumentMode(String mode)
	{
		this.argumentMode = mode;
	}

	public String getArgumentMode()
	{
		return argumentMode;
	}

	/**
	 * Change the SQL clause to define a default value to be different than "DEFAULT".
	 *
	 * @param clause the new clause that can be used instead of "DEFAULT"
	 */
	public void setDefaultClause(String clause)
	{
		if (StringUtil.isBlank(clause))
		{
			this.defaultClause = null;
		}
		else
		{
			this.defaultClause = clause.trim();
		}
	}

	/**
	 * Returns a DBMS dependent expression to define the column's collation.
	 *
	 * This is an expression that can be used inside a CREATE TABLE statement
	 * @see #getCollation()
	 */
	public String getCollationExpression()
	{
		return collationExpression;
	}

	public void setCollationExpression(String expression)
	{
		this.collationExpression = expression;
	}

	/**
	 * Returns the raw name of the column's collation.
	 *
	 * This is usually not an expression that can be used to reconstruct the column definition.
	 *
	 * @see #getCollationExpression()
	 */
	public String getCollation()
	{
		return collation;
	}

	public void setCollation(String collationName)
	{
		this.collation = collationName;
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
	 * Set the name of the source table if this column was used in a query.
	 *
	 * @param name
	 */
	public void setSourceTableName(String name)
	{
		if (StringUtil.isNonEmpty(name))
		{
			sourceTable = name;
		}
		else
		{
			sourceTable = null;
		}
	}

	/**
	 * Returns the name of the table if this ColumnIdentifier was created
	 * from a query
	 */
	public String getSourceTableName()
	{
		return sourceTable;
	}

	/**
	 * Return the column alias used in the SQL.
	 *
	 * This requires the JDBC driver to return different identifiers for the
	 * column name and the alias if one is used.
	 *
	 * @return the column alias if used
	 * @see ResultInfo#ResultInfo(java.sql.ResultSetMetaData, workbench.db.WbConnection)
	 */
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

	public String getGeneratorExpression()
	{
		return generatorExpression;
	}

	public void setGeneratorExpression(String expr)
	{
		this.generatorExpression = expr;
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

	/**
	 * The schema of the column.
	 *
	 * @return always null
	 */
	@Override
	public String getSchema()
	{
		return null;
	}

	/**
	 * The catalog of the column.
	 *
	 * @return always null
	 */
	@Override
	public String getCatalog()
	{
		return null;
	}

	/**
	 * @return always null
	 */
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

	@Override
	public String getObjectExpression(WbConnection conn)
	{
		return getObjectName(conn);
	}

	@Override
	public String getFullyQualifiedName(WbConnection conn)
	{
		return getObjectName(conn);
	}

	@Override
	public String getObjectType()
	{
		return "COLUMN";
	}

	@Override
	public String getObjectName()
	{
		return getColumnName();
	}

	@Override
	public CharSequence getSource(WbConnection con)
	{
		return this.name + " " + this.dbmsType;
	}

	/**
	 *	Define the size for this column (e.g. for VARCHAR columns)
	 *
	 * @param colSize  the size of the column
	 */
	public void setColumnSize(int colSize)
	{
		this.size = colSize;
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
		if (SqlUtil.isBlobType(type)) return 5;  // the console will display "(BLOB)"

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

	public void setIsPkColumn(boolean flag)
	{
		this.isPk = flag;
	}

	public boolean isPkColumn()
	{
		return this.isPk;
	}

	public void setIsNullable(boolean flag)
	{
		this.isNullable = flag;
	}

	public boolean isNullable()
	{
		return this.isNullable;
	}

	/**
	 * Define the DBMS data type as reported by the JDBC driver.
	 *
	 * @param dbType the column's data type
	 */
	public void setDbmsType(String dbType)
	{
		this.dbmsType = dbType;
	}

	/**
	 * Return the DBMS data type as reported by the JDBC driver.
	 *
	 * @return the column's data type
	 */
	public String getDbmsType()
	{
		return this.dbmsType;
	}

	public boolean isAutoGenerated()
	{
		return isAutoincrement() || isIdentityColumn();
	}

	public boolean isIdentityColumn()
	{
		if (!SqlUtil.isNumberType(this.type)) return false;

		// SQL Server
		if (this.dbmsType != null && dbmsType.toLowerCase().contains("identity"))
		{
			return true;
		}

		// certain DB2 versions
		if (defaultValue != null && defaultValue.toLowerCase().contains("identity"))
		{
			return true;
		}

		// HSQLDB and maybe DB2
		if (expression != null && expression.toLowerCase().contains("identity"))
		{
			return true;
		}

		return false;
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
		result.defaultClause = this.defaultClause;
		result.readOnly = this.readOnly;
		result.argumentMode = this.argumentMode;
		result.columnConstraint = this.columnConstraint;
		result.collation = this.collation;
		result.collationExpression = this.collationExpression;
		result.generatorExpression = this.generatorExpression;
		return result;
	}

	/**
	 * Return the column's name including any quoting if necessary.
	 *
	 * @param con the connection to be used, may be null
	 * @return the column's name, quote approriately
	 * @see DbMetadata#quoteObjectname(java.lang.String)
	 */
	public String getColumnName(WbConnection con)
	{
		QuoteHandler handler = (con == null ? QuoteHandler.STANDARD_HANDLER : con.getMetadata());
		return handler.quoteObjectname(name);
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
		this.hashCode = (name == null ? -1 : SqlUtil.removeObjectQuotes(name).toLowerCase().hashCode());
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

	@Override
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
	@Override
	public boolean equals(Object other)
	{
		if (other == null) return false;

		try
		{
			ColumnIdentifier col = (ColumnIdentifier)other;
			return StringUtil.equalStringIgnoreCase(SqlUtil.removeObjectQuotes(this.name), SqlUtil.removeObjectQuotes(col.name));
		}
		catch (Exception e)
		{
			return false;
		}
	}

	@Override
	public int hashCode()
	{
		return hashCode;
	}

	@Override
	public String getComment()
	{
		return comment;
	}

	@Override
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

	/**
	 * The position of the column in the table or ResultSet.
	 *
	 * @return the position of the column. The first column has the position 1 (one)<br/>
	 *         0 if the position is unknown
	 */
	public int getPosition()
	{
		return position;
	}

	/**
	 * Sets the position of the column in the table or the ResultSet.
	 *
	 * The first column must have 1 as the position.
	 * 0 indicates that the position is unknown
	 *
	 * @param pos the position to use.
	 */
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

	/**
	 * Return the readonly flag as returned by the JDBC driver for this column.
	 * @see #isUpdateable()
	 */
	public boolean isReadonly()
	{
		return readOnly;
	}

	/**
	 * Sets the readonly flag as returned by the JDBC driver.
	 *
	 * This might contain different information than the isUpdateable() flag!
	 */
	public void setReadonly(boolean flag)
	{
		readOnly = flag;
	}

	/**
	 * Returns if this column is theoretically updateable.
	 *
	 * This flag is managed by ourselves and can contain different information than isReadonly();
	 *
	 * @see #isReadonly()
	 */
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

	@Override
	public int compareTo(ColumnIdentifier other)
	{
		if (other == null) return 1;
		if (this.name == null) return -1;
		return SqlUtil.removeObjectQuotes(name).compareToIgnoreCase(SqlUtil.removeObjectQuotes(other.name));
	}

	/**
	 * Check if the passed column is contained in the list of columns.
	 *
	 * If the columns contain a position > 0 then the position is taken into account
	 * when checking if the column is in the list. Otherwise only the name will be checked.
	 *
	 * @param columns  the list of columns to search
	 * @param toFind   the column to find
	 * @return true if the column is contained in the list.
	 */
	public static boolean containsColumn(List<ColumnIdentifier> columns, ColumnIdentifier toFind)
	{
		if (columns == null) return false;
		if (toFind == null) return false;
		if (columns.isEmpty()) return false;

		for (ColumnIdentifier col : columns)
		{
			if (col.getPosition() > 0 && toFind.getPosition() > 0)
			{
				// make sure to use equals() to compare the column names in order
				// to take care of quotes and case-sensitivity
				if (col.getPosition() == toFind.getPosition() && col.equals(toFind)) return true;
			}
			else
			{
				if (col.equals(toFind)) return true;
			}
		}
		return false;
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

	@Override
	public boolean isComparableWith(DbObject other)
	{
		return (other instanceof ColumnIdentifier);
	}

	@Override
	public boolean isEqualTo(DbObject other)
	{
		if (other instanceof ColumnIdentifier)
		{
			ColumnIdentifier otherCol = (ColumnIdentifier)other;
			if (!StringUtil.equalStringOrEmpty(this.dbmsType, otherCol.dbmsType, true)) return false;
			if (this.isNullable != otherCol.isNullable) return false;
			if (this.isExpression != otherCol.isExpression) return false;
			if (this.isPk != otherCol.isPk) return false;
			if (!StringUtil.equalStringOrEmpty(this.defaultValue, otherCol.defaultValue)) return false;
			return true;
		}
		return false;
	}

	public static ColumnIdentifier findColumnInList(List<ColumnIdentifier> columns, String colname)
	{
		if (columns == null) return null;
		if (colname == null) return null;

		String toTest = SqlUtil.removeObjectQuotes(colname);
		for (ColumnIdentifier col : columns)
		{
			String name = SqlUtil.removeObjectQuotes(col.getColumnName());
			if (name.equalsIgnoreCase(toTest)) return col;
		}
		return null;
	}

	/**
	 * Resturns a new list where the PK columns are sorted first.
	 *
	 * The columns keep their original order inside each "block".
	 *
	 * The passed list will not be changed.
	 *
	 * @param columns  the columns to sort.
	 * @return a new list where the PK columns are at the beginning.
	 */
	public static List<ColumnIdentifier> sortPksFirst(List<ColumnIdentifier> columns)
	{
		List<ColumnIdentifier> result = new ArrayList<>(columns.size());
		for (ColumnIdentifier col : columns)
		{
			if (col.isPkColumn())
			{
				result.add(col);
			}
		}
		for (ColumnIdentifier col : columns)
		{
			if (!col.isPkColumn())
			{
				result.add(col);
			}
		}
		return result;
	}

	/**
	 * Sort the given list by the position of the columns.
	 *
	 * This will sort the columns "in-place", the passed list is modified.
	 *
	 * @param columnList  the column list to sort
	 * @see #getPosition()
	 */
	public static void sortByPosition(List<ColumnIdentifier> columnList)
	{
		Comparator<ColumnIdentifier> c = new Comparator<ColumnIdentifier>()
		{
			@Override
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
