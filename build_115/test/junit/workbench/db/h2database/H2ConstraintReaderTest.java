/*
 * H2ConstraintReaderTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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

import workbench.db.ConnectionMgr;
import java.util.List;
import workbench.TestUtil;
import workbench.db.TableConstraint;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import org.junit.AfterClass;
import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import workbench.db.ConstraintReader;
import workbench.db.ReaderFactory;

/**
 *
 * @author Thomas Kellerer
 */
public class H2ConstraintReaderTest
	extends WbTestCase
{

	public H2ConstraintReaderTest()
	{
		super("H2ConstraintReaderTest");
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	@Test
	public void testReader()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection();

		String sql = "CREATE TABLE check_test (id integer, constraint positive_id check (id > 42));";
		TestUtil.executeScript(con, sql);

		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("CHECK_TEST"));
		ConstraintReader reader = ReaderFactory.getConstraintReader(con.getMetadata());
		assertTrue(reader instanceof H2ConstraintReader);
		
		List<TableConstraint> cons = reader.getTableConstraints(con, tbl);
		assertNotNull(cons);
		assertEquals(1, cons.size());
		TableConstraint constraint = cons.get(0);
		assertEquals("POSITIVE_ID", constraint.getConstraintName());
		assertEquals("(ID > 42)", constraint.getExpression());
		assertEquals("check", constraint.getType());
		System.out.println(constraint.getSql());
		assertEquals("CONSTRAINT POSITIVE_ID CHECK (ID > 42)", constraint.getSql());
	}

}
