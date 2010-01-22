/*
 * RowData.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Struct;
import java.sql.Types;
import java.util.Arrays;

import java.util.List;
import workbench.db.importer.ValueDisplay;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.FileUtil;
import workbench.util.NumberUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *	A class to hold the data for a single row retrieved from the database.
 *	It will also save the originally retrieved information in case the
 *  data is changed.
 * <br><<br/>
 *
 *	A row can be in three different status:<br/>
 *  <br/>
 *	NEW          - the row has not been retrieved from the database (i.e. was created on the client)<br/>
 *  MODIFIED     - the row has been retrieved but has been changed since then<br/>
 *  NOT_MODIFIED - The row has not been changed since it has been retrieved<br/>
 *
 * @author Thomas Kellerer
 */ 
public class RowData
{
	/**
	 * The row has not been modified since retrieval
	 */
	public static final int NOT_MODIFIED = 0;

	/**
	 * The data has been modified since retrieval from the database
	 */
	public static final int MODIFIED = 1;

	/**
	 * The row has been inserted at the client (was not retrieved from the database)
	 */
	public static final int NEW = 2;

	private Object NO_CHANGE_MARKER = new Object();
	
	private int status = NOT_MODIFIED;

	/**
	 * This flag will be used by the {@link DataStore}
	 * to store the information for which rows the SQL statements
	 * have been sent to the database during the update process
	 * @see #setDmlSent(boolean)
	 */
	private boolean dmlSent;
	
	private boolean trimCharData;
	
	private Object[] colData;
	private Object[] originalData;
	private List<String> dependencyDeletes;

	private DataConverter converter;
	boolean ignoreReadErrors = false;

	public RowData(ResultInfo info)
	{
		this(info.getColumnCount());
	}
	
	public RowData(int colCount)
	{
		this.colData = new Object[colCount];
		this.setNew();
		ignoreReadErrors = Settings.getInstance().getBoolProperty("workbench.db.ignore.readerror", false);
	}

	public void setTrimCharData(boolean flag)
	{
		this.trimCharData = flag;
	}
	
	public Object[] getData() 
	{ 
		return this.colData; 
	}

	/**
	 * Define a converter that may convert the data read from the database
	 * to e.g. a more readable format.
	 * <br/>
	 * Currently only one converter is used to make SQL Server's "timestamp" data type (which
	 * is some kind of binary data but <b>not</b> a timestamp) look similar to the display in
	 * Microsoft's tools.
	 * <br/><br/>
	 * As potentially a large number of rows can be created the registered converter
	 * should be a singleton so that the memory used for retrieving the data is not increased
	 * too much.
	 * <br/>
	 * @param conv the converte to be used.
	 *
	 * @see workbench.db.mssql.SqlServerDataConverter
	 * @see workbench.db.oracle.OracleDataConverter
	 */
	public void setConverter(DataConverter conv)
	{
		this.converter = conv;
	}

	/**
	 * Checks if the given datatype will be converted.
	 *
	 * @param jdbcType
	 * @param dbmsType
	 * 
	 * @return true if the type is converted
	 */
	public boolean typeIsConverted(int jdbcType, String dbmsType)
	{
		if (converter == null) return false;
		return converter.convertsType(jdbcType, dbmsType);
	}
	
