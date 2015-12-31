/*
 * ColumnRemoverTest.java
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

import java.sql.Types;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.TestUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnRemoverTest
{

	@Test
	public void testRemoveColumns()
		throws Exception
	{
		TestUtil util = new TestUtil("ColumnRemoverTest");
		util.prepareEnvironment();

		String[] cols = new String[] {"NAME", "TYPE", "CATALOG", "SCHEMA", "REMARKS"};
		int types[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		int sizes[] = {30,12,10,10,20};

		DataStore ds = new DataStore(cols, types, sizes);
		int row = ds.addRow();
		ds.setValue(row, 0, "Name");
		ds.setValue(row, 1, "Type");
		ds.setValue(row, 2, "some_cat");
		ds.setValue(row, 3, "some_schema");
		ds.setValue(row, 4, "my comment");

		ColumnRemover remove = new ColumnRemover(ds);
		DataStore newDs = remove.removeColumnsByName("CATALOG", "SCHEMA");
		assertEquals(1, newDs.getRowCount());

		assertFalse(newDs.isModified());
		assertEquals(-1, newDs.getColumnIndex("CATALOG"));
		assertEquals(-1, newDs.getColumnIndex("SCHEMA"));
		assertEquals(0, newDs.getColumnIndex("NAME"));
		assertEquals(1, newDs.getColumnIndex("TYPE"));
		assertEquals(2, newDs.getColumnIndex("REMARKS"));

		assertEquals("Name", newDs.getValue(0, 0));
		assertEquals("Name", newDs.getValue(0, "NAME"));

		assertEquals("my comment", newDs.getValue(0, "REMARKS"));
		assertEquals("my comment", newDs.getValue(0, 2));

		assertEquals("Type", newDs.getValue(0, "TYPE"));
		assertEquals("Type", newDs.getValue(0, 1));
	}
}
