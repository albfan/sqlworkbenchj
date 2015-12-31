/*
 * ExcelReaderTest.java
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
package workbench.db.importer;

import java.io.File;
import java.sql.Types;
import java.text.SimpleDateFormat;
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
	private final	String dtFmt = "yyyy-MM-dd";
	private final	String tsFmt = "yyyy-MM-dd HH:mm:ss";
	private final	SimpleDateFormat dtFormat = new SimpleDateFormat(dtFmt);
	private final SimpleDateFormat tsFormat = new SimpleDateFormat(tsFmt);

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
	public void testSwitchSheets()
		throws Exception
	{
		TestUtil util = new TestUtil("ExcelReader");
		File input = util.copyResourceFile(this, "data.xls");
		ExcelReader reader = new ExcelReader(input, 1, null);
		try
		{
			reader.load();
			assertEquals(5, reader.getRowCount());
			reader.setActiveWorksheet("orders");
			assertEquals(5, reader.getRowCount());
			reader.setActiveWorksheet("person");
			assertEquals(3, reader.getRowCount());
		}
		finally
		{
			reader.done();
		}
		assertTrue(input.delete());
	}

	@Test
	public void testXlsX()
		throws Exception
	{

		WbFile data = createExcelFile("data.xlsx");

		try
		{
			ExcelReader reader = new ExcelReader(data, 0, null);
			reader.load();
			List<String> columns = reader.getHeaderColumns();
			assertNotNull(columns);
			assertEquals(5, columns.size());
			assertEquals("id", columns.get(0));
			assertEquals("firstname", columns.get(1));
			assertEquals("lastname", columns.get(2));
			assertEquals("hire_date", columns.get(3));
			assertEquals("last_login", columns.get(4));
			assertEquals(3, reader.getRowCount());

			List<Object> values = reader.getRowValues(1);
			java.sql.Date dt = new java.sql.Date(dtFormat.parse("2012-01-01").getTime());
			java.sql.Timestamp ts = new java.sql.Timestamp(tsFormat.parse("2012-01-01 18:19:20").getTime());
			assertEquals(dt, values.get(3));
			assertEquals(ts, values.get(4));

			values = reader.getRowValues(2);
			dt = new java.sql.Date(dtFormat.parse("2010-01-01").getTime());
			ts = new java.sql.Timestamp(tsFormat.parse("2010-02-03 16:17:18").getTime());
			assertEquals(dt, values.get(3));
			assertEquals(ts, values.get(4));
		}
		finally
		{
			assertTrue(data.delete());
		}
	}

	@Test
	public void testXls()
		throws Exception
	{
		WbFile data = createExcelFile("data.xls");
		try
		{
			ExcelReader reader = new ExcelReader(data, 0, null);
			reader.load();
			List<String> columns = reader.getHeaderColumns();
			assertNotNull(columns);
			assertEquals(5, columns.size());
			assertEquals("id", columns.get(0));
			assertEquals("firstname", columns.get(1));
			assertEquals("lastname", columns.get(2));
			assertEquals("hire_date", columns.get(3));
			assertEquals("last_login", columns.get(4));
		}
		finally
		{
			assertTrue(data.delete());
		}
	}

	private WbFile createExcelFile(String filename)
		throws Exception
	{
		TestUtil util = new TestUtil("ExcelReader");
		WbFile data = new WbFile(util.getBaseDir(), filename);

		ColumnIdentifier[] cols = new ColumnIdentifier[5];
		cols[0] = new ColumnIdentifier("id", Types.INTEGER);
		cols[1] = new ColumnIdentifier("firstname", Types.VARCHAR);
		cols[2] = new ColumnIdentifier("lastname", Types.VARCHAR);
		cols[3] = new ColumnIdentifier("hire_date", Types.DATE);
		cols[4] = new ColumnIdentifier("last_login", Types.TIMESTAMP);
		ResultInfo info = new ResultInfo(cols);

		XlsRowDataConverter converter = new XlsRowDataConverter();
		converter.setOutputFile(data);
		converter.setDefaultDateFormat(dtFmt);
		converter.setDefaultTimestampFormat(tsFmt);

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
		row.setValue(3, new java.sql.Date(dtFormat.parse("2012-01-01").getTime()));
		row.setValue(4, new java.sql.Timestamp(tsFormat.parse("2012-01-01 18:19:20").getTime()));

		converter.convertRowData(row, rownum ++);

		row.setValue(0, Integer.valueOf(43));
		row.setValue(1, "Ford");
		row.setValue(2, "Prefect");
		row.setValue(3, new java.sql.Date(dtFormat.parse("2010-01-01").getTime()));
		row.setValue(4, new java.sql.Timestamp(tsFormat.parse("2010-02-03 16:17:18").getTime()));
		converter.convertRowData(row, rownum ++);

		converter.getEnd(rownum);
		return data;
	}
}
