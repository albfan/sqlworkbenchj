/*
 * DataStorePrinterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.console;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Types;
import junit.framework.TestCase;
import workbench.storage.DataStore;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class DataStorePrinterTest
	extends TestCase
{
	public DataStorePrinterTest(String testName)
	{
		super(testName);
	}

	public void testPrintTo()
	{
		String[] cols = new String[] { "DESCRIPTION", "QUANTITY", "LASTNAME"};
		int[] types = new int[] { Types.VARCHAR, Types.INTEGER, Types.VARCHAR };
		int[] sizes = new int[] { 10, 5, 25 };

		DataStore ds = new DataStore(cols, types, sizes);
		int row = ds.addRow();
		ds.setValue(row, 0, "Very long test value");
		ds.setValue(row, 1, Integer.valueOf(1));
		ds.setValue(row, 2, "Beeblebrox");
		DataStorePrinter printer = new DataStorePrinter(ds);
		
		ByteArrayOutputStream ba = new ByteArrayOutputStream(500);
		PrintStream ps = new PrintStream(ba);
		printer.printTo(ps);
		String out = ba.toString();
		String[] lines = out.split(StringUtil.LINE_TERMINATOR);
		assertEquals(3, lines.length);
		assertEquals("DESCRIPTION          | QUANTITY | LASTNAME  ", lines[0]);
		assertEquals("---------------------+----------+-----------", lines[1]);
		assertEquals("Very long test value | 1        | Beeblebrox", lines[2]);
	}
}
