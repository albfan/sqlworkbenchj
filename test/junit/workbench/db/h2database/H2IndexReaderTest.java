/*
 * H2IndexReaderTest.java
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
package workbench.db.h2database;

import org.junit.AfterClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.ConnectionMgr;
import workbench.db.IndexReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;
import workbench.db.*;

/**
 *
 * @author Thomas Kellerer
 */
public class H2IndexReaderTest
	extends WbTestCase
{

	public H2IndexReaderTest()
	{
		super("H2IndexReaderTest");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testGetPrimaryKeyIndex()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();
		TestUtil.executeScript(con, "create table person (id integer primary key, person_name varchar(100));");
		TableIdentifier tbl = new TableIdentifier("PERSON");
		IndexReader reader = con.getMetadata().getIndexReader();
		assertTrue(reader instanceof H2IndexReader);
		PkDefinition pk = reader.getPrimaryKey(tbl);
		assertNotNull(pk);
		assertEquals(1, pk.getColumns().size());
		assertEquals("ID", pk.getColumns().get(0));
		assertTrue(pk.getPkName().startsWith("CONSTRAINT"));
	}
}
