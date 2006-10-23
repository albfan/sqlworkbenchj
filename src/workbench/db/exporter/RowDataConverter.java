/*
 * RowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import workbench.db.WbConnection;
import workbench.gui.components.BlobHandler;
import workbench.interfaces.DataFileWriter;
import workbench.interfaces.ErrorReporter;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.ColumnData;
import workbench.storage.NullValue;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.DefaultOutputFactory;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.OutputFactory;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.ZipOutputFactory;

/**
 * Interface for classes that can take objects of type {@link RowData}
 * and convert them to e.g. text, XML, HTML
 *
 * @author  support@sql-workbench.net
 */
public abstract class RowDataConverter
	implements DataFileWriter
{
	public static final String BLOB_ARCHIVE_SUFFIX = "_lobs";
	protected String encoding;
	protected WbConnection originalConnection;
	protected String generatingSql;
	protected ResultInfo metaData;
	private File outputFile;
	private String baseFilename;
	private boolean[] columnsToExport = null;
	protected List exportColumns = null;
	protected ErrorReporter errorReporter;
	
	protected SimpleDateFormat defaultDateFormatter;
	protected DecimalFormat defaultNumberFormatter;
	protected SimpleDateFormat defaultTimestampFormatter;
	protected boolean needsUpdateTable = false;
	private OutputFactory factory;
	private boolean compressExternalFiles;
	protected boolean useRowNumForBlobFile = true;
	protected int[] blobNameCols = null;
	protected List blobIdColumns = null;
	protected long currentRow = -1;
	protected RowData currentRowData;
	
	/**
	 *	The metadata for the result set that should be exported
	 */
	public RowDataConverter()
	{
		this.defaultDateFormatter = Settings.getInstance().getDefaultDateFormatter();
		this.defaultTimestampFormatter = Settings.getInstance().getDefaultTimestampFormatter();
		this.defaultNumberFormatter = Settings.getInstance().getDefaultDecimalFormatter();
	}

	public void setResultInfo(ResultInfo meta) 
	{ 
		this.metaData = meta; 
		this.useRowNumForBlobFile = true;
		
		if (this.blobIdColumns != null) 
		{
			int count = this.blobIdColumns.size();
			int found = 0;
			blobNameCols = new int[count];
			for (int i = 0; i < count; i++)
			{
				String col = (String)blobIdColumns.get(i);
				int index = meta.findColumn(col);
				blobNameCols[i] = index;
				if (index > -1) found ++;
			}
			if (found == 0)
			{
				this.blobNameCols = null;
				this.useRowNumForBlobFile = true;
			}
			else
			{
				this.useRowNumForBlobFile = false;
			}
		}
	}
	
	public ResultInfo getResultInfo() { return this.metaData; }
	
	void setBlobIdColumns(List cols)
	{
		blobIdColumns = cols;
	}
	
	public void setOutputFile(File f) 
	{ 
		this.outputFile = f; 
		if (f != null) 
		{
			WbFile wf = new WbFile(f);
			this.baseFilename = wf.getFileName();
		}
	}
	
	public void setCompressExternalFiles(boolean flag)
	{
		this.compressExternalFiles = flag;
	}
	
	private void initOutputFactory()
	{
		if (this.compressExternalFiles)
		{
			WbFile f = new WbFile(getOutputFile());
			String fname = f.getFileName() + BLOB_ARCHIVE_SUFFIX + ".zip";
			File archive = new File(getBaseDir(), fname);
			this.factory = new ZipOutputFactory(archive);
		}
		else
		{
			this.factory = new DefaultOutputFactory();
		}
	}
	
	public void exportFinished()
		throws IOException
	{
		if (this.factory != null)
		{
			this.factory.done();
		}
	}
	
	protected OutputStream createOutputStream(File output)
		throws IOException
	{
		if (this.factory == null) initOutputFactory();
		return this.factory.createOutputStream(output);
	}
	
	/**
	 * Needed for the SqlLiteralFormatter 
	 */
	public File generateDataFileName(ColumnData data)
	{
		StringBuffer fname = new StringBuffer(80);
		if (this.currentRowData != null && currentRow != -1)
		{
			int colIndex = this.metaData.findColumn(data.getIdentifier().getColumnName());
			return createBlobFile(currentRowData, colIndex, currentRow);
		}
		else
		{
			fname.append(StringUtil.makeFilename(data.getIdentifier().getColumnName()));
			fname.append('_');
			if (this.currentRow == -1)
			{
				fname.append(data.getValue().hashCode());
			}
			else
			{
				fname.append("row_");
				fname.append(currentRow);
			}
			fname.append(".data");
			File f = new File(getBaseDir(), fname.toString());
			return f;
		}
	}
	
	public File createBlobFile(RowData row, int colIndex, long rowNum)
	{
		StringBuffer fname = new StringBuffer(baseFilename.length() + 25);

		fname.append(baseFilename);
		
		if (this.useRowNumForBlobFile || this.blobNameCols == null)
		{
			fname.append("_r");
			fname.append(rowNum+1);
			fname.append("_c");
			fname.append(colIndex+1);
		}
		else
		{
			String col = this.metaData.getColumnName(colIndex);
			fname.append('_');
			fname.append(StringUtil.makeFilename(col));
			fname.append("_#");
			for (int i = 0; i < blobNameCols.length; i++)
			{
				int c = blobNameCols[i];
				if (c > -1)
				{
					Object o = row.getValue(c);
					if (i > 0) fname.append('_');
					fname.append(StringUtil.makeFilename(o.toString()));
				}
			}
		}
		fname.append(".data");
		File f = new File(getBaseDir(), fname.toString());
		return f;
	}

	public void writeClobFile(String value, File f, String encoding)
		throws IOException
	{
		if (value == null) return;
		Writer w = null;
		try
		{
			OutputStream out = this.createOutputStream(f);
			w = EncodingUtil.createWriter(out, encoding);
			w.write(value);
		}
		finally
		{
			try { w.close(); } catch (Throwable th) {}
		}
	}
	
	public void writeBlobFile(Object value, File f)
		throws IOException
	{
		if (value == null || value instanceof NullValue) return;
		OutputStream out = this.createOutputStream(f);
		try
		{
			BlobHandler.saveBlobToFile(value, out);
		}
		catch (SQLException e)
		{
			throw new IOException(ExceptionUtil.getDisplay(e));
		}
	}
	
	public File getBaseDir()
	{
		if (this.outputFile == null) return new File(".");
		if (this.outputFile.isAbsolute()) return this.outputFile.getParentFile();
		return new File(".");
	}
	
	protected File getOutputFile() { return this.outputFile; }
	
	public void setErrorReporter(ErrorReporter reporter)
	{
		this.errorReporter = reporter;
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
		if (StringUtil.isEmptyString(format)) return;
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		this.setDefaultDateFormatter(formatter);
	}
	
	public void setDefaultTimestampFormat(String format)
	{
		if (StringUtil.isEmptyString(format)) return;
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
