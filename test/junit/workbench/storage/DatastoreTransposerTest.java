/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.sql.Types;

import workbench.WbTestCase;

import workbench.util.CollectionUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DatastoreTransposerTest
	extends WbTestCase
{

	public DatastoreTransposerTest()
	{
		super("DatastoreTransposerTest");
	}

  @Test
  public void testLabelColumn()
  {
		String[] cols = new String[] {"dept", "count", "avg_salary"};
		int[] types = new int[] { Types.VARCHAR, Types.INTEGER, Types.INTEGER };
		DataStore data = new DataStore(cols, types);
    data.setResultName("Report");
		int row = data.addRow();
		data.setValue(row, 0, "Finance");
		data.setValue(row, 1, 5);
		data.setValue(row, 2, 5000);

		row = data.addRow();
		data.setValue(row, 0, "HR");
		data.setValue(row, 1, 1);
		data.setValue(row, 2, 2500);

		row = data.addRow();
		data.setValue(row, 0, "IT");
		data.setValue(row, 1, 12);
		data.setValue(row, 2, 6400);

		row = data.addRow();
		data.setValue(row, 0, "DEV");
		data.setValue(row, 1, 42);
		data.setValue(row, 2, 4200);

		row = data.addRow();
		data.setValue(row, 0, "Support");
		data.setValue(row, 1, 2);
		data.setValue(row, 2, 3000);

		DatastoreTransposer transposer = new DatastoreTransposer(data);
    DataStore result = transposer.transposeWithLabel("dept", null, null);

//    expected output:
//                | Finance | HR   | IT     | DEV   | Support
//    ----------- +---------+------+------  +------ +--------
//    count       | 5       | 1    | 12     | 42    | 2
//    avg_salary  | 5000    | 2500 | 6400   | 4200  | 3000

//		DataStorePrinter printer = new DataStorePrinter(result);
//		printer.printTo(System.out);

    assertNotNull(result);
    assertEquals(2, result.getRowCount());
    assertEquals("Finance", result.getColumnName(1));
    assertEquals("HR", result.getColumnName(2));
    assertEquals("IT", result.getColumnName(3));
    assertEquals("DEV", result.getColumnName(4));
    assertEquals("Support", result.getColumnName(5));

    assertEquals("count", result.getValueAsString(0,0));
    assertEquals("avg_salary", result.getValueAsString(1,0));
  }

	@Test
	public void testTransposeRows()
	{
		DataStore data = createDataStore();

		int[] rows = new int[] {0,2};
		DatastoreTransposer transposer = new DatastoreTransposer(data);
		DataStore result = transposer.transposeRows(rows);

//		DataStorePrinter printer = new DataStorePrinter(result);
//		printer.printTo(System.out);

		assertEquals(3, result.getRowCount());
		assertEquals(3, result.getColumnCount());

		assertEquals("Row 1", result.getColumnDisplayName(1));
		assertEquals("Row 3", result.getColumnDisplayName(2));

		assertEquals("1", result.getValueAsString(0, 1));
		assertEquals("Arthur", result.getValueAsString(1, 1));
		assertEquals("Dent", result.getValueAsString(2, 1));

		rows = new int[] {0,1,2};
		transposer = new DatastoreTransposer(data);
		result = transposer.transposeRows(rows);

//		DataStorePrinter printer = new DataStorePrinter(result);
//		printer.printTo(System.out);

		assertEquals(3, result.getRowCount());
		assertEquals(4, result.getColumnCount());
	}

	@Test
	public void testExcludeColumns()
	{
		DataStore data = createDataStore();
		int[] rows = new int[] {0,1,2};
		DatastoreTransposer transposer = new DatastoreTransposer(data);
		transposer.setColumnsToExclude(CollectionUtil.caseInsensitiveSet("firstname"));
		DataStore result = transposer.transposeRows(rows);
//		DataStorePrinter printer = new DataStorePrinter(result);
//		printer.printTo(System.out);
		assertEquals(2, result.getRowCount());
		assertEquals(4, result.getColumnCount());
	}

	private DataStore createDataStore()
	{
		String[] cols = new String[] {"id", "firstname", "lastname" };
		int[] types = new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR };

		DataStore data = new DataStore(cols, types);
		int row = data.addRow();
		data.setValue(row, 0, Integer.valueOf(1));
		data.setValue(row, 1, "Arthur");
		data.setValue(row, 2, "Dent");

		row = data.addRow();
		data.setValue(row, 0, Integer.valueOf(2));
		data.setValue(row, 1, "Ford");
		data.setValue(row, 2, "Prefect");

		row = data.addRow();
		data.setValue(row, 0, Integer.valueOf(3));
		data.setValue(row, 1, "Tricia");
		data.setValue(row, 2, "McMillan");
		data.setResultName("Person");
		return data;
	}
}
