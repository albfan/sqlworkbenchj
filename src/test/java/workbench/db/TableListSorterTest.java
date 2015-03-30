/*
 * TableListSorterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db;

import java.sql.Types;
import org.junit.Test;

import workbench.WbTestCase;
import workbench.db.importer.DataStoreImporter;
import workbench.storage.DataStore;
import workbench.storage.RowDataListSorter;
import workbench.storage.SortDefinition;
import static org.junit.Assert.*;

import workbench.console.DataStorePrinter;
/**
 *
 * @author Thomas Kellerer
 */
public class TableListSorterTest
	extends WbTestCase
{
	public TableListSorterTest()
	{
		super("TableListSorterTest");
	}

	@Test
	public void testSort()
	{
		SortDefinition def = new SortDefinition();
		def.addSortColumn(DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, true);
		def.addSortColumn(DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, true);
		def.addSortColumn(DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, true);
		def.addSortColumn(DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, true);

		final TableListSorter sorter = new TableListSorter(def);
		sorter.setSortMViewAsTable(true);

		String[] cols = new String[] { "NAME", "TYPE", "CATALOG", "SCHEMA", "REMARKS" };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR,Types.VARCHAR,Types.VARCHAR,Types.VARCHAR};

		DataStore ds = new DataStore(cols, types)
		{
			@Override
			protected RowDataListSorter createSorter(SortDefinition sort)
			{
				return sorter;
			}
		};

		String data =
			"name\ttype\tcatalog\tschema\tremarks\n" +
			"001\tTABLE\tFOO\tPUBLIC\t\n"+
			"002\tTABLE\tFOO\tPUBLIC\t\n" +
 			"003\tMATERIALIZED VIEW\tFOO\tPUBLIC\t\n" +
			"004\tMATERIALIZED VIEW\tFOO\tPUBLIC\t\n" +
			"009\tMATERIALIZED VIEW\tFOO\tPUBLIC\t\n" +
			"005\tTABLE\tFOO\tPUBLIC\t\n" +
			"007\tVIEW\tFOO\tPUBLIC\t\n" +
			"006\tSEQUENCE\tFOO\tPUBLIC\t\n" +
			"009\tSYNONYM\tFOO\tPUBLIC\t\n" +
			"008\tTYPE\tFOO\tPUBLIC\t\n"
			;

		DataStoreImporter importer = new DataStoreImporter(ds, null, null);
		importer.importString(data);
		importer.startImport();

		ds.sort(def);

		DataStorePrinter printer = new DataStorePrinter(ds);
		printer.printTo(System.out);

		String name = ds.getValueAsString(0, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		assertEquals("006", name);

		name = ds.getValueAsString(1, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		assertEquals("009", name);

		name = ds.getValueAsString(2, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		assertEquals("001", name);

		name = ds.getValueAsString(3, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		assertEquals("002", name);

		name = ds.getValueAsString(8, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		assertEquals("008", name);
	}
}