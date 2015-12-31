/*
 * RowDataConverter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.exporter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.interfaces.DataFileWriter;
import workbench.interfaces.ErrorReporter;
import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.DbSettings;
import workbench.db.WbConnection;

import workbench.gui.components.BlobHandler;

import workbench.storage.BlobLiteralFormatter;
import workbench.storage.ColumnData;
import workbench.storage.ResultColumnMetaData;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;

import workbench.util.DefaultOutputFactory;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.NumberStringCache;
import workbench.util.OutputFactory;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbDateFormatter;
import workbench.util.WbFile;
import workbench.util.WbNumberFormatter;
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
	protected boolean checkPosition;
	private File outputFile;
	private File baseDirectory;
	private String baseFilename;
	private String pageTitle;
	private boolean[] columnsToExport;
	protected List<ColumnIdentifier> exportColumns;
	protected ErrorReporter errorReporter;
	protected String nullString;

	protected SimpleDateFormat defaultTimeFormatter;
	protected WbDateFormatter defaultDateFormatter;
	protected WbNumberFormatter defaultNumberFormatter;
	protected WbDateFormatter defaultTimestampFormatter;
	protected boolean needsUpdateTable;
	protected OutputFactory factory;
	private boolean compressExternalFiles;
	protected boolean useRowNumForBlobFile = true;
	protected int[] blobNameCols;
	protected List<String> blobIdColumns;

	protected String filenameColumn;
	protected int filenameColumnIndex = -1;

	protected long currentRow = -1;
	protected RowData currentRowData;

	protected boolean convertDateToTimestamp;
	protected BlobLiteralFormatter blobFormatter;
	protected ExportDataModifier columnModifier;
	protected boolean includeColumnComments;

	protected InfinityLiterals infinityLiterals = InfinityLiterals.PG_LITERALS;
	protected List<String> keyColumnsToUse;

	private long maxBlobFilesPerDir;
	private long blobsWritten;

	protected DataExporter exporter;
	private Map<Integer, Boolean> multilineInfo;

	/**
	 * Spreadsheet option to add an additional sheet with the generating SQL
	 */
	protected boolean appendInfoSheet;

	/**
	 * Spreadsheet option to turn on AutoFilter for the column data
	 */
	protected boolean enableAutoFilter;

	protected boolean fixedHeader;
	protected boolean returnNulls;

	public RowDataConverter()
	{
		defaultDateFormatter = new WbDateFormatter(Settings.getInstance().getDefaultDateFormat());
		defaultTimestampFormatter = new WbDateFormatter(Settings.getInstance().getDefaultTimestampFormat());
		defaultTimeFormatter = new SimpleDateFormat(Settings.getInstance().getDefaultTimeFormat());
	}

	public void setExporter(DataExporter exporter)
	{
		this.exporter = exporter;
	}

	/**
	 * Returns the display string for <tt>null</tt> values.
	 *
	 * If nothing was specified this is an empty string.
	 *
	 * @return the display String, never null
	 */
	public String getNullDisplay()
	{
		return nullString == null ? "" : nullString;
	}

	/**
	 * Set the value to be exported for NULL values.
	 * @param value the String to be used for NULL values (if passed as null it is ignored)
	 */
	public void setNullString(String value)
	{
		this.nullString = value;
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

	public void setInfinityLiterals(InfinityLiterals literals)
	{
		this.infinityLiterals = literals;
		syncInfinityLiterals();
	}

	private void syncInfinityLiterals()
	{
		this.defaultDateFormatter.setInfinityLiterals(infinityLiterals);
		this.defaultTimestampFormatter.setInfinityLiterals(infinityLiterals);
	}

	public void setWriteHeader(boolean writeHeader)
	{
		this.writeHeader = writeHeader;
	}

	public void setPageTitle(String title)
	{
		this.pageTitle = title;
	}

	public void setIncludeColumnComments(boolean flag)
	{
		this.includeColumnComments = flag;
	}

	/**
	 * Enable distribution of LOB files over several directories, in order
	 * to keep the number of files per directory in a reasonable limit.
	 * <br/>
	 * A running number will be appended to the <tt>baseDir</tt> parameter, for each
	 * directory that gets created.
	 *
	 * @param maxFiles
	 */
	public void setMaxLobFilesPerDirectory(int maxFiles)
	{
		maxBlobFilesPerDir = maxFiles;
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

	public String getTargetFileDetails()
	{
		return null;
	}

	protected boolean isMultiline(int column)
	{
		Boolean result = multilineInfo.get(Integer.valueOf(column));
		if (result == null) return false;
		return result.booleanValue();
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

		int colCount = metaData.getColumnCount();
		this.multilineInfo = new HashMap<>(colCount);
		for (int c = 0; c < colCount; c++)
		{
			boolean multiline = SqlUtil.isMultiLineColumn(metaData.getColumn(c));
			multilineInfo.put(Integer.valueOf(c), Boolean.valueOf(multiline));
		}

		if (this.filenameColumn != null)
		{
			this.filenameColumnIndex = meta.findColumn(filenameColumn);
		}
		if (this.includeColumnComments)
		{
			retrieveColumnComments();
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

	public void retrieveColumnComments()
	{
		if (StringUtil.isEmptyString(generatingSql)) return;
		ResultColumnMetaData meta = new ResultColumnMetaData(generatingSql, this.originalConnection);
		try
		{
			meta.retrieveColumnRemarks(getResultInfo());
		}
		catch (SQLException e)
		{
			LogMgr.logError("RowDataConverter.retrieveColumnComments()", "Error retrieving column comments", e);
		}
	}

	public ResultInfo getResultInfo()
	{
		return this.metaData;
	}

	void setBlobIdColumns(List<String> cols)
	{
		blobIdColumns = cols == null ? null : new ArrayList<>(cols);
	}

	public void setOutputFile(WbFile f)
	{
		baseDirectory = null;
		outputFile = f;
		if (f != null && !f.isDirectory())
		{
			baseFilename = f.getFileName();
		}

		if (outputFile != null)
		{
			if (this.outputFile.isDirectory())
			{
				baseDirectory = new File(outputFile.getAbsolutePath());
			}
			else if (outputFile != null)
			{
				baseDirectory = this.outputFile.getParentFile();
			}
		}
		blobsWritten = 0;
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
	@Override
	public File generateDataFileName(ColumnData data)
		throws IOException
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
			File f = new File(getBlobDir(), fname.toString());
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
				filename = value.toString();
			}
		}
		return filename;
	}

	protected String getBlobFileValue(File f)
	{
		if (!distributeLobFiles()) return f.getName();
		String dir = f.getParentFile().getName();
		String fname = f.getName();
		return dir + "/" + fname;
	}

	public File createBlobFile(RowData row, int colIndex, long rowNum)
		throws IOException
	{
		String name = createFilename(row, colIndex, rowNum);
		File f = null;
		if (name == null)
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
			name = fname.toString();
		}
		f = new File(getBlobDir(), name);
		return f;
	}

	private boolean distributeLobFiles()
	{
		return (maxBlobFilesPerDir > 0 && !compressExternalFiles);
	}

	private File getBlobDir()
		throws IOException
	{
		if (!distributeLobFiles())
		{
			return getBaseDir();
		}
		StringBuilder dirName = new StringBuilder(baseFilename.length() + 15);
		dirName.append(baseFilename);
		dirName.append("_lobs_");
		int dirNumber = (int)(blobsWritten / maxBlobFilesPerDir) + 1;
		dirName.append(StringUtil.formatInt(dirNumber, 6));
		WbFile blobDir = new WbFile(getBaseDir(), dirName.toString());
		if (!blobDir.exists())
		{
			boolean created = blobDir.mkdirs();
			if (!created)
			{
				LogMgr.logError("RowDataConverter.getBlobDir()", "Could not create directory: " + blobDir.getFullPath(), null);
				throw new IOException("Could not create directory " + blobDir.getFullPath());
			}
		}
		return blobDir;
	}

	@Override
	public void writeClobFile(String value, File f, String encoding)
		throws IOException
	{
		if (value == null) return;
		Writer w = null;
		try
		{
			OutputStream out = this.createOutputStream(f);
			w = EncodingUtil.createWriter(out, encoding);
			blobsWritten ++;
			w.write(value);
		}
		finally
		{
			FileUtil.closeQuietely(w);
		}
	}

	@Override
	public long writeBlobFile(Object value, File f)
		throws IOException
	{
		if (value == null) return -1;
		long size = 0;

		try
		{
			OutputStream out = this.createOutputStream(f);
			size = BlobHandler.saveBlobToFile(value, out);
			blobsWritten ++;
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

	@Override
	public File getBaseDir()
	{
		if (this.outputFile == null) return new File(".");
		return baseDirectory;
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
		if (col < 0) return false;
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
			DbSettings dbs = this.originalConnection.getDbSettings();
			if (dbs != null)
			{
				this.convertDateToTimestamp = dbs.getConvertDateInExport();
			}
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
	public abstract StringBuilder convertRowData(RowData row, long rowIndex);

	/**
	 *	Returns the String sequence needed in before the actual data part.
	 *  (might be null)
	 */
	public abstract StringBuilder getStart();

	/**
	 *	Returns the String sequence needed in before the actual data part.
	 *  (might be an empty string)
	 */
	public abstract StringBuilder getEnd(long totalRows);

	public boolean needsUpdateTable()
	{
		return this.needsUpdateTable;
	}

	public void setBlobFormatter(BlobLiteralFormatter formatter)
	{
		this.blobFormatter = formatter;
	}

	public void setDefaultTimestampFormatter(WbDateFormatter formatter)
	{
		if (formatter == null) return;
		this.defaultTimestampFormatter = formatter;
		syncInfinityLiterals();
	}

	public void setDefaultTimeFormatter(SimpleDateFormat formatter)
	{
		if (formatter == null) return;
		defaultTimeFormatter = formatter;
	}

	public void setDefaultDateFormatter(WbDateFormatter formatter)
	{
		if (formatter == null) return;
		this.defaultDateFormatter = formatter;
		syncInfinityLiterals();
	}

	public void setDefaultNumberFormatter(WbNumberFormatter formatter)
	{
		this.defaultNumberFormatter = formatter;
	}

	public void setDefaultDateFormat(String format)
	{
		if (StringUtil.isEmptyString(format)) return;
		WbDateFormatter formatter = new WbDateFormatter(format);
		this.setDefaultDateFormatter(formatter);
	}

	public void setDefaultTimestampFormat(String format)
	{
		if (StringUtil.isEmptyString(format)) return;
		WbDateFormatter formatter = new WbDateFormatter(format);
		this.setDefaultTimestampFormatter(formatter);
	}

	public void setDefaultTimeFormat(String format)
	{
		if (StringUtil.isEmptyString(format)) return;
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		setDefaultTimeFormatter(formatter);
	}

	/**
	 *	Set a list of columns that should be exported
	 *	@param columns the list (of ColumnIdentifier objects) of columns to be exported.
	 *                 null means export all columns
	 */
	public void setColumnsToExport(List<ColumnIdentifier> columns)
	{
    if (columns == null)
    {
      this.columnsToExport = null;
      this.exportColumns = null;
      return;
    }

		this.exportColumns = new ArrayList<>(columns);

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
			this.columnsToExport[i] = ColumnIdentifier.containsColumn(columns, this.metaData.getColumn(i));
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
	 *
	 * Especially for Date objects this is different then getValueAsString()
	 * as a default formatter can be defined.
	 *
	 * null values will be returned as the String defined by {@link #setNullString(java.lang.String)}  which is an
	 * empty string by default.
	 *
	 * If null values should be returned as null, use {@link #setReturnNulls(boolean)}
	 *
	 * @param row The requested row
	 * @param col The column in aRow for which the value should be formatted
	 * @return The formatted value as a String
	 *
	 * @see #setDefaultDateFormatter(workbench.util.WbDateFormatter)
	 * @see #setDefaultTimeFormatter(java.text.SimpleDateFormat)
	 * @see #setDefaultNumberFormatter(workbench.util.WbNumberFormatter)
	 * @see #setDefaultDateFormat(String)
	 * @see #setDefaultTimestampFormat(String)
	 * @see #getNullDisplay()
	 * @see #setReturnNulls(boolean)
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
					result = StringUtil.getIsoTimestampFormatter().format(value);
				}
				else
				{
					result = this.defaultTimestampFormatter.format(value);
				}
			}
			else if (value instanceof java.sql.Time && defaultTimeFormatter != null)
			{
				result = defaultTimeFormatter.format(value);
			}
			else if (value instanceof java.util.Date && this.defaultDateFormatter != null)
			{
				result = this.defaultDateFormatter.format(value);
			}
			else if (value instanceof Number && this.defaultNumberFormatter != null)
			{
				result = this.defaultNumberFormatter.format((Number)value);
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
			else if (blobFormatter != null && (value instanceof Blob || value instanceof byte[] || value instanceof InputStream))
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


	protected void writeEscapedXML(StringBuilder out, String s, boolean keepCR)
	{
		if (s == null) return;

		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);

			if (c < 32)
			{
				out.append("&#");
				out.append(NumberStringCache.getNumberString(c));
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

	protected CharSequence escapeXML(String s, boolean keepCR)
	{
		if (s == null) return "";
		StringBuilder out = new StringBuilder(s.length() + 5);
		writeEscapedXML(out, s, keepCR);
		return out;
	}

	protected boolean hasOutputFileExtension(String ext)
	{
		File of = getOutputFile();
		if (of == null) return false;
		WbFile f = new WbFile(of);

		String extension = f.getExtension();
		if (StringUtil.isEmptyString(extension)) return false;
		return extension.equalsIgnoreCase(ext);
	}

	protected boolean checkKeyColumns()
	{
		boolean keysPresent = metaData.hasPkColumns();
		if (this.keyColumnsToUse != null && this.keyColumnsToUse.size() > 0)
		{
			// make sure the default key columns are not used
			this.metaData.resetPkColumns();
			for (String col : keyColumnsToUse)
			{
				this.metaData.setIsPkColumn(col, true);
			}
			keysPresent = true;
		}
		if (!keysPresent)
		{
			try
			{
				this.metaData.readPkDefinition(this.originalConnection);
				keysPresent = this.metaData.hasPkColumns();
			}
			catch (SQLException e)
			{
				LogMgr.logError("SqlRowDataConverter.setCreateInsert", "Could not read PK columns for update table", e);
			}
		}
		return keysPresent;
	}

}
