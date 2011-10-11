/*
 * RowData.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
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
import java.sql.SQLXML;
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
 * A class to hold the data for a single row retrieved from the database.
 * <br/>
 * It will also save the originally retrieved information in case the data is changed. <br/>
 * <br/>
 * A row can be in one of three different statuses:<br/>
 * <ul>
 * <li><tt>NEW</tt> - the row has not been retrieved from the database (i.e. was created on the client)</li>
 * <li><tt>MODIFIED</tt> - the row has been retrieved but has been changed since then</li>
 * <li><tt>NOT_MODIFIED</tt> - The row has not been changed since it has been retrieved</li>
 * </ul>
 * @author Thomas Kellerer
 */
public class RowData
{
	/**
	 * The row has not been modified since retrieval.
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

	private static final Object NO_CHANGE_MARKER = new Object();

	private int status = NOT_MODIFIED;

	/**
	 * Mark this row have beeing sent to the database.
	 *
	 * This flag will be used by the {@link DataStore}
	 * to store the information for which rows the SQL statements
	 * have been sent to the database during the update process
	 * @see #setDmlSent(boolean)
	 */
	private boolean dmlSent;

	private Object[] colData;
	private Object[] originalData;
	private List<String> dependencyDeletes;

	private Object userObject;

	private DataConverter converter;
	boolean ignoreReadErrors;

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

	public Object[] getData()
	{
		return this.colData;
	}

