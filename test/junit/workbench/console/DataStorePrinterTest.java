/*
 * DataStorePrinterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.console;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Types;

import workbench.WbTestCase;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DataStorePrinterTest
	extends WbTestCase
{
	public DataStorePrinterTest()
	{
		super("DataStorePrinterTest");
	}

	@BeforeClass
	public static void init()
	{
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

	@Test
	public void testSelectedColumns1()
	{
		DataStore ds = createTestData();

		DataStorePrinter printer = new DataStorePrinter(ds);
		printer.setColumnsToPrint(CollectionUtil.arrayList("DESCRIPTION", "QUANTITY"));
    StringWriter sw = new StringWriter(500);
    PrintWriter pw = new PrintWriter(sw);
		printer.printTo(pw);
		String out = sw.toString();

		String[] lines = out.split(StringUtil.LINE_TERMINATOR);
		int linecount = lines.length;
		assertEquals(10, linecount);

		assertEquals("DESCRIPTION          | QUANTITY", lines[0]);
		assertEquals("---------------------+---------", lines[1]);
		assertEquals("Very long test value |        1", lines[2]);
		assertEquals("Multi-line           |        2", lines[3]);
		assertEquals("test value"                     , lines[4]);
		assertEquals("My comment           |        3", lines[5]);
	}

	private DataStore createTestData2()
	{
		String[] cols = new String[] { "ID", "FIRSTNAME", "LASTNAME"};
		int[] types = new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR };
		int[] sizes = new int[] { 10, 5, 25 };

		DataStore ds = new DataStore(cols, types, sizes);
		int row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(1));
		ds.setValue(row, 1, "Zaphod");
		ds.setValue(row, 2, "Beeblebrox");

		row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(2));
		ds.setValue(row, 1, "Ford");
		ds.setValue(row, 2, "Prefect");

		row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(3));
		ds.setValue(row, 1, "Arthur");
		ds.setValue(row, 2, "Dent");

		return ds;
	}

	@Test
	public void testSelectedColumns2()
	{
		DataStore ds = createTestData2();

		DataStorePrinter printer = new DataStorePrinter(ds);
		printer.setColumnsToPrint(CollectionUtil.arrayList("FIRSTNAME", "LASTNAME"));

    StringWriter sw = new StringWriter(500);
    PrintWriter writer = new PrintWriter(sw);
		printer.printTo(writer);
		String out = sw.toString();

		String[] lines = out.split(StringUtil.LINE_TERMINATOR);
		int linecount = lines.length;
		assertEquals(7, linecount);

		assertEquals("FIRSTNAME | LASTNAME  ", lines[0]);
		assertEquals("----------+-----------", lines[1]);
		assertEquals("Zaphod    | Beeblebrox", lines[2]);
		assertEquals("Ford      | Prefect   ", lines[3]);
		assertEquals("Arthur    | Dent      ", lines[4]);
	}

	@Test
	public void testTabularPrint()
	{
		DataStore ds = createTestData();

		DataStorePrinter printer = new DataStorePrinter(ds);

    StringWriter sw = new StringWriter(500);
    PrintWriter pw = new PrintWriter(sw);
		printer.printTo(pw);
		String out = sw.toString();

		String[] lines = out.split(StringUtil.LINE_TERMINATOR);
		int linecount = lines.length;
		assertEquals(11, linecount);

		assertEquals("DESCRIPTION          | QUANTITY | LASTNAME      ", lines[0]);
		assertEquals("---------------------+----------+---------------", lines[1]);
		assertEquals("Very long test value |        1 | Beeblebrox    ", lines[2]);
		assertEquals("Multi-line           |        2 | Dent on       ", lines[3]);
		assertEquals("test value                      : two lines",      lines[4]);
		assertEquals("My comment           |        3 | lastname      ", lines[5]);
		assertEquals("                                : with two lines", lines[6]);

    sw = new StringWriter(500);
    pw = new PrintWriter(sw);
		printer = new DataStorePrinter(ds);
		printer.setFormatColumns(false);
		printer.setPrintRowCount(false);
		printer.printTo(pw);
		out = sw.toString();

		lines = out.split(StringUtil.LINE_TERMINATOR);
		linecount = lines.length;
//		System.out.println(out);
		assertEquals(ds.getRowCount() + 1, linecount);
		assertEquals("DESCRIPTION | QUANTITY | LASTNAME", lines[0]);
		assertEquals("Very long test value | 1 | Beeblebrox", lines[1]);
		assertEquals("Multi-line\\ntest value | 2 | Dent on\\ntwo lines", lines[2]);
		assertEquals("My comment | 3 | lastname \\nwith two lines", lines[3]);
	}

	@Test
	public void testRecordPrint()
	{
		DataStore ds = createTestData();
		DataStorePrinter printer = new DataStorePrinter(ds);
		printer.setPrintRowsAsLine(false);

    StringWriter sw = new StringWriter(500);
    PrintWriter pw = new PrintWriter(sw);
		printer.printTo(pw);
		String out = sw.toString();

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
