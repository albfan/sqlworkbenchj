/*
 * ExportWriter.java
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

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.storage.RowData;
import workbench.storage.RowDataReader;
import workbench.storage.RowDataReaderFactory;

import workbench.util.Alias;
import workbench.util.FileUtil;
import workbench.util.SqlUtil;
import workbench.util.WbFile;

/**
 * An ExportWriter acts as a coordinating class between the DataExporter and the corresponding
 * RowDataConverter.
 *
 * It manages the output file(s) and handles the transparent creation of ZIP files.
 *
 * @author  Thomas Kellerer
 */
public abstract class ExportWriter
{
	protected DataExporter exporter;
	protected boolean cancel = false;
	protected long rows;
	protected String tableToUse;
	protected RowActionMonitor rowMonitor;
	protected RowDataConverter converter;
	protected Writer outputWriter;
	protected WbFile outputFile;
	protected boolean canAppendStart;
  protected boolean trimCharData;
	protected boolean useStreamsForBlobs;
	protected boolean useStreamsForClobs;

	private int progressInterval = 10;

	public ExportWriter(DataExporter exp)
	{
		this.exporter = exp;
		converter = createConverter();

		WbConnection con = exporter.getConnection();

		// configureConverter() might be called more than once!
		// To prevent connection dependent information to be read
		// more than once, call setOriginalConnection() only here and now
		converter.setOriginalConnection(con);
		configureConverter();

		if (con != null)
		{
			useStreamsForBlobs = con.getDbSettings().getUseStreamsForBlobExport();
			useStreamsForClobs = con.getDbSettings().getUseStreamsForClobExport();
		}
	}

	public void configureConverter()
	{
		converter.setErrorReporter(exporter);
		converter.setExporter(exporter);

		converter.setEncoding(exporter.getEncoding());
		converter.setDefaultDateFormatter(exporter.getDateFormatter());
		converter.setDefaultTimestampFormatter(exporter.getTimestampFormatter());
		converter.setDefaultTimeFormatter(exporter.getTimeFormatter());
		converter.setDefaultNumberFormatter(exporter.getDecimalFormatter());
		converter.setColumnsToExport(this.exporter.getColumnsToExport());
		converter.setCompressExternalFiles(exporter.getCompressOutput());
		converter.setBlobIdColumns(exporter.getBlobIdColumns());
		converter.setFilenameColumn(exporter.getFilenameColumn());
		converter.setPageTitle(exporter.getPageTitle());
		converter.setWriteHeader(exporter.getExportHeaders());
		converter.setAppendInfoSheet(exporter.getAppendInfoSheet());
		converter.setEnableAutoFilter(exporter.getEnableAutoFilter());
		converter.setEnableFixedHeader(exporter.getEnableFixedHeader());
		converter.setDataModifier(exporter.getDataModifier());
		converter.setIncludeColumnComments(exporter.getIncludeColumnComments());
		converter.setMaxLobFilesPerDirectory(exporter.getMaxLobFilesPerDirectory());
		converter.setInfinityLiterals(exporter.getInfinityLiterals());
    trimCharData = exporter.getTrimCharData();
	}

	public abstract RowDataConverter createConverter();

	public void setProgressInterval(int interval)
	{
		if (interval <= 0)
		{
			this.progressInterval = 0;
		}
		else
		{
			this.progressInterval = interval;
		}
	}

	public void setRowMonitor(RowActionMonitor monitor)
	{
		this.rowMonitor = monitor;
	}

	public long getNumberOfRecords()
	{
		return rows;
	}

	public void writeExport(DataStore ds, List<ColumnIdentifier> columnsToExport)
		throws SQLException, IOException
	{
		ResultInfo info = ds.getResultInfo();
		this.converter.setGeneratingSql(ds.getGeneratingSql());
		this.converter.setResultInfo(info);
		converter.setColumnsToExport(columnsToExport);

		if (this.converter.needsUpdateTable() || !exporter.getControlFileFormats().isEmpty())
		{
			ds.checkUpdateTable();
		}

		this.cancel = false;
		this.rows = 0;

		startProgress();

		writeStart();

		int rowCount = ds.getRowCount();
		for (int i=0; i < rowCount; i++)
		{
			if (this.cancel) break;

			updateProgress(rows);
			RowData row = ds.getRow(i);
			writeRow(row, rows);
			rows ++;
		}
		writeEnd(rows);
	}

