/*
 * ResultInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 * A class to cache the meta information of a ResultSet
 * @author  support@sql-workbench.net
 */
public class ResultInfo
{
	// Cached ResultSetMetaData information
	private ColumnIdentifier[] columns;
	private int colCount;
	private int realColumns;
	private boolean hasPkColumns;
	private boolean pkFlagValid = false;
	private boolean pkColumnsAreReal = true;
	private TableIdentifier updateTable;
	
	public ResultInfo(String[] colNames, int[] colTypes, int[] colSizes)
	{
		this(colNames, colTypes, colSizes, null);
	}
	
	public ResultInfo(String[] colNames, int[] colTypes, int[] colSizes, String[] columnClasses)
	{
		this.colCount = colNames.length;
		this.columns = new ColumnIdentifier[this.colCount];
		for (int i=0; i < colCount; i++)
		{
			ColumnIdentifier col = new ColumnIdentifier(colNames[i]);
			if (colSizes != null) col.setColumnSize(colSizes[i]);
			col.setDataType(colTypes[i]);
			if (columnClasses != null) col.setColumnClassName(columnClasses[i]);
			this.columns[i] = col;
		}
	}

	public ResultInfo(TableIdentifier table, WbConnection conn)
		throws SQLException
	{
		this.columns = conn.getMetadata().getColumnIdentifiers(table);
		this.colCount = this.columns.length;
	}
	
	public ResultInfo(ResultSetMetaData metaData, WbConnection sourceConnection) 
		throws SQLException
	{
		this.colCount = metaData.getColumnCount();
		this.columns = new ColumnIdentifier[this.colCount];
		DbMetadata dbMeta = (sourceConnection == null ? null : sourceConnection.getMetadata());
		
		for (int i=0; i < this.colCount; i++)
		{
			String name = metaData.getColumnName(i + 1);
			boolean realColumn = true;
			if (name != null && name.trim().length() > 0)
			{
				this.realColumns ++;
			}
			else
			{
				name = "Col" + (i+1);
				realColumn = false;
			}
			
			int type = metaData.getColumnType(i + 1);
			if (dbMeta != null) type = dbMeta.fixColumnType(type); // currently only for Oracle
			ColumnIdentifier col = new ColumnIdentifier(name);
			col.setDataType(type);
			col.setUpdateable(realColumn);

			String typename = null;
			try
			{
				typename = metaData.getColumnTypeName(i + 1);
				col.setColumnTypeName(typename);
			}
			catch (Exception e)
			{
				col.setColumnTypeName(SqlUtil.getTypeName(col.getDataType()));
			}

			int scale = 0;
			int prec = 0;
			
			// Some JDBC drivers (e.g. Oracle, MySQL) do not like 
			// getPrecision or getScale() on all column types, so we only call
			// it for number data types (the only ones were it seems to make sense)
			try
			{
				if (SqlUtil.isNumberType(type)) scale = metaData.getScale(i + 1);
			}
			catch (Throwable th)
			{
				//LogMgr.logError("ResultInfo.<init>", "Error when obtaining scale for column " + name, th);
				scale = 0;
			}
			
			try
			{
				prec = metaData.getPrecision(i + 1);
			}
			catch (Throwable th)
			{
				//LogMgr.logError("ResultInfo.<init>", "Error when obtaining precision for column " + name, th);
				prec = 0;
			}

			int size = 0;
			try
			{
				size = metaData.getColumnDisplaySize(i + 1);
			}
			catch (Exception e)
			{
				size = prec;
			}

			col.setDecimalDigits(scale);
			String dbmsType = DbMetadata.getSqlTypeDisplay(typename, col.getDataType(), prec, scale);; 
			if (type == Types.VARCHAR)
			{
				// HSQL reports the VARCHAR size in displaySize()
				if (sourceConnection != null && sourceConnection.getMetadata().reportsRealSizeAsDisplaySize())
				{
					dbmsType = DbMetadata.getSqlTypeDisplay(typename, col.getDataType(), size, 0);
					col.setColumnSize(size);
				}
				else
				{
					// all others seem to report the VARCHAR size in precision
					col.setColumnSize(prec);
				}
			}
			else
			{
				col.setColumnSize(size);
			}
			col.setDbmsType(dbmsType);
			
			try
			{
				String cls = metaData.getColumnClassName(i + 1);
				// fix for drivers (such as Postgres') that use
				// byte[] as the class for binary data, as I don't know
				// what other drivers are doing, I'm simply anticipating 
				// other array types as well. 
				if (cls.charAt(0) == '[')
				{
					if (cls.equals("[B")) cls = "byte[]";
					else if (cls.equals("[C")) cls = "char[]";
					else if (cls.equals("[I")) cls = "int[]";
					else if (cls.equals("[J")) cls = "long[]";
					else if (cls.startsWith("[L"))
					{
						// a "class" starting with [L is a "real" Object not 
						// a native data type, so we'll extract the real class
						// name, and make that array of that class
						cls = cls.substring(2, cls.length() - 1) + "[]";
					}
				}
				col.setColumnClassName(cls);
			}
			catch (Throwable e)
			{
				col.setColumnClassName("java.lang.Object");
			}
			this.columns[i] = col;
		}
	}

