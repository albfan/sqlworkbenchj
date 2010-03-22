/*
 * RowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.WbConnection;
import workbench.gui.components.BlobHandler;
import workbench.interfaces.DataFileWriter;
import workbench.interfaces.ErrorReporter;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.BlobLiteralFormatter;
import workbench.storage.ColumnData;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.DefaultOutputFactory;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.OutputFactory;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;
import workbench.util.WbFile;
import workbench.util.ZipOutputFactory;

/**
 * Abstract class for converting data into various output formats.
 *
 * @author  Thomas Kellerer
 */
public abstract class RowDataConverter
	implements DataFileWriter
{
	public static final String BLOB_ARCHIVE_SUFFIX = "_lobs";
	protected String encoding;
	protected WbConnection originalConnection;
	protected String generatingSql;
	protected ResultInfo metaData;
	protected boolean writeHeader = true;
	private File outputFile;
	private String baseFilename;
	private String pageTitle;
	private boolean[] columnsToExport = null;
	protected List exportColumns = null;
	protected ErrorReporter errorReporter;

	protected SimpleDateFormat defaultDateFormatter;
	protected DecimalFormat defaultNumberFormatter;
	protected SimpleDateFormat defaultTimestampFormatter;
	protected boolean needsUpdateTable = false;
	protected OutputFactory factory;
	private boolean compressExternalFiles;
	protected boolean useRowNumForBlobFile = true;
	protected int[] blobNameCols = null;
	protected List<String> blobIdColumns = null;

	protected String filenameColumn = null;
	protected int filenameColumnIndex = -1;

	protected long currentRow = -1;
	protected RowData currentRowData;

	protected boolean convertDateToTimestamp = false;
	protected BlobLiteralFormatter blobFormatter;
	protected ExportDataModifier columnModifier;

	/**
	 * Spreadsheet option to add an additional sheet with the generating SQL
	 */
	protected boolean appendInfoSheet;

	/**
	 * Spreadsheet option to turn on AutoFilter for the column data
	 */
	protected boolean enableAutoFilter;


	protected boolean fixedHeader;

	public RowDataConverter()
	{
		this.defaultDateFormatter = Settings.getInstance().getDefaultDateFormatter();
		this.defaultTimestampFormatter = Settings.getInstance().getDefaultTimestampFormatter();
		this.defaultNumberFormatter = Settings.getInstance().createDefaultDecimalFormatter();
	}

	public void setDataModifier(ExportDataModifier modifier)
	{
		columnModifier = modifier;
	}
	
	public boolean getEnableFixedHeader()
	{
		return fixedHeader;
	}

	public void setEnableFixedHeader(boolean flag)
	{
		this.fixedHeader = flag;
	}


	public boolean getEnableAutoFilter()
	{
		return enableAutoFilter;
	}

	public void setEnableAutoFilter(boolean flag)
	{
		this.enableAutoFilter = flag;
	}

	public boolean getAppendInfoSheet()
	{
		return appendInfoSheet;
	}

	public void setAppendInfoSheet(boolean flag)
	{
		this.appendInfoSheet = flag;
	}

	public void setWriteHeader(boolean writeHeader)
	{
		this.writeHeader = writeHeader;
	}

	public void setPageTitle(String title)
	{
		this.pageTitle = title;
	}

	public String getPageTitle(String defaultTitle)
	{
		if (StringUtil.isEmptyString(pageTitle))
		{
			return defaultTitle;
		}
		else
		{
			return pageTitle;
		}
	}

	/**
	 * Define the structure of the result to be exported.
	 */
	public void setResultInfo(ResultInfo meta)
	{
		this.metaData = meta;
		this.useRowNumForBlobFile = true;

		if (this.blobIdColumns != null)
		{
			int count = this.blobIdColumns.size();
			int found = 0;
			blobNameCols = new int[count];
			int nameIndex = 0;
			for (String col : blobIdColumns)
			{
				int index = meta.findColumn(col);
				blobNameCols[nameIndex] = index;
				if (index > -1) found ++;
				nameIndex ++;
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

		if (this.filenameColumn != null)
		{
			this.filenameColumnIndex = meta.findColumn(filenameColumn);
		}
	}

	public void setFilenameColumn(String colname)
	{
		if (StringUtil.isBlank(colname))
		{
			this.filenameColumn = null;
		}
		else
		{
			this.filenameColumn = colname.trim();
		}
		this.filenameColumnIndex = -1;
	}

	public ResultInfo getResultInfo() { return this.metaData; }

	void setBlobIdColumns(List<String> cols)
	{
		blobIdColumns = cols;
	}

	public void setOutputFile(WbFile f)
	{
		this.outputFile = f;
		if (f != null && !f.isDirectory())
		{
			this.baseFilename = f.getFileName();
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
			// Make sure this instance of the factory it no re-used
			// otherwise writting multiple blob archives does not work
			// when exporting more than one table
			this.factory = null;
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
		StringBuilder fname = new StringBuilder(80);
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
			fname.append(getFileExtension());
			File f = new File(getBaseDir(), fname.toString());
			return f;
		}
	}

	protected String getFileExtension()
	{
		return Settings.getInstance().getProperty("workbench.export.default.blob.extension", ".data");
	}

	protected String createFilename(RowData row, int colIndex, long rowNum)
	{
		String filename = null;
		if (this.filenameColumnIndex > -1)
		{
			Object value = row.getValue(filenameColumnIndex);
			if (value != null)
			{
				//filename = StringUtil.makeFilename(value.toString());
				filename = value.toString();
			}
		}
		return filename;
	}

	public File createBlobFile(RowData row, int colIndex, long rowNum)
	{
		String name = createFilename(row, colIndex, rowNum);
		File f = null;
		if (name != null)
		{
			WbFile wf = new WbFile(name);
			if (!wf.isAbsolute())
			{
				f = new WbFile(getBaseDir(), name);
			}
		}
		else
		{
			StringBuilder fname = new StringBuilder(baseFilename.length() + 25);

			if (this.factory == null) initOutputFactory();

			if (!this.factory.isArchive())
			{
				fname.append(baseFilename);
				fname.append('_');
			}

			if (this.useRowNumForBlobFile || this.blobNameCols == null)
			{
				fname.append("r");
				fname.append(rowNum+1);
				fname.append("_c");
				fname.append(colIndex+1);
			}
			else
			{
				String col = this.metaData.getColumnName(colIndex);
				fname.append(StringUtil.makeFilename(col));
				fname.append("_#");
				for (int i = 0; i < blobNameCols.length; i++)
				{
					int c = blobNameCols[i];
					if (c > -1)
					{
						Object o = row.getValue(c);
						if (i > 0) fname.append('_');
						if (o == null)
						{
							fname.append("col#");
							fname.append(i);
							fname.append("NULL");
						}
						else
						{
							fname.append(StringUtil.makeFilename(o.toString()));
						}
					}
				}
			}

			fname.append(getFileExtension());
			f = new File(getBaseDir(), fname.toString());
		}

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
			FileUtil.closeQuietely(w);
		}
	}

	public long writeBlobFile(Object value, File f)
		throws IOException
	{
		if (value == null) return -1;
		long size = 0;

		try
		{
			OutputStream out = this.createOutputStream(f);
			size = BlobHandler.saveBlobToFile(value, out);
			return size;
		}
		catch (IOException io)
		{
			LogMgr.logError("TextRowDataConverter.convertRowData", "Error writing BLOB file: " + f.getName(), io);
			throw io;
		}
		catch (SQLException e)
		{
			LogMgr.logError("TextRowDataConverter.convertRowData", "Error writing BLOB file", e);
			throw new IOException(ExceptionUtil.getDisplay(e));
		}
	}

	public File getBaseDir()
	{
		if (this.outputFile == null) return new File(".");
		if (this.outputFile.isDirectory()) return this.outputFile;
		if (this.outputFile.isAbsolute()) return this.outputFile.getParentFile();
		return new File(".");
	}

	protected File getOutputFile()
	{
		return this.outputFile;
	}

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
		if (originalConnection != null)
		{
			this.convertDateToTimestamp = this.originalConnection.getDbSettings().getConvertDateInExport();
		}
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

	public void applyDataModifier(RowData row, long currentRow)
	{
		if (this.columnModifier != null)
		{
			columnModifier.modifyData(this, row, currentRow);
		}
	}
	/**
	 *	Returns the data for one specific row as a String in the
	 *  correct format
	 */
	public abstract StrBuffer convertRowData(RowData row, long rowIndex);

	/**
	 *	Returns the String sequence needed in before the actual data part.
	 *  (might be null)
	 */
	public abstract StrBuffer getStart();

	/**
	 *	Returns the String sequence needed in before the actual data part.
	 *  (might be an empty string)
	 */
	public abstract StrBuffer getEnd(long totalRows);

	public boolean needsUpdateTable()
	{
		return this.needsUpdateTable;
	}

	public void setBlobFormatter(BlobLiteralFormatter formatter)
	{
		this.blobFormatter = formatter;
	}

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
	public void setColumnsToExport(List<ColumnIdentifier> columns)
	{
		this.exportColumns = columns;
		if (columns == null)
		{
			this.columnsToExport = null;
			return;
		}

		if (metaData == null)
		{
			LogMgr.logError("RowDataConverter.setColumnsToExport()", "MetaData for result is NULL!", new Exception("TraceBack"));
			this.columnsToExport = new boolean[exportColumns.size()];
			for (int i=0; i < columnsToExport.length; i++)
			{
				columnsToExport[i] = true;
			}
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
	 * Return the number of columns that have to be exported
	 * @return the real column count
	 * @see #includeColumnInExport(int)
	 */
	protected int getRealColumnCount()
	{
		int count = 0;
		for (int i = 0; i < this.metaData.getColumnCount(); i++)
		{
			if (this.includeColumnInExport(i))	count ++;
		}
		return count;
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
		if (value == null)
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
			else if (convertDateToTimestamp && value instanceof java.util.Date)
			{
				// sometimes the Oracle driver create a java.util.Date object, but
				// DATE columns in Oracle do contain a time part and thus we need to
				// format it correctly.
				// Newer Oracle drivers (>= 10) support a property to treat
				// DATE columns as Timestamp but for backward compatibility I'll leave
				// this fix in here.
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
			else if (blobFormatter != null && (value instanceof Blob || value instanceof byte[]))
			{
				try
				{
					result = blobFormatter.getBlobLiteral(value).toString();
				}
				catch (SQLException e)
				{
					LogMgr.logError("TextRowDataConverter.convertRowData", "Error creating blob literal", e);
					throw new RuntimeException("Error creating blob literal", e);
				}
			}
			else
			{
				result = value.toString();
			}
			return result;
		}
	}

	protected void writeEscapedXML(StrBuffer out, String s, boolean keepCR)
	{
		if (s == null) return;
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);

			if (c < 32)
			{
				out.append("&#");
				out.append(Integer.toString(c));
				out.append(';');
			}
			else
			{
				switch (c)
				{
					case '&':
						out.append("&amp;");
						break;
					case '<':
						out.append("&lt;");
						break;
					case '>':
						out.append("&gt;");
						break;
					default:
						out.append(c);
				}
			}
		}
	}
}
