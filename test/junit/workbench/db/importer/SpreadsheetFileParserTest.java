/*
 * SpreadsheetFileParserTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.importer;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SpreadsheetFileParserTest
	extends WbTestCase
{
	private final String filename = "data.ods";
	private int importedRows;
	private int colCount;

	public SpreadsheetFileParserTest()
	{
		super("TabularDataParser");
	}

	@Test
	public void testSecondSheet()
		throws Exception
	{
		TestUtil util = getTestUtil();
		File input = util.copyResourceFile(this, filename);

		SpreadsheetFileParser parser = new SpreadsheetFileParser();
		parser.setContainsHeader(true);
		parser.setReceiver(getReceiver());
		parser.setInputFile(input);
		parser.setSheetIndex(1);
		try
		{
			colCount = 0;
			importedRows = 0;

			parser.processOneFile();
			assertEquals(4, importedRows);
			assertEquals(4, colCount);
		}
		finally
		{
			parser.done();
		}
		assertTrue(input.delete());
	}

	@Test
	public void testFirstSheet()
		throws Exception
	{
		TestUtil util = getTestUtil();
		File input = util.copyResourceFile(this, filename);

		SpreadsheetFileParser parser = new SpreadsheetFileParser();
		parser.setContainsHeader(true);
		parser.setReceiver(getReceiver());
		parser.setInputFile(input);

		try
		{
			colCount = 0;
			importedRows = 0;

			parser.processOneFile();
			assertEquals(2, importedRows);
			assertEquals(6, colCount);
		}
		finally
		{
			parser.done();
		}
		if (input.exists())
		{
			assertTrue(input.delete());
		}
	}

	private DataReceiver getReceiver()
	{
		return new DataReceiver()
		{
			@Override
			public boolean getCreateTarget()
			{
				return false;
			}

			@Override
			public boolean shouldProcessNextRow()
			{
				return true;
			}

			@Override
			public void nextRowSkipped()
			{
			}

			@Override
			public void setTableList(List<TableIdentifier> targetTables)
			{
			}

			@Override
			public void deleteTargetTables()
				throws SQLException
			{
			}

			@Override
			public void beginMultiTable()
				throws SQLException
			{
			}

			@Override
			public void endMultiTable()
			{
			}

			@Override
			public void processFile(StreamImporter stream)
				throws SQLException, IOException
			{
			}

			@Override
			public void processRow(Object[] row)
				throws SQLException
			{
				importedRows ++;
				colCount = row.length;
			}

			@Override
			public void setTableCount(int total)
			{
			}

			@Override
			public void setCurrentTable(int current)
			{
			}

			@Override
			public void setTargetTable(TableIdentifier table, List<ColumnIdentifier> columns)
				throws SQLException
			{
			}

			@Override
			public void importFinished()
			{
			}

			@Override
			public void importCancelled()
			{
			}

			@Override
			public void tableImportError()
			{
			}

			@Override
			public void tableImportFinished()
				throws SQLException
			{
			}

			@Override
			public void recordRejected(String record, long importRow, Throwable e)
			{
			}
		};

	}
}
