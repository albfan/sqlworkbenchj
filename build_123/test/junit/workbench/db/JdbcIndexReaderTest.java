/*
 * JdbcIndexReaderTest.java
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
package workbench.db;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class JdbcIndexReaderTest
	extends WbTestCase
{
	public JdbcIndexReaderTest()
	{
		super("JdbcIndexReaderTest");
	}


	@After
	public void cleanup()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testGetTableIndexInformation()
		throws Exception
	{
		TestUtil util = getTestUtil();

		WbConnection con = util.getConnection();
		TestUtil.executeScript(con,
			"create table foo (id integer, code varchar(10));\n" +
			"create unique index idx_foo on foo (id, code);\n" +
			"commit;");

		TableIdentifier table = con.getMetadata().findTable(new TableIdentifier("FOO"));

		DataStore result = con.getMetadata().getIndexReader().getTableIndexInformation(table);
		assertNotNull(result);
		assertEquals(1, result.getRowCount());

		String name = result.getValueAsString(0, IndexReader.COLUMN_IDX_TABLE_INDEXLIST_INDEX_NAME);
		assertEquals("IDX_FOO", name);

		String columns = result.getValueAsString(0, IndexReader.COLUMN_IDX_TABLE_INDEXLIST_COL_DEF);
		assertEquals("ID ASC, CODE ASC", columns);

		String unique = result.getValueAsString(0, IndexReader.COLUMN_IDX_TABLE_INDEXLIST_UNIQUE_FLAG);
		assertEquals("YES", unique);

		String pk = result.getValueAsString(0, IndexReader.COLUMN_IDX_TABLE_INDEXLIST_PK_FLAG);
		assertEquals("NO", pk);

		String idxType = result.getValueAsString(0, IndexReader.COLUMN_IDX_TABLE_INDEXLIST_TYPE);
		assertEquals("NORMAL", idxType);
	}

}