	/**
	 * Define a converter that may convert the data read from the database
	 * to e.g. a more readable format.
	 * <br/>
	 * As potentially a large number of rows can be created the registered converter
	 * should be a singleton so that the memory used for retrieving the data is not increased
	 * too much.
	 * <br/>
	 * @param conv the converter to be used.
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
	 * BLOBs (and similar datatypes) will be read into a byte array. CLOBs (and similar datatypes)
	 * will be converted into a String object.
	 * <br/>
	 * All other types will be retrieved using getObject() from the result set, except for
	 * timestamp and date to work around issues with the Oracle driver.
	 * <br/>
	 * If the driver returns a java.sql.Struct, this will be converted into a String
	 * using {@linkplain StructConverter#getStructDisplay(java.sql.Struct)}
	 * <br/>
	 * After retrieving the value from the ResultSet it is passed to a registered DataConverter.
	 * If a converter is registered, no further processing will be done with the column's value
	 * <br/>
	 * The status of this RowData will be reset (NOT_MODIFIED) after the data has been retrieved.
	 *
	 * @param rs the ResultSet that is positioned to the correct row
	 * @param info the resultInfo for the data currently retrieved
	 * @param trimCharData if true, values for Types.CHAR columns will be trimmed.
	 *
	 * @see #setConverter(workbench.storage.DataConverter)
	 */
	public void read(ResultSet rs, ResultInfo info, boolean trimCharData)
		throws SQLException
	{
		int colCount = this.colData.length;
		boolean longVarcharAsClob = info.treatLongVarcharAsClob();
		boolean useGetBytesForBlobs = info.useGetBytesForBlobs();
		boolean useGetStringForClobs = info.useGetStringForClobs();
		boolean useGetStringForBit = info.useGetStringForBit();
		boolean useGetXML = info.useGetXML();

		Object value;

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
				if (type == Types.VARCHAR || type == Types.NVARCHAR)
				{
					value = rs.getString(i+1);
				}
				else if (type == Types.TIMESTAMP)
				{
					value = rs.getTimestamp(i+1);
				}
				else if (type == Types.DATE)
				{
					value = rs.getDate(i+1);
				}
				else if (useGetStringForBit && type == Types.BIT)
				{
					value = rs.getString(i + 1);
				}
				else if (type == java.sql.Types.STRUCT)
				{
					Object o = rs.getObject(i+1);
					if (o instanceof Struct)
					{
						value = StructConverter.getInstance().getStructDisplay((Struct)o);
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
				else if (type == Types.SQLXML)
				{
					value = readXML(rs, i+1, useGetXML);
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
				else if (type == Types.CHAR || type == Types.NCHAR)
				{
					value = rs.getString(i+1);
					if (trimCharData && value != null)
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
				else
				{
					value = rs.getObject(i + 1);
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

	private Object readXML(ResultSet rs, int column, boolean useGetXML)
		throws SQLException
	{
		Object value = null;
		if (useGetXML)
		{
			SQLXML xml = null;
			try
			{
				xml = rs.getSQLXML(column);
				value = xml.getString();
			}
			finally
			{
				if (xml != null) xml.free();
			}
		}
		else
		{
			value = readCharacterStream(rs, column);
		}
		return value;
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
	 * Create a deep copy of this object.
	 * <br/>
	 * The status of the new row will be <tt>NEW</tt>, which means that the "original" data will
	 * be lost after creating the copy.
	 */
	public RowData createCopy()
	{
		RowData result = new RowData(this.colData.length);
		System.arraycopy(colData, 0, result.colData, 0, colData.length);
		result.converter = this.converter;
		result.userObject = this.userObject;
		result.ignoreReadErrors = this.ignoreReadErrors;
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
		Arrays.fill(originalData, NO_CHANGE_MARKER);
	}

	public Object getUserObject()
	{
		return userObject;
	}

	public void setUserObject(Object value)
	{
		userObject = value;
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
	 * the database.
	 * <br/>
	 * If the column was not modified or this row is new
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

	/**
	 * Restore the value of a specific column to its original value.
	 * <br/>
	 * If the column has not been changed, nothing happens
	 *
	 * @param column the column to restore
	 * @return the original (now current) value, null if no original value was present
	 */
	public Object restoreOriginalValue(int column)
	{
		if (this.originalData == null) return null;
		this.colData[column] = this.originalData[column];
		resetStatusForColumn(column);
		return this.colData[column];
	}

	/**
	 * Restore the values to the ones initially retrieved from the database.
	 * <br/>
	 * After calling this, the status of this row will be <tt>NOT_MODIFIED</tt>
	 *
	 * @return true if there were original values
	 *         false if nothing was restored
	 */
	public boolean restoreOriginalValues()
	{
		if (this.originalData == null) return false;
		for (int i=0; i < this.originalData.length; i++)
		{
			if (this.originalData[i] != null)
			{
				this.colData[i] = this.originalData[i];
			}
		}
		this.originalData = null;
		this.resetStatus();
		return true;
	}

	public void resetStatusForColumn(int column)
	{
		if (!this.isNew() && this.originalData != null)
		{
			this.originalData[column] = NO_CHANGE_MARKER;
			for (int i=0; i < originalData.length; i++)
			{
				// if any other column has been modified, the status of the row
				// should not change
				if (this.originalData[i] != NO_CHANGE_MARKER) return;
			}
			// all columns are now NOT_MODIFIED, so reset the row status as well
			this.resetStatus();
		}
	}

	/**
	 * Returns true if the indicated column has been modified since the
	 * initial retrieve. (i.e. since the last time resetStatus() was called
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
		Arrays.fill(colData, null);
		this.resetStatus();
	}

	/**
	 *	Sets the status of this row to new.
	 */
	public final void setNew()
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
	 * Set the status to modified.
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

	@Override
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

	/**
	 * Compares the two values.
	 * <br/>
	 * Byte arrays are compared using Arrays.equals.
	 * Numbers are compared using {@link NumberUtil#valuesAreEqual(java.lang.Number, java.lang.Number) }
	 * which "normalized" that object's classes to compare the real values.
	 * <br/>
	 * For all other values, the equals() method is used.
	 * <br/>
	 *
	 * @param one one value
	 * @param other the other value
	 *
	 * @return true if they are equal
	 *
	 * @see NumberUtil#valuesAreEqual(java.lang.Number, java.lang.Number)
	 */
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
			return NumberUtil.valuesAreEqual((Number)one, (Number)other);
		}
		return one.equals(other);
	}

}
