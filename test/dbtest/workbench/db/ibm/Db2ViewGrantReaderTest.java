/*
 * Db2ViewGrantReaderTest.java
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
package workbench.db.ibm;

import java.util.Collection;
import java.util.List;

import workbench.TestUtil;

import workbench.db.TableGrant;
import workbench.db.TableIdentifier;
import workbench.db.ViewGrantReader;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2ViewGrantReaderTest
{

	public Db2ViewGrantReaderTest()
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
      "GRANT SELECT ON " + schema + ".v_person TO PUBLIC;\n" +
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

		List<TableIdentifier> views = conn.getMetadata().getObjectList(schema, new String[] { "VIEW" });
		assertNotNull(views);
		assertEquals(1, views.size());

		ViewGrantReader reader = ViewGrantReader.createViewGrantReader(conn);
		assertTrue(reader instanceof Db2ViewGrantReader);

		Collection<TableGrant> grants = reader.getViewGrants(conn, views.get(0));
		assertNotNull(grants);
		assertEquals(1, grants.size());
		TableGrant grant = grants.iterator().next();
		assertEquals("SELECT", grant.getPrivilege());
		assertEquals("PUBLIC", grant.getGrantee());
	}

}
