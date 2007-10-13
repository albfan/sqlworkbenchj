/*
 * RowData.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import java.sql.Types;

import java.util.List;
import workbench.log.LogMgr;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *	A class to hold the data for a single row retrieved from the database.
 *	It will also save the originally retrieved information in case the
 *  data is changed.
 *	A row can be in three different status:
 *	NEW          - the row has not been retrieved from the database (i.e. was created on the client)
 *  MODIFIED     - the row has been retrieved but has been changed since then
 *  NOT_MODIFIED - The row has not been changed since it has been retrieved
 */
public class RowData
{
	public static final int NOT_MODIFIED = 0;
	public static final int MODIFIED = 1;
	public static final int NEW = 2;

	private Object NO_CHANGE_MARKER = new Object();
	
	private int status = NOT_MODIFIED;

	/**
	 *	This flag will be used by the {@link DataStore}
	 *	to store the information for which rows the SQL statements
	 *  have been sent to the database during the update process
	 */
	private boolean dmlSent = false;
	
	private boolean trimCharData = false;
	
	private Object[] colData;
	private Object[] originalData;
	private List<String> dependencyDeletes;
	
	public RowData(ResultInfo info)
	{
		this(info.getColumnCount());
	}
	
	public RowData(int aColCount)
	{
		this.colData = new Object[aColCount];
		this.setNew();
	}

	public void setTrimCharData(boolean flag) { this.trimCharData = flag; }
	
	public Object[] getData() { return this.colData; }
	
	/**
	 *	Read the row data from the supplied ResultSet
	 */
	public synchronized void read(ResultSet rs, ResultInfo info)
		throws SQLException
	{
		int colCount = this.colData.length;
		Object value = null;
		for (int i=0; i < colCount; i++)
		{
			int type = info.getColumnType(i);
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
				else if (SqlUtil.isBlobType(type))
				{
					// BLOB columns are always converted bot byte[] internally
					InputStream in = null;
					try
					{
						in = rs.getBinaryStream(i+1);
						if (in != null && !rs.wasNull())
						{
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
						value = null;
					}
					finally
					{
						try { in.close(); } catch (Throwable th) {}
					}
				}
				else if (SqlUtil.isClobType(type))
				{
					// CLOB columns are always converted to String objects internally
					Reader in = null;
					try
					{
						in = rs.getCharacterStream(i+1);
						if (in != null && !rs.wasNull())
						{
							value = FileUtil.readCharacters(in);
						}
						else 
						{
							value = null;
						}
					}
					catch (IOException e)
					{
						LogMgr.logError("RowData.read()", "Error retrieving data for column '" + info.getColumnName(i) + "'", e);
						// fallback to getObject()
						
						value = rs.getObject(i+1);
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
				throw e;
			}

			this.colData[i] = value;
		}
		this.resetStatus();
	}

	/**
	 *	Create a deep copy of this object.
	 *	The status of the new row will be NOT_MODIFIED
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
	 *	Sets the new data for the given column.
	 *	After a call isModified() will return true
	 *	if the row was not modified before. If the
	 *	row is new the status will not change.
	 *
	 *	@throws IndexOutOfBoundsException
	 */
	public synchronized void setValue(int aColIndex, Object aValue)
		throws IndexOutOfBoundsException
	{
		if (!this.isNew())
		{
			Object oldValue = this.colData[aColIndex];
			if (oldValue != null && oldValue.equals(aValue)) return;
			if (this.originalData == null)
			{
				createOriginalData();
			}
			
			if (this.originalData[aColIndex] == NO_CHANGE_MARKER)
			{
				this.originalData[aColIndex] = this.colData[aColIndex];
			}
		}
		this.colData[aColIndex] = aValue;
		this.setModified();
	}

	/**
	 *	Returns the value for the given column
	 *
	 *	@throws IndexOutOfBoundsException
	 */
	public synchronized Object getValue(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.colData[aColumn];
	}

	/**
	 * Returns the value from the specified column as it was retrieved from 
	 * the database
	 */
	public synchronized Object getOriginalValue(int aColumn)
		throws IndexOutOfBoundsException
	{
		if (this.isColumnModified(aColumn))
		{
			return this.originalData[aColumn];
		}
		return this.getValue(aColumn);
	}

	public synchronized void restoreOriginalValues()
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

	/**
	 * Returns true if the indicated column has been modified since the 
	 * initial retrieve (i.e. since the last time resetStatus() was called
	 * 
	 */
	public synchronized boolean isColumnModified(int aColumn)
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
	public synchronized void resetStatus()
	{
		this.status = NOT_MODIFIED;
		this.dmlSent = false;
		this.originalData = null;
	}

	/**
	 * Resets data and status
	 */
	public synchronized void reset()
	{
		this.colData = null;
		this.resetStatus();
	}
	/**
	 *	Sets the status of this row to new.
	 */
	public synchronized void setNew()
	{
		this.status = NEW;
	}

	/**
	 *	Returns true if the row is neither modified nor is a new row.
	 *
	 *	@return true if the row has not been altered since retrieval
	 */
	public synchronized boolean isOriginal()
	{
		return this.status == NOT_MODIFIED;
	}

	/**
	 *	Check if the row has been modified.
	 *
	 *	@return true if the row has been modified since retrieval
	 *
	 */
	public synchronized boolean isModified()
	{
		return (this.status & MODIFIED) ==  MODIFIED;
	}

	/**
	 *	Check if the row has been added to the DataStore
	 *	after the initial retrieve.
	 *
	 *	@return true if it's a new row
	 */
	public synchronized boolean isNew()
	{
		return (this.status & NEW) == NEW;
	}

	/**
	 *	Set the status to modified.
	 */
	public synchronized void setModified()
	{
		this.status = this.status | MODIFIED;
	}

	synchronized void setDmlSent(boolean aFlag)
	{
		this.dmlSent = aFlag;
	}

	public synchronized boolean isDmlSent() 
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
		int count = this.colData.length;
		StringBuilder result = new StringBuilder(count * 20);

		result.append('{');
		for (int c=0; c < count; c++)
		{
			result.append('[');
			result.append(this.getValue(c));
			result.append(']');
		}
		result.append('}');
		return result.toString();
	}
}
