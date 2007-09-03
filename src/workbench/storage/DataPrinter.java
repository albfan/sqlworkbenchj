/*
 * DataPrinter.java
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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.exporter.TextRowDataConverter;
import workbench.log.LogMgr;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * A class to print the contents of a {@link DataStore} to a PrintStream
 * 
 * @author support@sql-workbench.net
 */
public class DataPrinter
{
	private DataStore data;
	private TextRowDataConverter converter;
	
	public DataPrinter(DataStore source)
	{
		this.data = source;
		initConverter("\t", StringUtil.LINE_TERMINATOR, null, true);
	}

	public DataPrinter(DataStore source, boolean includeHeaders)
	{
		this.data = source;
		initConverter("\t", StringUtil.LINE_TERMINATOR, null, includeHeaders);
	}
	
	public DataPrinter(DataStore source, String delimiter, String lineEnd, List<ColumnIdentifier> columns, boolean includeHeader)
	{
		this.data = source;
		initConverter(delimiter, lineEnd, columns, includeHeader);
	}
	

	private void initConverter(String delimiter, String lineEnd, List<ColumnIdentifier> columns, boolean includeHeader)
	{
		converter = new TextRowDataConverter();
		converter.setResultInfo(data.getResultInfo());
		converter.setWriteBlobToFile(false);
		converter.setWriteHeader(includeHeader);
		converter.setLineEnding(lineEnd);
		converter.setDelimiter(delimiter);
		converter.setColumnsToExport(columns);
	}
	
	public void printTo(PrintStream out)
	{
		PrintWriter pw = new PrintWriter(out);
		try
		{
			writeDataString(pw, (int[])null);
		}
		catch (IOException e)
		{
			LogMgr.logError("DataPrinter.printTo", "Error when printing DataStore contents", e);
		}
	}

	public String getRowDataAsString(int row)
	{
		RowData rowData = data.getRow(row);
		StrBuffer line = converter.convertRowData(rowData, row);
		if (line != null) return line.toString();
		return null;
	}
	
	/**
	 *	Write the contents of the DataStore into the writer but only the rows
	 *  that have been passed in the rows[] parameter
	 */
	public void writeDataString(Writer out, int[] rows)
		throws IOException
	{
		StrBuffer header = converter.getStart();
		if (header != null) 
		{
			header.writeTo(out);
			out.flush(); 
		}
		
		int count = (rows == null ? data.getRowCount() : rows.length);

		for (int i=0; i < count; i++)
		{
			int row = (rows == null ? i : rows[i]);
			RowData rowData = data.getRow(row);
			StrBuffer line = converter.convertRowData(rowData, row);
			line.writeTo(out);
			out.flush(); 
		}
	}	
}
