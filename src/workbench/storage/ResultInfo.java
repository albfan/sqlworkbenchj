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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

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
	private TableIdentifier updateTable;
	
	public ResultInfo(ColumnIdentifier[] cols)
	{
		this.colCount = cols.length;
		this.columns = new ColumnIdentifier[this.colCount];
		for (int i=0; i < colCount; i++)
		{
			this.columns[i] = cols[i].createCopy();
		}
	}
	
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
			if (dbMeta != null) type = dbMeta.fixColumnType(type); // currently only for Oracle's DATE type
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
	}
	
	public void setIsPkColumn(String column, boolean flag)
	{
		int index = this.findColumn(column);
		if (index > -1) this.setIsPkColumn(index, flag);
	}
	
	public void setIsPkColumn(int col, boolean flag)
	{
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
		for (int i=0; i < cols.length; i++)
		{
			String name = cols[i].getColumnName();
			int col = this.findColumn(name);
			if (col > -1)
			{
				boolean pk = cols[i].isPkColumn();
				this.columns[col].setIsPkColumn(pk);
			}
		}
	}
	
	public boolean hasPkColumns()
	{
		for (int i=0; i < this.colCount; i++)
		{
			if (this.columns[i].isPkColumn())
			{
				return true; 
			}
		}
		return false;
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

	public void readPkDefinition(WbConnection aConnection)
		throws SQLException
	{
		if (aConnection == null) return;
		if (this.updateTable == null) return;

		Connection sqlConn = aConnection.getSqlConnection();
		DatabaseMetaData meta = sqlConn.getMetaData();
		String table = aConnection.getMetadata().adjustObjectnameCase(this.updateTable.getTableName());
		String schema = aConnection.getMetadata().adjustObjectnameCase(this.updateTable.getSchema());

		resetPkColumns();
		
		ResultSet rs = meta.getPrimaryKeys(null, schema, table);
		boolean found = this.readPkColumns(rs);
		
		if (!found)
		{
			found = readPkColumnsFromMapping(aConnection); 
		}
		
		return;
	}
	
	public int findColumn(String name)
	{
		if (name == null) return -1;

		String plain = StringUtil.trimQuotes(name);
		
		for (int i = 0; i < this.colCount; i++)
		{
			String col = StringUtil.trimQuotes(this.getColumnName(i));
			if (plain.equalsIgnoreCase(StringUtil.trimQuotes(col)))
			{
				return i;
			}
		}

		return -1;
	}

	public boolean readPkColumnsFromMapping(WbConnection con)
	{
		if (this.updateTable == null) return false;
		Collection cols = PkMapping.getInstance().getPKColumns(con, this.updateTable.createCopy());
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
		if (found) 
		{
			LogMgr.logInfo("ResultInfo.readPkColumnsFromMapping()", "Using pk definition for " + updateTable.getTableName() + " from mapping file: " + StringUtil.listToString(cols, ',', false));
		}
		return found;
	}
	
	private boolean readPkColumns(ResultSet rs)
	{
		boolean found = false;
		try
		{
			while (rs.next())
			{
				String col = rs.getString("COLUMN_NAME");
				int index = this.findColumn(col);
				if (index > -1)
				{
					this.setIsPkColumn(index, true);
					found = true;
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("ResultInfo.readPkColumns()", "Error when reading ResultSet for key columns", e);
		}
		finally
		{
			try { rs.close(); } catch (Exception e) {}
		}
		return found;
	}
	
}
