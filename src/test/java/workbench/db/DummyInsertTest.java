/*
 * DummyInsertTest.java
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
package workbench.db;

import java.util.ArrayList;
import java.util.List;

import workbench.TestUtil;
import workbench.resource.Settings;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DummyInsertTest
{

	@Test
	public void testGetSource()
		throws Exception
	{
		TestUtil util = new TestUtil("dummyInsertGen1");
		WbConnection con = util.getConnection();

		try
		{
      TestUtil.executeScript(con,
        "create table person (nr integer, firstname varchar(20), lastname varchar(20));\n" +
        "commit;");
			TableIdentifier person = con.getMetadata().findTable(new TableIdentifier("PERSON"));
			DummyInsert insert = new DummyInsert(person);
      insert.setDoFormatSql(false);
      assertEquals("INSERT", insert.getObjectType());

			String sql = insert.getSource(con).toString();

      String expected = "INSERT INTO PERSON\n  (NR, FIRSTNAME, LASTNAME)\nVALUES\n  (NR_value, 'FIRSTNAME_value', 'LASTNAME_value');";
//      System.out.println(sql + "\n----\n"+ expected);
			assertEquals(expected, sql);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testValueTemplate()
		throws Exception
	{
		TestUtil util = new TestUtil("dummyInsertGen2");
		WbConnection con = util.getConnection();
    assertNotNull(con);

    DummyInsert insert = null;
		try
		{
      TestUtil.executeScript(con,
        "create table person (nr integer, firstname varchar(20), lastname varchar(20));\n" +
        "commit;");
			TableIdentifier person = con.getMetadata().findTable(new TableIdentifier("PERSON"));

			insert = new DummyInsert(person);
      insert.setDoFormatSql(false);

      Settings.getInstance().setProperty(insert.getTemplateConfigKey(), "@" + DummyDML.PLACEHOLDER_COL_NAME);
      Settings.getInstance().setProperty(DummyDML.PROP_CONFIG_GENERATE_LITERAL, false);

			String sql = insert.getSource(con).toString();
      String expected = "INSERT INTO PERSON\n  (NR, FIRSTNAME, LASTNAME)\nVALUES\n  (@NR, @FIRSTNAME, @LASTNAME);";
      System.out.println(sql + "\n---\n" + expected);
			assertEquals(expected, sql);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
      Settings.getInstance().setProperty(insert.getTemplateConfigKey(), null);
      Settings.getInstance().setProperty(DummyDML.PROP_CONFIG_GENERATE_LITERAL, null);
		}
	}

	@Test
	public void testSelectedColumns()
		throws Exception
	{
		TestUtil util = new TestUtil("dummyInsertGen3");
		WbConnection con = util.getConnection();
    assertNotNull(con);

		try
		{
      TestUtil.executeScript(con,
        "create table person (nr integer, firstname varchar(20), lastname varchar(20));\n" +
        "commit;");
			TableIdentifier person = con.getMetadata().findTable(new TableIdentifier("PERSON"));
			List<ColumnIdentifier> cols = new ArrayList<>();
			cols.add(new ColumnIdentifier("NR"));

			DummyInsert insert = new DummyInsert(person, cols);
      insert.setDoFormatSql(false);
			String sql = insert.getSource(con).toString();
      String expected = "INSERT INTO PERSON\n  (NR)\nVALUES\n  (NR_value);";
//      System.out.println(sql + "\n---\n" + expected);
			assertEquals(expected, sql);
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
