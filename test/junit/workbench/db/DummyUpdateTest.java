/*
 * DummyUpdateTest.java
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


import workbench.TestUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DummyUpdateTest
{

	@Test
	public void testGetSource()
		throws Exception
	{
		TestUtil util = new TestUtil("DummyUpdateGen1");
		WbConnection con = util.getConnection();

		try
		{
      TestUtil.executeScript(con,
        "create table person (nr integer not null primary key, firstname varchar(20), lastname varchar(20));\n" +
        "commit;");
			TableIdentifier person = con.getMetadata().findTable(new TableIdentifier("PERSON"));
			DummyUpdate update = new DummyUpdate(person);
      update.setDoFormatSql(false);

			assertEquals("UPDATE", update.getObjectType());
			String sql = update.getSource(con).toString();

      String expected =
        "UPDATE PERSON\n" +
        "   SET FIRSTNAME = 'FIRSTNAME_value',\n" +
        "       LASTNAME = 'LASTNAME_value'\n" +
        "WHERE NR = NR_value;";
//      System.out.println("Got: \n" + sql + "\n------Expected\n" + expected);
      assertEquals(expected, sql);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testMultiColumnPK()
		throws Exception
	{
		TestUtil util = new TestUtil("DummyUpdateGen1");
		WbConnection con = util.getConnection();

		try
		{
			TestUtil.executeScript(con,
        "create table link_table (id1 integer not null, id2 integer not null, some_data varchar(20), primary key (id1, id2));\n" +
        "commit;");

			TableIdentifier person = con.getMetadata().findTable(new TableIdentifier("LINK_TABLE"));
			DummyUpdate update = new DummyUpdate(person);
      update.setDoFormatSql(false);
			assertEquals("UPDATE", update.getObjectType());
			String sql = update.getSource(con).toString();

      String expected =
        "UPDATE LINK_TABLE\n" +
        "   SET SOME_DATA = 'SOME_DATA_value'\n" +
        "WHERE ID1 = ID1_value\n" +
        "  AND ID2 = ID2_value;";
//      System.out.println("Got: \n" + sql + "\n------Expected\n" + expected);
      assertEquals(expected, sql);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}


}
