/*
 * ExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import workbench.storage.DataStore;

import workbench.storage.ResultInfo;
import workbench.storage.RowActionMonitor;
import workbench.storage.RowData;
import workbench.util.StrBuffer;

/**
 *
 * @author  info@sql-workbench.net
 */
public abstract class ExportWriter
{
	protected DataExporter exporter;
	protected boolean cancel = false;
	protected long rows;
	protected String tableToUse;
	protected RowActionMonitor rowMonitor;
	private RowDataConverter converter;
	private Writer output;
	private int progressInterval = 10;

	public ExportWriter(DataExporter exp)
	{
		this.exporter = exp;
	}

	public abstract RowDataConverter createConverter(ResultInfo info);

	public void setProgressInterval(int interval)
	{
		if (interval <= 0)
			this.progressInterval = 0;
		else
			this.progressInterval = interval;
	}

	public void setRowMonitor(RowActionMonitor monitor)
	{
		this.rowMonitor = monitor;
	}

	public long getNumberOfRecords()
	{
		return rows;
	}

	private void initConverter(ResultInfo info)
	{
		converter = createConverter(info);
		converter.setEncoding(exporter.getEncoding());
		converter.setDefaultDateFormatter(exporter.getDateFormatter());
		converter.setDefaultTimestampFormatter(exporter.getTimestampFormatter());
		converter.setDefaultNumberFormatter(exporter.getDecimalFormatter());
		converter.setGeneratingSql(exporter.getSql());
		converter.setOriginalConnection(this.exporter.getConnection());
		converter.setColumnsToExport(this.exporter.getColumnsToExport());
	}

	public void writeExport(DataStore ds)
		throws SQLException, IOException
	{
		ResultInfo info = ds.getResultInfo();
		this.initConverter(info);
		if (this.converter.needsUpdateTable())
		{
			ds.checkUpdateTable();
		}

		this.cancel = false;
		this.rows = 0;

		if (this.rowMonitor != null && this.progressInterval > 0)
		{
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_EXPORT);
		}
		writeStart();
		int colCount = info.getColumnCount();
		int rowCount = ds.getRowCount();
		StrBuffer data = null;
		for (int i=0; i < rowCount; i++)
		{
			if (this.cancel) break;

			if (this.rowMonitor != null && this.progressInterval > 0 &&
				  (this.rows == 1 || this.rows % this.progressInterval == 0))
			{
				this.rowMonitor.setCurrentRow((int)this.rows, -1);
			}
			RowData row = ds.getRow(i);
			data = converter.convertRowData(row, rows);
			data.writeTo(this.output);
			rows ++;
		}
		writeEnd(rows);
	}

	public void setOutput(Writer out)
	{
		this.output = out;
	}

	public void writeExport(ResultSet rs, ResultInfo info)
		throws SQLException, IOException
	{
		this.initConverter(info);

		this.cancel = false;
		this.rows = 0;

		if (this.rowMonitor != null && this.progressInterval > 0)
		{
			this.rowMonitor.setMonitorType(RowActionMonitor.MONITOR_EXPORT);
		}
		int colCount = info.getColumnCount();
		writeStart();
		StrBuffer data = null;
		while (rs.next())
		{
			if (this.cancel) break;

			if (this.rowMonitor != null && this.progressInterval > 0 &&
				  (this.rows == 1 || this.rows % this.progressInterval == 0))
			{
				this.rowMonitor.setCurrentRow((int)this.rows, -1);
			}
			RowData row = new RowData(colCount);
			row.read(rs, info);
			data = converter.convertRowData(row, rows);
			data.writeTo(this.output);
			rows ++;
		}
		writeEnd(rows);
	}

	private void writeStart()
		throws IOException
	{
		StrBuffer data = converter.getStart();
		if (data != null)
		{
			data.writeTo(this.output);
		}
	}

	private void writeEnd(long rows)
		throws IOException
	{
		StrBuffer data = converter.getEnd(rows);
		if (data != null)
		{
			data.writeTo(this.output);
		}
	}

	public void exportFinished()
	{
		if (this.output != null)
		{
			try { this.output.close(); } catch (Throwable th) {}
		}
	}

	public void cancel()
	{
		this.cancel = true;
	}

	/**
	 * Getter for property tableToUse.
	 * @return Value of property tableToUse.
	 */
	public String getTableToUse()
	{
		return tableToUse;
	}

	/**
	 * Setter for property tableToUse.
	 * @param tableToUse New value of property tableToUse.
	 */
	public void setTableToUse(String tableToUse)
	{
		this.tableToUse = tableToUse;
	}

}