/*
 * Db2ViewGrantReaderTest.java
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
package workbench.db.ibm;


import workbench.TestUtil;

import workbench.db.TableIdentifier;
import workbench.db.ViewReader;
import workbench.db.ViewReaderFactory;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2ViewReaderTest
{

	public Db2ViewReaderTest()
	{
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		Db2TestUtil.initTestCase();

		WbConnection conn = Db2TestUtil.getDb2Connection();
		if (conn == null) return;

		String schema = Db2TestUtil.getSchemaName();

		String sql =
			"CREATE TABLE " + schema + ".person (id integer, firstname varchar(50), lastname varchar(50)); \n" +
      "CREATE VIEW " + schema + ".v_person AS SELECT * FROM wbjunit.person; \n" +
      "comment on table  " + schema + ".v_person is 'a smart description';\n" +
			"commit;\n";
		TestUtil.executeScript(conn, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		Db2TestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetViewGrantSql()
		throws Exception
	{
		WbConnection conn = Db2TestUtil.getDb2Connection();
		if (conn == null) fail("No connection available");

		String schema = Db2TestUtil.getSchemaName();

		TableIdentifier view = new TableIdentifier(schema, "V_PERSON");
		view.setType("VIEW");
		ViewReader reader = ViewReaderFactory.createViewReader(conn);
		CharSequence source = reader.getExtendedViewSource(view);
		assertNotNull(source);
		String sql = source.toString();
		assertTrue(sql.contains("FROM wbjunit.person"));
		assertTrue(sql.contains("'a smart description'"));
	}

}
