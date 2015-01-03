/*
 * SelectIntoVerifierTest.java
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
package workbench.db;

import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectIntoVerifierTest
	extends WbTestCase
{
	public SelectIntoVerifierTest()
	{
		super("SelectIntoTesterTest");
	}

	@Test
	public void testIsSelectIntoNewTable()
		throws Exception
	{
		SelectIntoVerifier tester = new SelectIntoVerifier("microsoft_sql_server");

		String sql = "select * into new_table from old_table;";
		assertTrue("Pattern for SQL Server not working", tester.isSelectIntoNewTable(sql));

		sql = "-- Test\n" +
					"select * into #temp2 from #temp1;\n";
		assertTrue("Pattern for SQL Server not working", tester.isSelectIntoNewTable(sql));


		tester = new SelectIntoVerifier("postgresql");
		sql = "select * into new_table from old_table;";
		assertTrue("Pattern for Postgres not working", tester.isSelectIntoNewTable(sql));

		sql = "-- Test\n" +
					"select * into new_table from old_table;\n";
		assertTrue("Pattern for Postgres not working", tester.isSelectIntoNewTable(sql));

		tester = new SelectIntoVerifier("informix-online");
		sql = "select * from old_table into new_table";
		assertTrue(tester.isSelectIntoNewTable(sql));
	}
}
