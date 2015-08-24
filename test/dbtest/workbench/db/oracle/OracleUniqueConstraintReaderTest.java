/*
 * OracleUniqueConstraintReaderTest.java
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
package workbench.db.oracle;

import java.util.List;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.IndexDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

import workbench.db.ReaderFactory;
import workbench.db.UniqueConstraintReader;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleUniqueConstraintReaderTest
	extends WbTestCase
{

	public OracleUniqueConstraintReaderTest()
	{
		super("OracleUniqueConstraintReaderTest");
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection conn = OracleTestUtil.getOracleConnection();
		if (conn == null) return;

		String sql =
			"CREATE TABLE parent \n" +
			"( \n" +
			"   id          integer    NOT NULL PRIMARY KEY, \n" +
			"   unique_id1  integer, \n" +
			"   unique_id2  integer \n" +
			"); \n" +
			"ALTER TABLE parent \n" +
			"   ADD CONSTRAINT uk_id1_id2 UNIQUE (unique_id1, unique_id2); \n" +
			" \n" +
			" \n" +
			"COMMIT;";
		TestUtil.executeScript(conn, sql);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testProcessIndexList()
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		assertNotNull("Oracle not available", con);

		TableIdentifier parent = con.getMetadata().findObject(new TableIdentifier("PARENT"));
		List<IndexDefinition> indexList = con.getMetadata().getIndexReader().getTableIndexList(parent);

    UniqueConstraintReader uniqueReader = ReaderFactory.getUniqueConstraintReader(con);
    uniqueReader.readUniqueConstraints(indexList, con);

		boolean foundConstraint = false;
		for (IndexDefinition idx : indexList)
		{
			if (idx.getName().equals("UK_ID1_ID2"))
			{
				assertTrue(idx.isUniqueConstraint());
				assertEquals("UK_ID1_ID2", idx.getUniqueConstraintName());
				foundConstraint = true;
			}
		}
		assertTrue(foundConstraint);
	}
}
