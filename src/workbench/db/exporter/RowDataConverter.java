/*
 * RowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.sql.Clob;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.storage.NullValue;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * Interface for classes that can take objects of type {@link RowData}
 * and convert them to e.g. text, XML, HTML
 *
 * @author  support@sql-workbench.net
 */
public abstract class RowDataConverter
{
	protected String encoding;
	protected WbConnection originalConnection;
	protected String generatingSql;
	protected ResultInfo metaData;
	
	private boolean[] columnsToExport = null;
	protected List exportColumns = null;
	
	protected SimpleDateFormat defaultDateFormatter = StringUtil.ISO_DATE_FORMATTER;
	protected DecimalFormat defaultNumberFormatter;
	protected SimpleDateFormat defaultTimestampFormatter = StringUtil.ISO_TIMESTAMP_FORMATTER;
	protected boolean needsUpdateTable = false;
	
	/**
	 *	The metadata for the result set that should be exported
	 */
	public RowDataConverter(ResultInfo meta)
	{
		this.metaData = meta;
		int colCount = meta.getColumnCount();
	}

	public boolean includeColumnInExport(int col)
	{
		if (this.columnsToExport == null) return true;
		return this.columnsToExport[col];
	}

	/**
	 *	The connection that was used to generate the source data.
	 */
	public void setOriginalConnection(WbConnection conn)
	{
		this.originalConnection = conn;
	}
	
	/**
	 *	The SQL statement that was used to generate the data.
	 */
	public void setGeneratingSql(String sql)
	{
		this.generatingSql = sql;
	}
	
	/**
	 *	Set the encoding for the output string.
	 *	This might not be used by all implemented Converters
	 */
	public void setEncoding(String enc)
	{
		this.encoding = enc;
	}

	public String getEncoding()
	{
		return this.encoding;
	}
	
	/**
	 *	Returns a display name for this exporter
	 */
	public abstract String getFormatName();
	
	/**
	 *	Returns the data from the source as one String
	 *  in the format of this exporter.
	 *	This is equivalent to concatenating the output from:
	 *	#getStart();
	 *  #converRowData(RowData) for all rows
	 *  #getEnd();
	 */
	public abstract StrBuffer convertData();
	
	
	/**
	 *	Returns the data for one specific row as a String in the 
	 *  correct format
	 */
	public abstract StrBuffer convertRowData(RowData row, long rowIndex);
	
	/**
	 *	Returns the String sequence needed in before the actual data part.
	 *  (might be an empty string)
	 */
	public abstract StrBuffer getStart();
	
	/**
	 *	Returns the String sequence needed in before the actual data part.
	 *  (might be an empty string)
	 */
	public abstract StrBuffer getEnd(long totalRows);

	public boolean needsUpdateTable() { return this.needsUpdateTable; }
	
	public void setDefaultTimestampFormatter(SimpleDateFormat formatter)
	{
		if (formatter == null) return;
		this.defaultTimestampFormatter = formatter;
	}
	
	public void setDefaultDateFormatter(SimpleDateFormat formatter)
	{
		if (formatter == null) return;
		this.defaultDateFormatter = formatter;
	}

	public void setDefaultNumberFormatter(DecimalFormat formatter)
	{
		this.defaultNumberFormatter = formatter;
	}

	public void setDefaultDateFormat(String format)
	{
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		this.setDefaultDateFormatter(formatter);
	}
	
	public void setDefaultTimestampFormat(String format)
	{
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		this.setDefaultTimestampFormatter(formatter);
	}

	public void setDefaultNumberFormat(String aFormat)
	{
		if (aFormat == null) return;
		try
		{
			this.defaultNumberFormatter = new DecimalFormat(aFormat);
		}
		catch (Exception e)
		{
			this.defaultNumberFormatter = null;
			LogMgr.logWarning("RowDataConverter.setDefaultDateFormat()", "Could not create decimal formatter for format " + aFormat);
		}
	}

	/**
	 *	Set a list of columns that should be exported
	 *	@param columns the list (of ColumnIdentifier objects) of columns to be exported.
	 *                 null means export all columns
	 */
	public void setColumnsToExport(List columns)
	{
		this.exportColumns = columns;
		if (columns == null)
		{
			this.columnsToExport = null;
			return;
		}
		int colCount = this.metaData.getColumnCount();
		if (this.columnsToExport == null)
		{
			this.columnsToExport = new boolean[colCount];
		}
		for (int i=0; i < colCount; i++)
		{
			this.columnsToExport[i] = columns.contains(this.metaData.getColumn(i));
		}
	}
	
	/**
	 * Return the column's value as a formatted String.
	 * Especially for Date objects this is different then getValueAsString()
	 * as a default formatter can be defined.
	 * @param row The requested row
	 * @param col The column in aRow for which the value should be formatted
	 * @return The formatted value as a String
	 * @see #setDefaultDateFormatter(SimpleDateFormat)
	 * @see #setDefaultTimestampFormatter(SimpleDateFormat)
	 * @see #setDefaultNumberFormatter(DecimalFormat)
	 * @see #setDefaultDateFormat(String)
	 * @see #setDefaultTimestampFormat(String)
	 * @see #setDefaultNumberFormat(String)
	 */
	public String getValueAsFormattedString(RowData row, int col)
		throws IndexOutOfBoundsException
	{
		Object value = row.getValue(col);
    if (value == null || value instanceof NullValue)
		{
      return null;
		}
    else
		{
			String result = null;
			if (value instanceof java.sql.Timestamp && this.defaultTimestampFormatter != null)
			{
				result = this.defaultTimestampFormatter.format(value);
			}
			else if (value instanceof java.util.Date && this.originalConnection.getMetadata().isOracle())
			{
				// sometimes the Oracle driver create a java.util.Date object, but
				// DATE columns in Oracle do contain a time part and thus we need to
				// format it correctly.
				if (this.defaultTimestampFormatter == null)
				{
					result = StringUtil.ISO_TIMESTAMP_FORMATTER.format(value);
				}
				else
				{
					result = this.defaultTimestampFormatter.format(value);
				}
			}
			else if (value instanceof java.util.Date && this.defaultDateFormatter != null)
			{
				result = this.defaultDateFormatter.format(value);
			}
			else if (value instanceof Number && this.defaultNumberFormatter != null)
			{
				result = this.defaultNumberFormatter.format(value);
			}
			else if (value instanceof Clob)
			{
				try
				{
					Clob lob = (Clob)value;
					long len = lob.length();
					return lob.getSubString(1, (int)len);
				}
				catch (SQLException e)
				{
					return "";
				}
			}
			else
			{
				result = value.toString();
			}
      return result;
		}
	}
	
}
