/*
 * DataPrinterTest.java
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
package workbench.storage;

import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Types;

import workbench.WbTestCase;
import workbench.resource.Settings;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DataPrinterTest
	extends WbTestCase
{

	public DataPrinterTest()
	{
		super("DataPrinterTest");
	}

	@Test
	public void testWriteDataString()
		throws Exception
	{
		int[] types = new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR };
		String[] names = new String[] {"ID", "FIRSTNAME", "LASTNAME" };
		DataStore ds = new DataStore(names, types);
		int row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(1));
		ds.setValue(row, 1, "Arthur");
		ds.setValue(row, 2, "Dent");

		row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(2));
		ds.setValue(row, 1, "Zaphod");
		ds.setValue(row, 2, "Beeblebrox");

		Writer out = new StringWriter(50);

		DataPrinter printer = new DataPrinter(ds, ";", "\n", null, true);
		printer.writeDataString(out, null);
		assertEquals("ID;FIRSTNAME;LASTNAME\n1;Arthur;Dent\n2;Zaphod;Beeblebrox\n", out.toString());

		out = new StringWriter(50);
		printer.writeDataString(out, new int[] {1} );
		assertEquals("ID;FIRSTNAME;LASTNAME\n2;Zaphod;Beeblebrox\n", out.toString());

		int[] colMap = new int[] { 2, 1, 0 };
		printer.setColumnMapping(colMap);

		out = new StringWriter(50);
		printer.writeDataString(out, new int[] {0} );
		assertEquals("LASTNAME;FIRSTNAME;ID\nDent;Arthur;1\n", out.toString());
	}

	@Test
	public void testDecimal()
		throws Exception
	{
		int[] types = new int[] {Types.DECIMAL, Types.VARCHAR };
		String[] names = new String[] {"PRICE", "PRODUCT" };
		DataStore ds = new DataStore(names, types);
		int row = ds.addRow();
		ds.setValue(row, 0, new BigDecimal("1.234"));
		ds.setValue(row, 1, "Foo");

		row = ds.addRow();
		ds.setValue(row, 0, new BigDecimal("3.14"));
		ds.setValue(row, 1, "Bar");

		Writer out = new StringWriter(50);

		String decimal = Settings.getInstance().getDecimalSymbol();
		int digits = Settings.getInstance().getMaxFractionDigits();

		try
		{
			Settings.getInstance().setMaxFractionDigits(0);
			Settings.getInstance().setDecimalSymbol(".");
			DataPrinter printer = new DataPrinter(ds, ";", "\n", null, true);
			printer.writeDataString(out, null);
			assertEquals("PRICE;PRODUCT\n1.234;Foo\n3.14;Bar\n", out.toString());

			Settings.getInstance().setDecimalSymbol(",");
			printer = new DataPrinter(ds, ";", "\n", null, true);
			out = new StringWriter(50);
			printer.writeDataString(out, null);
			assertEquals("PRICE;PRODUCT\n1,234;Foo\n3,14;Bar\n", out.toString());
		}
		finally
		{
			Settings.getInstance().setDecimalSymbol(decimal);
			Settings.getInstance().setMaxFractionDigits(digits);
		}
	}
}
