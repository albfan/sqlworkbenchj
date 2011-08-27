/*
 * ResultInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
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
import java.util.List;

import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to cache the meta information of a ResultSet
 * @author  Thomas Kellerer
 */
public class ResultInfo
{
	// Cached ResultSetMetaData information
	final private ColumnIdentifier[] columns;
	final private int colCount;
	private int realColumns;
	private TableIdentifier updateTable;
	private boolean treatLongVarcharAsClob;
	private boolean useGetBytesForBlobs;
	private boolean useGetStringForClobs;
	private boolean isUserDefinedPK;
	private boolean useGetStringForBit;
	private boolean useGetXML;

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
		this.colCount = colNames.length;
		this.columns = new ColumnIdentifier[this.colCount];
		for (int i=0; i < colCount; i++)
		{
			ColumnIdentifier col = new ColumnIdentifier(colNames[i]);
			if (colSizes != null) col.setColumnSize(colSizes[i]);
			col.setDataType(colTypes[i]);
			this.columns[i] = col;
		}
	}

	public ResultInfo(TableIdentifier table, WbConnection conn)
		throws SQLException
	{
		DbMetadata meta = conn.getMetadata();

		TableIdentifier toUse = meta.findObject(table);

		if (toUse == null)
		{
			toUse = table.createCopy();
			if (toUse.getType() == null)
			{
				String type = meta.getObjectType(toUse);
				toUse.setType(type);
			}

			if (toUse.getSchema() == null)
			{
				toUse.setSchema(meta.getCurrentSchema());
			}

			if (toUse.getCatalog() == null)
			{
				toUse.setCatalog(meta.getCurrentCatalog());
			}
		}

		List<ColumnIdentifier> cols = meta.getTableColumns(toUse);
		this.columns = new ColumnIdentifier[cols.size()];
		int i = 0;
		for (ColumnIdentifier col : cols)
		{
			columns[i] = col;
			i++;
		}
		this.colCount = this.columns.length;
		this.treatLongVarcharAsClob = conn.getDbSettings().longVarcharIsClob();
	}

	public ResultInfo(ResultSetMetaData metaData, WbConnection sourceConnection)
		throws SQLException
	{
		this.colCount = metaData.getColumnCount();
		this.columns = new ColumnIdentifier[this.colCount];
		DbMetadata dbMeta = null;
		if (sourceConnection != null)
		{
			dbMeta = sourceConnection.getMetadata();
			treatLongVarcharAsClob = sourceConnection.getDbSettings().longVarcharIsClob();
			useGetBytesForBlobs = sourceConnection.getDbSettings().useGetBytesForBlobs();
			useGetStringForClobs = sourceConnection.getDbSettings().useGetStringForClobs();
			useGetStringForBit = sourceConnection.getDbSettings().useGetStringForBit();
			useGetXML = sourceConnection.getDbSettings().useGetXML();
		}

		for (int i=0; i < this.colCount; i++)
		{
			String name = metaData.getColumnName(i + 1);
			String alias = metaData.getColumnLabel(i + 1);

			boolean realColumn = true;
			if (StringUtil.isNonBlank(name))
			{
				this.realColumns ++;
			}
			else
			{
				name = "Col" + (i+1);
				realColumn = false;
			}

			ColumnIdentifier col = new ColumnIdentifier(name);

			if (!name.equals(alias))
			{
				col.setColumnAlias(alias);
			}

			col.setUpdateable(realColumn);
			try
			{
				int nullInfo = metaData.isNullable(i + 1);
				col.setIsNullable(nullInfo != ResultSetMetaData.columnNoNulls);
			}
			catch (Throwable th)
			{
				LogMgr.logWarning("ResultInfo.initMetadata()", "Error when checking nullable for column : " + name, th);
			}

			String typename = null;
			try
			{
				typename = metaData.getColumnTypeName(i + 1);
			}
			catch (Exception e)
			{
				typename = null;
			}

			int type = metaData.getColumnType(i + 1);
			if (StringUtil.isEmptyString(typename))
			{
				// use the Java name if the driver did not return a type name for this column
				typename = SqlUtil.getTypeName(type);
			}
			if (dbMeta != null) type = dbMeta.getDataTypeResolver().fixColumnType(type, typename);

			col.setDataType(type);
			col.setColumnTypeName(typename);

			int scale = 0;
			int prec = 0;

			// Some JDBC drivers (e.g. Oracle, MySQL) do not like
			// getPrecision or getScale() on all column types, so we only call
			// it for number data types (the only ones were it seems to make sense)
			try
			{
				if (SqlUtil.isNumberType(type)) scale = metaData.getScale(i + 1);
				if (scale == 0) scale = -1;
			}
			catch (Throwable th)
			{
				//LogMgr.logError("ResultInfo.<init>", "Error when obtaining scale for column " + name, th);
				scale = -1;
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

			col.setDisplaySize(size);
			col.setDecimalDigits(scale);
			String dbmsType = null;
			if (dbMeta != null)
			{
				dbmsType = dbMeta.getDataTypeResolver().getSqlTypeDisplay(typename, col.getDataType(), prec, scale);
			}
			else
			{
				dbmsType = SqlUtil.getSqlTypeDisplay(typename, col.getDataType(), prec, scale);
			}

			if (type == Types.VARCHAR)
			{
				// HSQL reports the VARCHAR size in displaySize()
				if (sourceConnection != null && sourceConnection.getDbSettings().reportsRealSizeAsDisplaySize())
				{
					dbmsType = SqlUtil.getSqlTypeDisplay(typename, col.getDataType(), size, 0);
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
				if (useGetStringForBit && type == Types.BIT)
				{
					col.setColumnClassName("java.lang.String");
				}
				else
				{
					String cls = null;
					if (dbMeta != null)
					{
						cls = dbMeta.getDataTypeResolver().getColumnClassName(col.getDataType(), col.getDbmsType());
					}
					if (cls == null) cls = metaData.getColumnClassName(i + 1);
					col.setColumnClassName(cls);
				}
			}
			catch (Throwable e)
			{
				col.setColumnClassName("java.lang.Object");
			}
			this.columns[i] = col;
		}
	}

	public boolean useGetXML()
	{
		return useGetXML;
	}

	public boolean useGetStringForBit()
	{
		return useGetStringForBit;
	}

	public boolean useGetStringForClobs()
	{
		return useGetStringForClobs;
	}

	public boolean useGetBytesForBlobs()
	{
		return useGetBytesForBlobs;
	}

	public boolean treatLongVarcharAsClob()
	{
		return treatLongVarcharAsClob;
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
		isUserDefinedPK = false;
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

	/**
	 * Redefines the PK columns for this ResultInfo.
	 *
	 * All existing PK flags will be cleared and for all columns passed the PK flag is set.
	 */
	public void setPKColumns(List<ColumnIdentifier> cols)
	{
		// reset existing PK definition
		for (ColumnIdentifier col : columns)
		{
			col.setIsPkColumn(false);
		}

		for (ColumnIdentifier col : cols)
		{
			String name = col.getColumnName();
			int colIndex = this.findColumn(name);
			if (colIndex > -1)
			{
				this.columns[colIndex].setIsPkColumn(true);
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

	public String getColumnDisplayName(int i)
	{
		return this.columns[i].getDisplayName();
	}

	public String getColumnName(int i)
	{
		return this.columns[i].getColumnName();
	}

	public String getDbmsTypeName(int i)
	{
		return this.columns[i].getDbmsType();
	}

	public int getColumnCount()
	{
		return this.colCount;
	}

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
			found = readPkColumnsFromMapping();
		}
	}

	public int findColumn(String name)
	{
		if (name == null) return -1;

		String plain = StringUtil.trimQuotes(name);

		for (int i = 0; i < this.columns.length; i++)
		{
			String col = StringUtil.trimQuotes(columns[i].getColumnName());
			if (plain.equalsIgnoreCase(col))
			{
				return i;
			}
		}

		return -1;
	}

	public boolean isUserDefinedPK()
	{
		return isUserDefinedPK;
	}

	public boolean readPkColumnsFromMapping()
	{
		if (this.updateTable == null) return false;
		List<String> cols = PkMapping.getInstance().getPKColumns(this.updateTable.createCopy());
		if (cols == null) return false;
		isUserDefinedPK = false;

		for (String col : cols)
		{
			int index = this.findColumn(col);
			if (index > -1)
			{
				this.setIsPkColumn(index, true);
				isUserDefinedPK = true;
			}
		}
		if (isUserDefinedPK)
		{
			LogMgr.logInfo("ResultInfo.readPkColumnsFromMapping()", "Using pk definition for " + updateTable.getTableName() + " from mapping file: " + StringUtil.listToString(cols, ',', false));
		}
		return isUserDefinedPK;
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
			SqlUtil.closeResult(rs);
		}
		return found;
	}

}
