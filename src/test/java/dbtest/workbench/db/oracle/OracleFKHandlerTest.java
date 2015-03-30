/*
 * OracleFKHandlerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.oracle;

import java.sql.SQLException;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleFKHandlerTest
	extends WbTestCase
{

	public OracleFKHandlerTest()
	{
		super("OracleFKHandlerTest");
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testDeferrable()
		throws SQLException
	{
		WbConnection conn = OracleTestUtil.getOracleConnection();
		assertNotNull("No Oracle connection available", conn);
		OracleFKHandler fkHandler = new OracleFKHandler(conn);
		String create =
			"create table parent (id integer not null primary key);\n" +
			"create table child_deferred (id integer not null primary key, pid integer not null,\n" +
			" constraint fk_aaa foreign key (pid) references parent (id) deferrable initially deferred);\n" +
			"create table child_immediate (id integer not null primary key, pid integer not null,\n" +
			" constraint fk_bbb foreign key (pid) references parent (id) deferrable initially immediate);\n" +
			"create table child_not_deferred (id integer not null primary key, pid integer not null,\n" +
			" constraint fk_ccc foreign key (pid) references parent (id));\n";

		TestUtil.executeScript(conn, create);

		TableIdentifier parent = conn.getMetadata().findTable(new TableIdentifier("PARENt"));

		DataStore fklist = fkHandler.getReferencedBy(parent);
		assertNotNull(fklist);
		assertEquals(3, fklist.getRowCount());

		fklist.sortByColumn(0, true);
//		DataStorePrinter printer = new DataStorePrinter(fklist);
//		printer.printTo(System.out);

		String deferrable = fklist.getValueAsString(0, "DEFERRABLE");
		assertEquals("INITIALLY DEFERRED", deferrable);

		deferrable = fklist.getValueAsString(1, "DEFERRABLE");
		assertEquals("INITIALLY IMMEDIATE", deferrable);

		deferrable = fklist.getValueAsString(2, "DEFERRABLE");
		assertEquals("NOT DEFERRABLE", deferrable);
		OracleTestUtil.cleanUpTestCase();
	}

}