	public ColumnIdentifier getColumn(int i)
	{
		return this.columns[i];
	}
	
	public ColumnIdentifier[] getColumns()
	{
		return this.columns;
	}

	public boolean isNullable(int col)
	{
		return this.columns[col].isNullable();
	}
	
	public void resetPkColumns()
	{
		for (int i=0; i < this.colCount; i++)
		{
			this.columns[i].setIsPkColumn(false);
		}
		this.hasPkColumns = false;
		this.pkFlagValid = true;
	}
	public void setIsPkColumn(String column, boolean flag)
	{
		int index = this.findColumn(column);
		if (index > -1) this.setIsPkColumn(index, flag);
	}
	
	public void setIsPkColumn(int col, boolean flag)
	{
		if (flag) 
		{
			this.hasPkColumns = true;
			this.pkFlagValid = true;
			this.pkColumnsAreReal = true;
		}
		this.columns[col].setIsPkColumn(flag);
	}
	
	public boolean hasUpdateableColumns()
	{
		return this.realColumns > 0;
	}
	
	public boolean isUpdateable(int col)
	{
		return this.columns[col].isUpdateable();
	}
	
	public void setIsNullable(int col, boolean flag) 
	{
		this.columns[col].setIsNullable(flag);
	}
	public void setUpdateable(int col, boolean flag)
	{
		this.columns[col].setUpdateable(flag);
	}

	public boolean isPkColumn(int col)
	{
		return this.columns[col].isPkColumn();
	}
	
	public void setPKColumns(ColumnIdentifier[] cols)
	{
		this.hasPkColumns = false;
		for (int i=0; i < cols.length; i++)
		{
			String name = cols[i].getColumnName();
			int col = this.findColumn(name);
			if (col > -1)
			{
				this.columns[col].setIsPkColumn(cols[i].isPkColumn());
				if (cols[i].isPkColumn()) this.hasPkColumns = true;
			}
		}
		this.pkColumnsAreReal = true;
		this.pkFlagValid = true;
	}
	
	public boolean hasPkColumns()
	{
		if (this.pkFlagValid) return this.hasPkColumns;
		this.hasPkColumns = false;
		for (int i=0; i < this.colCount; i++)
		{
			if (this.columns[i].isPkColumn())
			{
				this.pkFlagValid = true;
				this.hasPkColumns = true;
				break;
			}
		}
		return this.hasPkColumns;
	}
	
	
	public void setUpdateTable(TableIdentifier table)
	{
		this.updateTable = table;
	}
	
	public TableIdentifier getUpdateTable()
	{
		return this.updateTable;
	}

	public int getColumnSize(int col)
	{
		return this.columns[col].getColumnSize();
	}
	
	public void setColumnSizes(int[] sizes)
	{
		if (sizes == null) return;
		if (sizes.length != this.colCount) return;
		for (int i=0; i < this.colCount; i++)
		{
			this.columns[i].setColumnSize(sizes[i]);
		}
	}	
	
