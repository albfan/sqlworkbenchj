/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.gui.components;

import java.sql.Types;

import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;

import org.dbunit.dataset.Column;
import org.dbunit.dataset.ITableMetaData;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DBUnitTableAdapterTest
	extends WbTestCase
{

	public DBUnitTableAdapterTest()
	{
		super("DBUnitTableAdapterTest");
	}

	@Test
	public void testGetValue()
		throws Exception
	{
		ColumnIdentifier id = new ColumnIdentifier("ID", Types.INTEGER);
		id.setIsPkColumn(true);
		ColumnIdentifier fname = new ColumnIdentifier("FIRSTNAME", Types.VARCHAR);
		ColumnIdentifier lname = new ColumnIdentifier("LASTNAME", Types.VARCHAR);
		ColumnIdentifier[] c = {id, fname, lname};

		DataStore ds = new DataStore(new ResultInfo(c));
		ds.setUpdateTableToBeUsed(new TableIdentifier("PERSON"));
		int row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(42));
		ds.setValue(row, 1, "Arthur");
		ds.setValue(row, 2, "Dent");

		DBUnitTableAdapter adapter = new DBUnitTableAdapter(ds);
		assertEquals(1, adapter.getRowCount());
		ITableMetaData meta = adapter.getTableMetaData();
		assertNotNull(meta);
		assertEquals("PERSON", meta.getTableName());
		Column[] cols = meta.getColumns();
		assertNotNull(cols);
		assertEquals(3, cols.length);
		assertEquals("FIRSTNAME", cols[1].getColumnName());
		assertEquals(0, meta.getColumnIndex("ID"));
		assertEquals(1, meta.getColumnIndex("FIRSTNAME"));
		Column[] pk = meta.getPrimaryKeys();
		assertNotNull(pk);
		assertEquals(1, pk.length);
		assertEquals("ID", pk[0].getColumnName());
		Object name = adapter.getValue(0, "FIRSTNAME");
		assertEquals("Arthur", name);
	}


}
