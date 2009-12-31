/*
 * DataStorePrinterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Types;
import workbench.WbTestCase;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DataStorePrinterTest
	extends WbTestCase
{
	public DataStorePrinterTest(String testName)
	{
		super(testName);
	}

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
		System.setProperty("workbench.gui.language", "en");
	}

	private DataStore createTestData()
	{
		String[] cols = new String[] { "DESCRIPTION", "QUANTITY", "LASTNAME"};
		int[] types = new int[] { Types.VARCHAR, Types.INTEGER, Types.VARCHAR };
		int[] sizes = new int[] { 10, 5, 25 };

		DataStore ds = new DataStore(cols, types, sizes);
		int row = ds.addRow();
		ds.setValue(row, 0, "Very long test value");
		ds.setValue(row, 1, Integer.valueOf(1));
		ds.setValue(row, 2, "Beeblebrox");

		row = ds.addRow();
		ds.setValue(row, 0, "Multi-line\ntest value");
		ds.setValue(row, 1, Integer.valueOf(2));
		ds.setValue(row, 2, "Dent on\ntwo lines");

		row = ds.addRow();
		ds.setValue(row, 0, "My comment");
		ds.setValue(row, 1, Integer.valueOf(3));
		ds.setValue(row, 2, "lastname \nwith two lines");

		row = ds.addRow();
		ds.setValue(row, 0, "Some Comment");
		ds.setValue(row, 1, null);
		ds.setValue(row, 2, "Some name");

		row = ds.addRow();
		ds.setValue(row, 0, "Some Comment");
		ds.setValue(row, 1, Integer.valueOf(5));
		ds.setValue(row, 2, null);
		return ds;
	}


	public void testTabularPrint()
	{
		DataStore ds = createTestData();

		DataStorePrinter printer = new DataStorePrinter(ds);
		ByteArrayOutputStream ba = new ByteArrayOutputStream(500);
		PrintStream ps = new PrintStream(ba);
		printer.printTo(ps);
		String out = ba.toString();
		ps.close();
//		System.out.println(out);
		String[] lines = out.split(StringUtil.LINE_TERMINATOR);
		int linecount = lines.length;
		assertEquals(11, linecount);

		assertEquals("DESCRIPTION           | QUANTITY | LASTNAME      ", lines[0]);
		assertEquals("----------------------+----------+---------------", lines[1]);
		assertEquals("Very long test value  | 1        | Beeblebrox    ", lines[2]);
		assertEquals("Multi-line            | 2        | Dent on       ", lines[3]);
		assertEquals("test value                       : two lines",      lines[4]);
		assertEquals("My comment            | 3        | lastname      ", lines[5]);
		assertEquals("                                 : with two lines", lines[6]);

		// Test the unformatted output
		ba = new ByteArrayOutputStream(500);
		ps = new PrintStream(ba);
		printer = new DataStorePrinter(ds);
		printer.setFormatColumns(false);
		printer.setPrintRowCount(false);
		printer.printTo(ps);
		out = ba.toString();
		ps.close();
		lines = out.split(StringUtil.LINE_TERMINATOR);
		linecount = lines.length;
//		System.out.println(out);
		assertEquals(ds.getRowCount() + 1, linecount);
		assertEquals("DESCRIPTION | QUANTITY | LASTNAME", lines[0]);
		assertEquals("Very long test value | 1 | Beeblebrox", lines[1]);
		assertEquals("Multi-line\\ntest value | 2 | Dent on\\ntwo lines", lines[2]);
		assertEquals("My comment | 3 | lastname \\nwith two lines", lines[3]);
	}

	public void testRecordPrint()
	{
		DataStore ds = createTestData();
		DataStorePrinter printer = new DataStorePrinter(ds);
		printer.setPrintRowsAsLine(false);
		ByteArrayOutputStream ba = new ByteArrayOutputStream(500);
		PrintStream ps = new PrintStream(ba);
		printer.printTo(ps);
		String out = ba.toString();
//		System.out.println(out);
		ps.close();
		String[] lines = out.split(StringUtil.LINE_TERMINATOR);
		int linecount = lines.length;
		assertEquals(25, linecount);
		assertEquals("---- [Row 1] -------------------------------", lines[0]);
		assertEquals("DESCRIPTION : Very long test value", lines[1]);
		assertEquals("QUANTITY    : 1", lines[2]);
		assertEquals("LASTNAME    : Beeblebrox", lines[3]);
		assertEquals("---- [Row 2] -------------------------------", lines[4]);
		assertEquals("DESCRIPTION : Multi-line", lines[5]);
		assertEquals("              test value", lines[6]);
		assertEquals("QUANTITY    : 2", lines[7]);
		assertEquals("LASTNAME    : Dent on", lines[8]);
		assertEquals("              two lines", lines[9]);
		
	}
}
