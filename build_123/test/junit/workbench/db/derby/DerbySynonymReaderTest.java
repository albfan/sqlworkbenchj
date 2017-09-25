/*
 * DerbySynonymReaderTest.java
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
package workbench.db.derby;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.*;

import static org.junit.Assert.*;


/**
 *
 * @author Thomas Kellerer
 */
public class DerbySynonymReaderTest
	extends WbTestCase
{

	public DerbySynonymReaderTest()
	{
		super("DerbySynonymReaderTest");
	}

	@After
	public void tearDown()
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@AfterClass
	public static void afterClass()
	{
		DerbyTestUtil.clearProperties();
	}

	@Test
	public void testGetSynonymList()
		throws Exception
	{
		WbConnection con = DerbyTestUtil.getDerbyConnection(getTestUtil().getBaseDir());
		String sql =
			"create table foo (bar integer);\n" +
			"create synonym foobar for foo;\n";

		TestUtil.executeScript(con, sql);
		con.commit();

		List<TableIdentifier> syns = con.getMetadata().getObjectList(null, new String[] {"SYNONYM" });

		assertNotNull(syns);
		assertEquals(1, syns.size());
		TableIdentifier synonym = syns.get(0);
		assertEquals("FOOBAR", synonym.getTableName());

		TableIdentifier table = con.getMetadata().getSynonymTable(synonym);
		assertNotNull(table);
		assertEquals("FOO", table.getTableName());

		String source = synonym.getSource(con).toString();
		String expected =
				"CREATE SYNONYM APP.FOOBAR\n" +
				"   FOR APP.FOO;";
		assertEquals(expected, source.trim());
	}


}
