/*
 * DummySelectTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

import java.util.ArrayList;
import java.util.List;
import workbench.TestUtil;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class DummySelectTest
{

	@Test
	public void testGetSource()
		throws Exception
	{
		TestUtil util = new TestUtil("dummySelect");
		WbConnection con = null;
		try
		{
			util.prepareEnvironment();
			con = util.getConnection();
			String sql = "create table person (nr integer primary key, firstname varchar(50), lastname varchar(50));";
			TestUtil.executeScript(con, sql);
			DummySelect select = new DummySelect(new TableIdentifier("person"));
			String selectSql = select.getSource(con).toString().trim();
			String expected =
			 "SELECT NR,\n"+
       "       FIRSTNAME,\n" +
       "       LASTNAME\n" +
			 "FROM PERSON;";
			assertEquals(expected, selectSql);
		}
		finally
		{
			con.disconnect();
		}
	}

	@Test
	public void testSelectedColumns()
		throws Exception
	{
		TestUtil util = new TestUtil("dummyInsertGen1");
		util.prepareEnvironment();
		WbConnection con = util.getConnection();

		try
		{
			TestUtil.executeScript(con,
				"create table person (nr integer, firstname varchar(20), lastname varchar(20));\n" +
				"commit;");
			TableIdentifier person = con.getMetadata().findTable(new TableIdentifier("PERSON"));
			List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
			cols.add(new ColumnIdentifier("NR"));

			DummySelect select = new DummySelect(person, cols);
			String sql = select.getSource(con).toString();
//			System.out.println("*********\n"+sql);
			assertTrue(sql.trim().equals("SELECT NR\nFROM PERSON;"));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

}
