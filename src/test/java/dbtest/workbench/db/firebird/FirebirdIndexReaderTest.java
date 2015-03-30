/*
 * FirebirdIndexReaderTest.java
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
package workbench.db.firebird;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.IndexDefinition;
import workbench.db.IndexReader;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class FirebirdIndexReaderTest
	extends WbTestCase
{

	public FirebirdIndexReaderTest()
	{
		super("FirebirdIndexReaderTest");
	}

	@Test
	public void testGetExpression()
		throws Exception
	{
		WbConnection conn = FirebirdTestUtil.getFirebirdConnection();
		assertNotNull("No connection available", conn);

		try
		{
			conn.setAutoCommit(true);
			String sql =
				"create table person (id integer, first_name varchar(50), last_name varchar(50));\n" +
				"create index idx_upper_name on person computed by (upper(last_name));\n";
			TestUtil.executeScript(conn, sql);

			IndexReader reader = conn.getMetadata().getIndexReader();
			assertTrue(reader instanceof FirebirdIndexReader);
			List<IndexDefinition> indexList = reader.getTableIndexList(new TableIdentifier("PERSON"), false);
			assertEquals(1, indexList.size());
			IndexDefinition index = indexList.get(0);
			assertNotNull(index);
			assertEquals("COMPUTED BY (upper(last_name))", index.getExpression());
			String create = index.getSource(conn).toString().trim();
//			System.out.println("********\n" + create);
			String expected =
				"CREATE INDEX IDX_UPPER_NAME\n" +
				"   ON PERSON COMPUTED BY (upper(last_name));";
			assertEquals(expected, create);
		}
		finally
		{
			TestUtil.executeScript(conn, "drop table person;");
		}
	}
}
