/*
 * LookupDataLoaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class LookupDataLoaderTest
extends WbTestCase
{
	public LookupDataLoaderTest()
	{
		super("FkValuesRetrieverTest");
	}

	@Before
	public void setUp()
		throws Exception
	{
	}

	@After
	public void tearDown()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testGetReferencedTable()
		throws Exception
	{
		WbConnection conn = getTestUtil().getConnection();
		TestUtil.executeScript(conn,
			"create table address_type (type_id integer primary key, description varchar(100));\n" +
			"create table address (\n" +
			"     id integer, \n" +
			"     adr_type_id integer, \n" +
			"     details varchar(100), \n" +
			" foreign key (adr_type_id) references address_type (type_id) \n" +
			"); \n" +
			"insert into address_type values (3, 'work');\n" +
			"insert into address_type values (2, 'private'); \n"  +
			"insert into address_type values (1, 'delivery'); \n"  +
			"commit;"
			);

		TableIdentifier tbl = conn.getMetadata().findTable(new TableIdentifier("ADDRESS"));
		LookupDataLoader retriever = new LookupDataLoader(tbl, "ADR_TYPE_ID");
		retriever.retrieveReferencedTable(conn);
		TableIdentifier result = retriever.getLookupTable();
		assertNotNull(result);
		assertEquals("ADDRESS_TYPE", result.getTableName());
		DataStore data = retriever.getLookupData(conn, 0, null, true);
		assertNotNull(data);
		assertEquals(3, data.getRowCount());
		assertEquals(1, data.getValueAsInt(0, 0, -1));
		assertEquals(2, data.getValueAsInt(1, 0, -1));
		assertEquals(3, data.getValueAsInt(2, 0, -1));
	}

}
