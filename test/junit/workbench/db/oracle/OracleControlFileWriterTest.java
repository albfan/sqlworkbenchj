/*
 * OracleControlFileWriterTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.sql.Types;
import java.util.List;
import workbench.TestUtil;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.exporter.DataExporter;
import workbench.db.exporter.RowDataConverter;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import workbench.util.WbFile;
import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleControlFileWriterTest
	extends WbTestCase
{

	public OracleControlFileWriterTest()
	{
		super("OracleControlFileWriterTest");
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
			public WbFile getOutputFile()
			{
				return export;
			}

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
		};

		exporter.setTimestampFormat("yyyy_MM_dd");
		exporter.setEncoding("UTF-8");

		ColumnIdentifier id = new ColumnIdentifier("id" ,Types.INTEGER, true);
		ColumnIdentifier firstname = new ColumnIdentifier("firstname", Types.VARCHAR);
		ColumnIdentifier lastname = new ColumnIdentifier("lastname", Types.VARCHAR);
		ColumnIdentifier birthday = new ColumnIdentifier("birthday", Types.DATE);
		final TableIdentifier table = new TableIdentifier("person");

		final ResultInfo info = new ResultInfo(new ColumnIdentifier[] { id, firstname, lastname, birthday } );
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
			OracleControlFileWriter writer = new OracleControlFileWriter();
			writer.writeFormatFile(exporter, converter);
			WbFile formatFile = new WbFile(util.getBaseDir(), "export.ctl");
			assertTrue(formatFile.exists());
			List<String> lines = TestUtil.readLines(formatFile);
			assertNotNull(lines);
			assertEquals(15, lines.size());

			// First line is a comment so the actual contents starts with the second line
			assertEquals("OPTIONS (skip=1)", lines.get(1));
			assertEquals("LOAD DATA CHARACTERSET 'AL32UTF8'", lines.get(3));
			assertEquals("INFILE 'export.txt'", lines.get(4));
			assertEquals("INTO TABLE person", lines.get(7));
			assertEquals("FIELDS TERMINATED BY '\\t' TRAILING NULLCOLS", lines.get(8));

			assertEquals("(", lines.get(9).trim());
			assertEquals("  id,", lines.get(10));
			assertEquals("  firstname,", lines.get(11));
			assertEquals("  lastname,", lines.get(12));
			assertEquals("  birthday  DATE \"YYYY_MM_DD\"", lines.get(13));
			assertEquals(")", lines.get(14).trim());
		}
		finally
		{
			util.emptyBaseDirectory();
		}
	}
}