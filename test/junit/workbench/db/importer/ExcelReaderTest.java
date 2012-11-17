/*
 * ExcelReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.importer;

import java.sql.Types;
import java.util.List;

import workbench.TestUtil;

import workbench.db.ColumnIdentifier;
import workbench.db.exporter.XlsRowDataConverter;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;

import workbench.util.WbFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class ExcelReaderTest
{
	public ExcelReaderTest()
	{
	}

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
	}

	@Test
	public void testGetHeaderColumnsOOXML()
		throws Exception
	{

		WbFile data = createExcelFile("data.xlsx");

		try
		{
			ExcelReader reader = new ExcelReader(data, 0);
			reader.load();
			List<String> columns = reader.getHeaderColumns();
			assertNotNull(columns);
			assertEquals(3, columns.size());
			assertEquals("id", columns.get(0));
			assertEquals("firstname", columns.get(1));
			assertEquals("lastname", columns.get(2));

			assertEquals(3, reader.getRowCount());
		}
		finally
		{
			assertTrue(data.delete());
		}
	}

	@Test
	public void testGetHeaderColumnsXls()
		throws Exception
	{
		WbFile data = createExcelFile("data.xls");
		try
		{
			ExcelReader reader = new ExcelReader(data, 0);
			reader.load();
			List<String> columns = reader.getHeaderColumns();
			assertNotNull(columns);
			assertEquals(3, columns.size());
			assertEquals("id", columns.get(0));
			assertEquals("firstname", columns.get(1));
			assertEquals("lastname", columns.get(2));
		}
		finally
		{
			assertTrue(data.delete());
		}
	}

	private WbFile createExcelFile(String filename)
	{
		TestUtil util = new TestUtil("XlsHeaderReaderTest");
		WbFile data = new WbFile(util.getBaseDir(), filename);

		ColumnIdentifier[] cols = new ColumnIdentifier[3];
		cols[0] = new ColumnIdentifier("id", Types.INTEGER);
		cols[1] = new ColumnIdentifier("firstname", Types.VARCHAR);
		cols[2] = new ColumnIdentifier("lastname", Types.VARCHAR);
		ResultInfo info = new ResultInfo(cols);

		XlsRowDataConverter converter = new XlsRowDataConverter();
		converter.setOutputFile(data);
		if (filename.endsWith("xlsx"))
		{
			converter.setUseXLSX();
		}
		converter.setResultInfo(info);
		converter.getStart();
		int rownum = 0;

		RowData row = new RowData(info);
		row.setValue(0, Integer.valueOf(42));
		row.setValue(1, "Arthur");
		row.setValue(2, "Dent");
		converter.convertRowData(row, rownum ++);

		row.setValue(0, Integer.valueOf(43));
		row.setValue(1, "Ford");
		row.setValue(2, "Prefect");
		converter.convertRowData(row, rownum ++);

		converter.getEnd(rownum);
		return data;
	}
}