	public int getColumnType(int i) 
	{ 
		if (i >= this.columns.length) return Types.OTHER;
		return this.columns[i].getDataType(); 
	}
	
	public void setColumnClassName(int i, String name)
	{
		this.columns[i].setColumnClassName(name);
	}
	
	public String getColumnClassName(int i) 
	{ 
		String className = this.columns[i].getColumnClassName();
		if (className != null) return className;
		return this.getColumnClass(i).getName();
	}
	
	public String getColumnName(int i) { return this.columns[i].getColumnName(); }
	public String getDbmsTypeName(int i) { return this.columns[i].getDbmsType(); }
	public int getColumnCount() { return this.colCount; }
	
	public Class getColumnClass(int aColumn)
	{
		if (aColumn > this.colCount) return null;
		return this.columns[aColumn].getColumnClass();
	}

	public boolean hasRealPkColumns()
	{
		return this.pkColumnsAreReal;
	}
	
	public void readPkDefinition(WbConnection aConnection)
		throws SQLException
	{
		this.readPkDefinition(aConnection, true);
	}
	
	public void readPkDefinition(WbConnection aConnection, boolean useAll)
		throws SQLException
	{
		if (aConnection == null) return;
		if (this.hasPkColumns()) return;
		if (this.updateTable == null) return;

		Connection sqlConn = aConnection.getSqlConnection();
		DatabaseMetaData meta = sqlConn.getMetaData();
		String table = aConnection.getMetadata().adjustObjectnameCase(this.updateTable.getTableName());
		String schema = aConnection.getMetadata().adjustObjectnameCase(this.updateTable.getSchema());

		ResultSet rs = meta.getPrimaryKeys(null, schema, table);
		boolean found = this.readPkColumns(rs);
		
		// no primary keys found --> try the bestRowIdentifier...
		if (!found)
		{
			LogMgr.logInfo("ResultInfo.readPkDefinition()", "No primary key found, trying getBestRowIdentifier()");
			rs = meta.getBestRowIdentifier(null, schema, table, DatabaseMetaData.bestRowSession, false);
			found = this.readPkColumns(rs);
		}

		if (!found)
		{
			LogMgr.logInfo("ResultInfo.readPkDefinition()", "No primary key found, checking for mapping file");
			found = readPkColumnsFromMapping(aConnection); // try to retrieve the PK from a configuration file
		}

		// if we didn't find any columns, use all columns as the identifier
		if (!found && useAll)
		{
			for (int i=0; i < this.getColumnCount(); i++)
			{
				this.setIsPkColumn(i, true);
			}
		}
		this.pkColumnsAreReal = found;
	}
	
	public int findColumn(String name)
	{
		if (name == null) return -1;

		for (int i = 0; i < this.colCount; i++)
		{
			String col = this.getColumnName(i);
			if (col != null && name.equalsIgnoreCase(col))
			{
				return i;
			}
		}

		return -1;
	}

	private boolean readPkColumnsFromMapping(WbConnection con)
	{
		Collection cols = PkMapping.getInstance().getPKColumns(con, this.updateTable);
		if (cols == null) return false;
		Iterator itr = cols.iterator();
		boolean found = false;
		while (itr.hasNext())
		{
			String col = (String)itr.next();
			int index = this.findColumn(col);
			if (index > -1)
			{
				this.setIsPkColumn(index, true);
				found = true;
			}
		}
		return found;
	}
	
	private boolean readPkColumns(ResultSet rs)
	{
		String col = null;
		int index = 0;
		boolean found = false;
		try
		{
			while (rs.next())
			{
				col = rs.getString("COLUMN_NAME");
				index = this.findColumn(col);
				if (index > -1)
				{
					this.setIsPkColumn(index, true);
					found = true;
				}
			}
		}
		catch (SQLException e)
		{
			LogMgr.logError(this, "Identifier column " + col + " not found in resultset! Using all rows as keys", e);
		}
		finally
		{
			try { rs.close(); } catch (Exception e) {}
		}
		return found;
	}
	
}
