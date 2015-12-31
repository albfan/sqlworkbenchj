/*
 * DataStoreReplacerTest.java
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
package workbench.storage;

import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class DataStoreReplacerTest
	extends WbTestCase
{

	public DataStoreReplacerTest()
	{
		super("DataStoreReplacerTest");
	}

	@Test
	public void testReplaceSelection()
	{
		try
		{
			String[] cols = new String[] { "firstname", "lastname", "numeric" };
			int[] types = new int[] { java.sql.Types.VARCHAR, java.sql.Types.VARCHAR, java.sql.Types.INTEGER };

			DataStore ds = new DataStore(cols, types);
			for (int i = 0; i < 20; i++)
			{
				int row = ds.addRow();
				ds.setValue(row, 0, "First" + Integer.toString(i));
				ds.setValue(row, 1, "Last" + Integer.toString(i));
				ds.setValue(row, 2, new Integer(i));
			}
			int[] selected = new int[] { 0,1,2 };
			DataStoreReplacer replacer = new DataStoreReplacer(ds);
			replacer.setSelectedRows(selected);
			Position pos = replacer.find("2", true, true, false);
			assertEquals("Value not found", new Position(2,2), pos);

			pos = replacer.find("5", true, true, false);
			assertEquals("Value outside selection found", Position.NO_POSITION, pos);

			int replaced = replacer.replaceAll("First", "Other", selected, true, false, false);
			assertEquals("Wrong number of values replaced", 3, replaced);

			Object value = ds.getValue(0, 0);
			assertEquals("Value not replaced", "Other0", value);

			value = ds.getValue(6, 0);
			assertEquals("Wrong row value replaced", "First6", value);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testReplace()
	{
		try
		{
			String[] cols = new String[] { "firstname", "lastname", "numeric" };
			int[] types = new int[] { java.sql.Types.VARCHAR, java.sql.Types.VARCHAR, java.sql.Types.INTEGER };

			DataStore ds = new DataStore(cols, types);

			int row = ds.addRow();
			ds.setValue(row, 0, "Arthur");
			ds.setValue(row, 1, "Dent");
			ds.setValue(row, 2, new Integer(5));

			row = ds.addRow();
			ds.setValue(row, 0, "Zaphod");
			ds.setValue(row, 1, "Beeblebrox");
			ds.setValue(row, 2, new Integer(12));

			int mary1Row = ds.addRow();
			ds.setValue(mary1Row, 0, "Mary");
			ds.setValue(mary1Row, 1, "Moviestar");
			ds.setValue(mary1Row, 2, new Integer(23));

			int mary2Row= ds.addRow();
			ds.setValue(mary2Row, 0, "Mary");
			ds.setValue(mary2Row, 1, "Poppins");
			ds.setValue(mary2Row, 2, new Integer(42));

			DataStoreReplacer replacer = new DataStoreReplacer(ds);
			int replaced = replacer.replaceAll("Mary", "Yram", null, true, false, false);
			assertEquals("Wrong number of replacements", 2, replaced);

			String value = ds.getValueAsString(mary1Row, 0);
			assertEquals("Wrong new value", "Yram", value);

			try
			{
				replacer.replaceAll("42", "gaga", null, false, true, false);
				fail("No ConverterException thrown");
			}
			catch (Exception e)
			{
			}

			value = ds.getValueAsString(mary2Row, 2);
			assertEquals("Value was replaced", "42", value);

			try
			{
				replacer.replaceAll("42", "24", null, false, true, false);
				value = ds.getValueAsString(mary2Row, 2);
				assertEquals("Value was not replaced", "24", value);
			}
			catch (Exception e)
			{
				fail("Replacement threw an exception");
			}

		}
		catch (Exception e)
		{
			fail(e.getMessage());
		}
	}

	@Test
	public void testFind()
	{
		try
		{
			String[] cols = new String[] { "firstname", "lastname", "numeric" };
			int[] types = new int[] { java.sql.Types.VARCHAR, java.sql.Types.VARCHAR, java.sql.Types.INTEGER };

			DataStore ds = new DataStore(cols, types);

			int row = ds.addRow();
			ds.setValue(row, 0, "Arthur");
			ds.setValue(row, 1, "Dent");
			ds.setValue(row, 2, new Integer(5));

			row = ds.addRow();
			ds.setValue(row, 0, "Zaphod");
			ds.setValue(row, 1, "Beeblebrox");
			ds.setValue(row, 2, new Integer(12));

			int mary1Row = ds.addRow();
			ds.setValue(mary1Row, 0, "Mary");
			ds.setValue(mary1Row, 1, "Moviestar");
			ds.setValue(mary1Row, 2, new Integer(23));

			int mary2Row= ds.addRow();
			ds.setValue(mary2Row, 0, "Mary");
			ds.setValue(mary2Row, 1, "Poppins");
			ds.setValue(mary2Row, 2, new Integer(42));

			DataStoreReplacer instance = new DataStoreReplacer(ds);

			Position expResult = new Position(0,1);
			Position result = instance.find("dent", true, false, false);
			assertEquals("Value not found", expResult, result);


			result = instance.find("gaga", true, false, false);
			assertEquals("Value found", Position.NO_POSITION, result);


			result = instance.find("Mary", false, false, false);
			assertEquals("First value not found", new Position(mary1Row, 0), result);

			result = instance.findNext();
			assertEquals("Second value not found", new Position(mary2Row, 0), result);

			result = instance.find("12", true, false, false);
			assertEquals("Numeric value not found", new Position(1,2), result);

			result = instance.find("M[a|o]", true, false, true);
			assertEquals("Regex not found", new Position(2,0), result);
			result = instance.findNext();
			assertEquals("Regex not found", new Position(2,1), result);
			result = instance.findNext();
			assertEquals("Regex not found", new Position(3,0), result);
			result = instance.findNext();
			assertEquals("Regex found", Position.NO_POSITION, result);

			result = instance.find("[1|4]2", true, false, true);
			assertEquals("Regex not found", new Position(1,2), result);
			result = instance.findNext();
			assertEquals("Regex not found", new Position(mary2Row,2), result);
			result = instance.findNext();
			assertEquals("Regex found", Position.NO_POSITION, result);

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


}