	/**
	 * Read the current row from the ResultSet into this RowData.
	 * <br/>
	 * It is assumed that ResultSet.next() has already been called on the ResultSet.
	 * <br/>
	 * 
	 * BLOBs (and similar datatypes) will be read into a byte array. CLOBs (and similar datatypes)
	 * will be converted into a String object.
	 * <br/>
	 * All other types will be retrieved using getObject() from the result set, except for
	 * timestamp and date to work around issues with the Oracle driver.
	 * <br/>
	 * After retrieving the value from the ResultSet it is passed to a registered DataConverter.
	 * <br/>
	 * The status of this RowData will be reset after the data has been retrieved.
	 *
	 * @see #setConverter(workbench.storage.DataConverter)
	 */
	public void read(ResultSet rs, ResultInfo info)
		throws SQLException
	{
		int colCount = this.colData.length;
		boolean longVarcharAsClob = info.treatLongVarcharAsClob();
		boolean useGetBytesForBlobs = info.useGetBytesForBlobs();
		boolean useGetStringForClobs = info.useGetStringForClobs();
		
		Object value = null;
		
		for (int i=0; i < colCount; i++)
		{
			int type = info.getColumnType(i);

			if (converter != null)
			{
				String dbms = info.getDbmsTypeName(i);
				if (converter.convertsType(type, dbms))
				{
					value = rs.getObject(i + 1);
					this.colData[i] = converter.convertValue(type, dbms, value);
					continue;
				}
			}

			try
			{
				// Not using getObject() for timestamp columns
				// is a workaround for Oracle, because
				// it does not return the correct object class
				// when using getObject() on a TIMESTAMP column
				// I simply assume that this is working properly
				// for other JDBC drivers as well.
				if (type == java.sql.Types.TIMESTAMP)
				{
					value = rs.getTimestamp(i+1);
				}
				else if (type == java.sql.Types.DATE)
				{
					value = rs.getDate(i+1);
				}
				else if (type == java.sql.Types.STRUCT)
				{
					Object o = rs.getObject(i+1);
					if (o instanceof Struct)
					{
						value = SqlUtil.getStructDisplay((Struct)o);
					}
					else
					{
						value = o;
					}
				}
				else if (SqlUtil.isBlobType(type))
				{
					if (useGetBytesForBlobs)
					{
						value = rs.getBytes(i+1);
						if (rs.wasNull()) value = null;
					}
					else
					{
						// BLOB columns are always converted to byte[] internally
						InputStream in = null;
						try
						{
							in = rs.getBinaryStream(i+1);
							if (in != null && !rs.wasNull())
							{
								// readBytes will close the InputStream
								value = FileUtil.readBytes(in);
							}
							else
							{
								value = null;
							}
						}
						catch (IOException e)
						{
							LogMgr.logError("RowData.read()", "Error retrieving binary data for column '" + info.getColumnName(i) + "'", e);
							value = rs.getObject(i+1);
						}
					}
				}
				else if (SqlUtil.isXMLType(type))
				{
					value = readCharacterStream(rs, i + 1);
				}
				else if (SqlUtil.isClobType(type, longVarcharAsClob))
				{
					if (useGetStringForClobs)
					{
						value = rs.getString(i + 1);
					}
					else
					{
						value = readCharacterStream(rs, i + 1);
					}
				}
				else
				{
					value = rs.getObject(i + 1);
					if (type == Types.CHAR && trimCharData && value != null)
					{
						try
						{
							value = StringUtil.rtrim((String)value);
						}
						catch (Throwable th)
						{
							LogMgr.logError("RowData.read()", "Error trimming CHAR data", th);
						}
					}
				}
			}
			catch (SQLException e)
			{
				if (ignoreReadErrors)
				{
					value = null;
					LogMgr.logError("RowData.read()", "Error retrieving data for column '" + info.getColumnName(i) + "'. Using NULL!!", e);
				}
				else
				{
					throw e;
				}
			}
			this.colData[i] = value;
		}
		this.resetStatus();
	}

	private Object readCharacterStream(ResultSet rs, int column)
		throws SQLException
	{
		Object value = null;
		Reader in = null;
		try
		{
			in = rs.getCharacterStream(column);
			if (in != null && !rs.wasNull())
			{
				// readCharacters will close the Reader
				value = FileUtil.readCharacters(in);
			}
			else
			{
				value = null;
			}
		}
		catch (IOException e)
		{
			LogMgr.logWarning("RowData.read()", "Error retrieving clob data for column '" + rs.getMetaData().getColumnName(column) + "'", e);
			value = rs.getObject(column);
		}
		return value;
	}
	/**
	 *	Create a deep copy of this object.
	 *	The status of the new row will be <tt>NEW</tt>
	 */
	public RowData createCopy()
	{
		RowData result = new RowData(this.colData.length);
		for (int i=0; i < this.colData.length; i++)
		{
			result.colData[i] = this.colData[i];
		}
		return result;
	}

	public int getColumnCount()
	{
		if (this.colData == null) return 0;
		return this.colData.length;
	}

	private void createOriginalData()
	{
		this.originalData = new Object[this.colData.length];
		for (int i = 0; i < this.originalData.length; i++)
		{
			this.originalData[i] = NO_CHANGE_MARKER;
		}
	}
	
	/**
	 * Sets the new data for the given column.
	 * <br>
	 * After calling setValue(), isModified() will return true.
	 * <br/>
	 *
	 * @throws IndexOutOfBoundsException
	 * @see #isModified()
	 */
	public void setValue(int aColIndex, Object newValue)
		throws IndexOutOfBoundsException
	{
		if (!this.isNew())
		{
			Object oldValue = this.colData[aColIndex];
			if (objectsAreEqual(oldValue, newValue)) return;
			if (this.originalData == null)
			{
				createOriginalData();
			}
			
			if (this.originalData[aColIndex] == NO_CHANGE_MARKER)
			{
				this.originalData[aColIndex] = this.colData[aColIndex];
			}
		}
		this.colData[aColIndex] = newValue;
		this.setModified();
	}

