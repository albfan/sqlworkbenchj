/*
 * Db2FormatFileWriterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import org.junit.Test;
import workbench.WbTestCase;
import java.io.IOException;
import workbench.TestUtil;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.exporter.DataExporter;
import workbench.db.exporter.RowDataConverter;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.FileUtil;
import workbench.util.StrBuffer;
import workbench.util.WbFile;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2FormatFileWriterTest
	extends WbTestCase
{

	public Db2FormatFileWriterTest()
	{
		super("Db2FormatFileWriterTest");
	}

	@Test
	public void testWriteFormatFile()
		throws IOException
	{
		ColumnIdentifier id = new ColumnIdentifier("ID", java.sql.Types.INTEGER);
		id.setDbmsType("INTEGER");

		ColumnIdentifier name = new ColumnIdentifier("NAME", java.sql.Types.VARCHAR);
		id.setDbmsType("VARCHAR(50)");

		ColumnIdentifier blob = new ColumnIdentifier("BINARY_DATA", java.sql.Types.BLOB);
		id.setDbmsType("BLOB");
		
		ResultInfo info = new ResultInfo(new ColumnIdentifier[] { id, name, blob} );
		info.setUpdateTable(new TableIdentifier("SOME_TABLE"));
		
		TestUtil util = new TestUtil("TestDb2Format");
		String dir = util.getBaseDir();
		final WbFile exportFile = new WbFile(dir, "test_export.txt");

		DataExporter exporter = new DataExporter(null)
		{
			@Override
			public String getFullOutputFilename()
			{
				return exportFile.getFullPath();
			}
		};
		
		exporter.setTextDelimiter("\t");
		exporter.setDecimalSymbol(",");
		exporter.setEncoding("ISO-8859-1");
		exporter.setDateFormat("dd.mm.yyyy");
		exporter.setTableName("UNIT.TEST_TABLE");
		exporter.setWriteClobAsFile(false);
		
		RowDataConverter converter = new RowDataConverter()
		{
			@Override
			public StrBuffer convertRowData(RowData row, long rowIndex)
			{
				return null;
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
		converter.setResultInfo(info);
		Db2FormatFileWriter instance = new Db2FormatFileWriter();
		
		instance.writeFormatFile(exporter, converter);
		WbFile controlFile = new WbFile(dir, "test_export.clp");
		assertTrue(controlFile.exists());
		String content = FileUtil.readFile(controlFile, "ISO-8859-1");
		assertTrue(content.indexOf("LOBS FROM .") > -1);
		assertTrue(content.indexOf("coldelX09") > -1);
		assertTrue(content.indexOf("IMPORT FROM test_export.txt OF DEL") > -1);
		assertTrue(content.indexOf("INTO UNIT.TEST_TABLE") > -1);
		assertTrue(content.indexOf("decpt=,") > -1);
		assertTrue(content.indexOf("codepage=819") > -1);
		
		exporter.setTableName(null);
		exporter.setEncoding("UTF-8");
		info = new ResultInfo(new ColumnIdentifier[] { id, name, } );
		info.setUpdateTable(new TableIdentifier("SOME_TABLE"));
		converter.setResultInfo(info);
		instance.writeFormatFile(exporter, converter);
		assertTrue(controlFile.exists());
		content = FileUtil.readFile(controlFile, "ISO-8859-1");
		assertFalse(content.indexOf("LOBS FROM") > -1);
		assertTrue(content.indexOf("INTO SOME_TABLE") > -1);
		assertTrue(content.indexOf("codepage=1208") > -1);
	}
}
