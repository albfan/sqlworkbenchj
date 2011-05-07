/*
 * PostgresCopyStatementWriterTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import workbench.db.exporter.DataExporter;
import workbench.db.exporter.RowDataConverter;
import java.util.List;
import workbench.db.TableIdentifier;
import java.sql.Types;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import workbench.util.WbFile;
import workbench.TestUtil;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PostgresCopyStatementWriterTest
	extends WbTestCase
{

	public PostgresCopyStatementWriterTest()
	{
		super("PostgresCopyStatementWriterTest");
	}

	@Test
	public void testWriteFormatFile()
		throws Exception
	{
		TestUtil util = getTestUtil();
		final WbFile export = new WbFile(util.getBaseDir(), "export.txt");
		DataExporter exporter = new DataExporter(null)
		{

			@Override
			public String getFullOutputFilename()
			{
				return export.getFullPath();
			}

			@Override
			public boolean getExportHeaders()
			{
				return true;
			}

			@Override
			public String getTextDelimiter()
			{
				return "\t";
			}

			@Override
			public String getTextQuoteChar()
			{
				return "\"";
			}
			@Override
			public String getEncoding()
			{
				return "UTF-8";
			}
		};

		ColumnIdentifier id = new ColumnIdentifier("id" ,Types.INTEGER, true);
		ColumnIdentifier firstname = new ColumnIdentifier("firstname", Types.VARCHAR);
		ColumnIdentifier lastname = new ColumnIdentifier("lastname", Types.VARCHAR);
		final TableIdentifier table = new TableIdentifier("person");

		final ResultInfo info = new ResultInfo(new ColumnIdentifier[] { id, firstname, lastname } );
		info.setUpdateTable(table);

		RowDataConverter converter = new RowDataConverter()
		{

			@Override
			public ResultInfo getResultInfo()
			{
				return info;
			}

			@Override
			public StrBuffer convertRowData(RowData row, long rowIndex)
			{
				return new StrBuffer();
			}

			@Override
			public StrBuffer getStart()
			{
				return null;
			}

			@Override
			public StrBuffer getEnd(long totalRows)
			{
				return null;
			}
		};

		try
		{
			PostgresCopyStatementWriter writer = new PostgresCopyStatementWriter();
			writer.writeFormatFile(exporter, converter);
			WbFile formatFile = new WbFile(util.getBaseDir(), "copy_export.sql");
			assertTrue(formatFile.exists());

			List<String> contents = TestUtil.readLines(formatFile);
			assertNotNull(contents);
			assertEquals(1, contents.size()); // the \copy command cannot span multiple lines.
			assertTrue(contents.get(0).startsWith("\\copy person (id, firstname, lastname)"));
			assertTrue(contents.get(0).contains("delimiter as '\t'"));
			assertTrue(contents.get(0).contains("csv header"));
			assertTrue(contents.get(0).contains("quote as '\"'"));
			assertTrue(contents.get(0).contains("encoding 'UTF-8'"));
		}
		finally
		{
			util.emptyBaseDirectory();
		}
	}
}