	/**
	 *	Returns the value for the given column
	 *
	 *	@throws IndexOutOfBoundsException
	 */
	public Object getValue(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.colData[aColumn];
	}

	/**
	 * Returns the value from the specified column as it was retrieved from 
	 * the database. If the column was not modified or this row is new
	 * then the current value is returned.
	 */
	public Object getOriginalValue(int aColumn)
		throws IndexOutOfBoundsException
	{
		if (!isNew() && this.isColumnModified(aColumn))
		{
			return this.originalData[aColumn];
		}
		return this.getValue(aColumn);
	}

	public void restoreOriginalValues()
	{
		if (this.originalData == null) return;
		for (int i=0; i < this.originalData.length; i++)
		{
			if (this.originalData[i] != null)
			{
				this.colData[i] = this.originalData[i];
			}
		}
		this.originalData = null;
		this.resetStatus();
	}

	public void resetStatusForColumn(int column)
	{
		if (!this.isNew() && this.originalData != null)
		{
			this.originalData[column] = NO_CHANGE_MARKER;
			for (int i=0; i < originalData.length; i++)
			{
				if (this.originalData[i] != NO_CHANGE_MARKER) return;
			}
			this.resetStatus();
		}
	}
	
	/**
	 * Returns true if the indicated column has been modified since the 
	 * initial retrieve (i.e. since the last time resetStatus() was called
	 * 
	 */
	public boolean isColumnModified(int aColumn)
	{
		if (this.isOriginal()) return false;
		if (this.isNew())
		{
			return this.colData[aColumn] != null;
		}
		else
		{
			if (this.originalData == null) return false;
			return (this.originalData[aColumn] != NO_CHANGE_MARKER);
		}
	}
	
	/**
	 *	Resets the internal status. After a call to resetStatus()
	 *	isModified() will return false, and isOriginal() will return true.
	 */
	public void resetStatus()
	{
		this.status = NOT_MODIFIED;
		this.dmlSent = false;
		this.originalData = null;
	}

	/**
	 * Resets data and status
	 */
	public void reset()
	{
		for (int i=0; i < this.colData.length; i++)
		{
			colData[i] = null;
		}
		this.resetStatus();
	}
	/**
	 *	Sets the status of this row to new.
	 */
	public void setNew()
	{
		this.status = NEW;
	}

	/**
	 *	Returns true if the row is neither modified nor is a new row.
	 *
	 *	@return true if the row has not been altered since retrieval
	 */
	public boolean isOriginal()
	{
		return this.status == NOT_MODIFIED;
	}

	/**
	 * Check if the row has been modified.
	 * <br/>
	 * A row can be modified <b>and</b> new!
	 * 
	 * @return true if the row has been modified since retrieval
	 *
	 */
	public boolean isModified()
	{
		return (this.status & MODIFIED) ==  MODIFIED;
	}

	/**
	 * Check if the row has been added to the DataStore
	 * after the initial retrieve.
	 *
	 * A row can be modified <b>and</b> new!
	 *
	 * @return true if it's a new row
	 */
	public boolean isNew()
	{
		return (this.status & NEW) == NEW;
	}

	/**
	 *	Set the status to modified.
	 */
	public void setModified()
	{
		this.status = this.status | MODIFIED;
	}

	void setDmlSent(boolean aFlag)
	{
		this.dmlSent = aFlag;
	}

	public boolean isDmlSent() 
	{ 
		return this.dmlSent; 
	}

	public List<String> getDependencyDeletes()
	{
		return this.dependencyDeletes;
	}
	
	public void setDependencyDeletes(List<String> statements)
	{
		this.dependencyDeletes = statements;
	}
	
	public String toString()
	{
		ValueDisplay display = new ValueDisplay(colData);
		return display.toString();
	}

	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 59 * hash + (this.colData != null ? this.colData.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof RowData)) return false;
		RowData other = (RowData)obj;
		if (other.colData.length != this.colData.length) return false;
		for (int i=0; i < colData.length; i++)
		{
			if (!objectsAreEqual(colData[i], other.colData[i])) return false;
		}
		return true;
	}

	public static boolean objectsAreEqual(Object one, Object other)
	{
		if (one == null && other == null) return true;
		if (one == null || other == null) return false;
		
		// consider blobs
		if (one instanceof byte[] && other instanceof byte[])
		{
			return Arrays.equals((byte[])one, (byte[])other);
		}
		if (one instanceof Number && other instanceof Number)
		{
			return NumberUtil.valuesAreEquals((Number)one, (Number)other);
		}
		return one.equals(other);
	}
	
}
