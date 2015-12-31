/*
 * OracleControlFileWriterTest.java
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
package workbench.db.oracle;

import java.sql.Types;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.exporter.DataExporter;
import workbench.db.exporter.RowDataConverter;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;

import workbench.util.StringUtil;
import workbench.util.WbFile;

import org.junit.Test;

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
				return "\\t";
			}

			@Override
			public String getTextQuoteChar()
			{
				return "\"";
			}

			@Override
			public String getTableNameToUse()
			{
				return "person";
			}
		};

		exporter.setTimestampFormat("yyyy_MM_dd");
		exporter.setEncoding("UTF-8");

		ColumnIdentifier id = new ColumnIdentifier("id" ,Types.INTEGER, true);
		ColumnIdentifier firstname = new ColumnIdentifier("firstname", Types.VARCHAR);
		ColumnIdentifier lastname = new ColumnIdentifier("lastname", Types.VARCHAR);
		ColumnIdentifier birthday = new ColumnIdentifier("birthday", Types.DATE);
		ColumnIdentifier salary = new ColumnIdentifier("salary", Types.NUMERIC);
		salary.setDbmsType("number(12,2)");

		final TableIdentifier table = new TableIdentifier("person");

		final ResultInfo info = new ResultInfo(new ColumnIdentifier[] { id, firstname, lastname, birthday, salary } );
		info.setUpdateTable(table);

		RowDataConverter converter = new RowDataConverter()
		{
			@Override
			public ResultInfo getResultInfo()
			{
				return info;
			}

			@Override
			public StringBuilder convertRowData(RowData row, long rowIndex)
			{
				return new StringBuilder();
			}

			@Override
			public StringBuilder getStart()
			{
				return null;
			}

			@Override
			public StringBuilder getEnd(long totalRows)
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
			List<String> lines = StringUtil.readLines(formatFile);
			assertNotNull(lines);
			assertEquals(16, lines.size());

			// First line is a comment so the actual contents starts with the second line
			assertEquals("OPTIONS (skip=1, direct=true, rows=10000, silent=(feedback) )", lines.get(1));
			assertEquals("LOAD DATA CHARACTERSET 'AL32UTF8'", lines.get(3));
			assertEquals("INFILE 'export.txt'", lines.get(4));
			assertEquals("INTO TABLE person", lines.get(7));
			assertEquals("FIELDS TERMINATED BY '\\t' TRAILING NULLCOLS", lines.get(8));

			assertEquals("(", lines.get(9).trim());
			assertEquals("  id,", lines.get(10));
			assertEquals("  firstname,", lines.get(11));
			assertEquals("  lastname,", lines.get(12));
			assertEquals("  birthday  DATE \"YYYY_MM_DD\",", lines.get(13));
			assertEquals("  salary    TERMINATED BY WHITESPACE", lines.get(14));
			assertEquals(")", lines.get(15).trim());
		}
		finally
		{
			util.emptyBaseDirectory();
		}
	}
}