	public boolean managesOutput()
	{
		return false;
	}

	public void setOutputFile(WbFile out)
	{
		this.outputFile = out;
		this.converter.setOutputFile(out);
	}

	public void setOutputWriter(Writer out)
	{
		this.outputWriter = out;
	}

	public void writeExport(ResultSet rs, ResultInfo info, String query)
		throws SQLException, IOException
	{
		this.converter.setResultInfo(info);
		this.converter.setGeneratingSql(query);

		this.cancel = false;
		this.rows = 0;

		if (this.converter.needsUpdateTable() || !exporter.getControlFileFormats().isEmpty())
		{
			List<Alias> tables = SqlUtil.getTables(query, false, this.exporter.getConnection());
			if (tables.size() > 0)
			{
				info.setUpdateTable(new TableIdentifier(tables.get(0).getObjectName(), exporter.getConnection()));
			}
		}

		startProgress();

		boolean first = true;
		if (this.exporter.writeEmptyResults())
		{
			writeStart();
		}

		RowDataReader reader = RowDataReaderFactory.createReader(info, exporter.getConnection());
		reader.setUseStreamsForBlobs(useStreamsForBlobs);
		reader.setUseStreamsForClobs(useStreamsForClobs);

		while (rs.next())
		{
			if (this.cancel) break;

			if (first)
			{
				first = false;
				if (!this.exporter.writeEmptyResults())
				{
					writeStart();
				}
			}
			updateProgress(rows);

			RowData row = reader.read(rs, trimCharData);
			writeRow(row, rows);
			reader.closeStreams();
			rows ++;
		}

		if (rows > 0 || this.exporter.writeEmptyResults())
		{
			writeEnd(rows);
		}
	}

	protected void startProgress()
	{
		if (this.rowMonitor != null && this.progressInterval > 0)
		{
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_EXPORT);
		}
	}

	protected void updateProgress(long currentRow)
	{
		if (this.rowMonitor != null && this.progressInterval > 0 &&
				(currentRow == 1 || this.rows % this.progressInterval == 0))
		{
			this.rowMonitor.setCurrentRow((int)currentRow, -1);
		}
	}

	protected void writeRow(RowData row, long numRows)
		throws IOException
	{
		converter.applyDataModifier(row, numRows);
		CharSequence data = converter.convertRowData(row, numRows);
		if (data != null && outputWriter != null)
		{
			this.outputWriter.append(data);
		}
	}

	protected void writeStart()
		throws IOException
	{
		boolean doWriteStart = true;
		if (exporter.getAppendToFile())
		{
			doWriteStart = canAppendStart;
			// If the header can be appended anyway, then there is no need
			// to check if the file is empty
			if (this.outputFile != null && !canAppendStart)
			{
				doWriteStart = !outputFile.exists() || (outputFile.length() == 0);
			}
		}

		if (!doWriteStart) return;

		writeFormatFile();
		CharSequence data = converter.getStart();
		if (data != null && outputWriter != null)
		{
			this.outputWriter.append(data);
		}
	}

	protected void writeEnd(long totalRows)
		throws IOException
	{
		CharSequence data = converter.getEnd(totalRows);
		if (data != null && outputWriter != null)
		{
			this.outputWriter.append(data);
		}
	}

	public void exportStarting()
		throws IOException
	{

	}

	public long exportFinished()
	{
		FileUtil.closeQuietely(outputWriter);
		try
		{
			if (this.converter != null) this.converter.exportFinished();
		}
		catch (Exception e)
		{
			LogMgr.logError("ExportWriter.exportFinished()", "Error closing output stream", e);
			return -1;
		}
		return this.rows;
	}

	public void cancel()
	{
		this.cancel = true;
	}

	/**
	 * Setter for property tableToUse.
	 * @param tableName New value of property tableToUse.
	 */
	public void setTableToUse(String tableName)
	{
		this.tableToUse = tableName;
	}

	protected void writeFormatFile()
	{
		Set<ControlFileFormat> formats = exporter.getControlFileFormats();
		for (ControlFileFormat format : formats)
		{
			FormatFileWriter writer = ControlFileFormat.createFormatWriter(format);
			writer.writeFormatFile(exporter, converter);
		}
	}

	public RowDataConverter getConverter()
	{
		return converter;
	}
}
