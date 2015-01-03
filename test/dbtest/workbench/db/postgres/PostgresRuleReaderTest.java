/*
 * PostgresRuleReaderTest.java
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
package workbench.db.postgres;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.DbObject;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import static org.junit.Assert.*;
/**
 *
 * @author Thomas Kellerer
 */
public class PostgresRuleReaderTest
	extends WbTestCase
{
	private static final String TEST_ID = "ruletest";

	public PostgresRuleReaderTest()
	{
		super("PostgresRuleReaderTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		TestUtil.executeScript(con,
			"CREATE table person (id integer, firstname varchar(50), lastname varchar(50));\n" +
			"COMMIT;\n" +
			"CREATE RULE \"_INSERT\" AS ON INSERT TO person DO INSTEAD NOTHING;\n " +
			"COMMIT; \n"
		);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase();
	}

	@Test
	public void retrieveRules()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null)
		{
			return;
		}
		List<TableIdentifier> objects = con.getMetadata().getObjectList(TEST_ID, new String[] { "RULE" });
		assertEquals(1, objects.size());
		TableIdentifier tbl = objects.get(0);
		assertEquals("RULE", tbl.getObjectType());
		String sql = tbl.getSource(con).toString();
		assertEquals("CREATE RULE \"_INSERT\" AS\n    ON INSERT TO person DO INSTEAD NOTHING;", sql);
		DbObject rule = con.getMetadata().getObjectDefinition(tbl);
		String drop = rule.getDropStatement(con, true);
		assertEquals("DROP RULE \"_INSERT\" ON person CASCADE", drop);
		drop = rule.getDropStatement(con, false);
		assertEquals("DROP RULE \"_INSERT\" ON person", drop);
	}

}
