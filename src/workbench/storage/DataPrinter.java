/*
 * DataPrinter.java
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
import java.io.Writer;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.exporter.TextRowDataConverter;
import workbench.util.CharacterRange;
import workbench.util.StrBuffer;
import workbench.util.StringUtil;

/**
 * A class to print the contents of a {@link DataStore} to a PrintStream
 *
 * @author Thomas Kellerer
 */
public class DataPrinter
{
	private DataStore data;
	private TextRowDataConverter converter;
	private int[] columnMap;

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
		converter.setEscapeRange(CharacterRange.RANGE_CONTROL);
	}

	/**
	 * Define a mapping from the stored order of columns to the order
	 * that is visible to the user.
	 *
	 * @param map
	 */
	public void setColumnMapping(int[] map)
	{
		columnMap = map;
	}

	/**
	 * Write the contents of the DataStore into the writer but only the rows
	 * that have been passed in the rows[] parameter
	 * 
	 * @param out the writer to use
	 * @param rows the rows to print, if this is null all rows are printed
	 */
	public void writeDataString(Writer out, int[] rows)
		throws IOException
	{
		StrBuffer header = converter.getStart(columnMap);
		if (header != null)
		{
			header.writeTo(out);
			out.flush();
		}

		int count = (rows == null ? data.getRowCount() : rows.length);

		for (int i=0; i < count; i++)
		{
			int row = (rows == null ? i : rows[i]);
			StrBuffer line = converter.convertRowData(data.getRow(row), row, columnMap);
			line.writeTo(out);
			out.flush();
		}
	}

}
