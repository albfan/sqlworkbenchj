/*
 * FirebirdViewGrantReaderTest.java
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
package workbench.db.firebird;

import java.util.Collection;
import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableGrant;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class FirebirdViewGrantReaderTest
	extends WbTestCase
{

	public FirebirdViewGrantReaderTest()
	{
		super("FirebirdViewGrantReaderTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		FirebirdTestUtil.initTestCase();
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"create table foo (id integer);\n" +
			"create view v_foo as select * from foo;\n" +
			"grant select on v_foo to public;");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		FirebirdTestUtil.cleanUpTestCase();
	}


	@Test
	public void testUpdateColumnDefinition()
		throws Exception
	{
		WbConnection con = FirebirdTestUtil.getFirebirdConnection();
		assertNotNull("No connection available", con);
		
		List<TableIdentifier> tables = con.getMetadata().getTableList();
		assertNotNull(tables);
		assertEquals(1, tables.size());

		List<TableIdentifier> views = con.getMetadata().getObjectList(null, new String[] { "VIEW"});
		assertNotNull(views);
		assertEquals(1, views.size());

		FirebirdViewGrantReader reader = new FirebirdViewGrantReader();
		Collection<TableGrant> grants = reader.getViewGrants(con, views.get(0));
		assertNotNull(grants);
		assertEquals(1, grants.size());
		TableGrant grant = grants.iterator().next();
		assertEquals("SELECT", grant.getPrivilege());
		assertEquals("PUBLIC", grant.getGrantee());
	}

}
