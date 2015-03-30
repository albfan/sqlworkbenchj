/*
 * TableDeleteSyncTest.java
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
package workbench.db.compare;

import java.io.StringWriter;
import java.sql.Statement;
import java.util.Set;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.sql.parser.ScriptParser;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TableDeleteSyncTest
	extends WbTestCase
{
	private WbConnection source;
	private WbConnection target;
	private int rowCount = 127;
	private int toDelete = 53;
	private TestUtil util;

	public TableDeleteSyncTest()
	{
		super("syncDelete");
		util = getTestUtil();
	}

	public void createTables()
		throws Exception
	{
		Statement sourceStmt = null;
		Statement targetStmt = null;

		try
		{
			source = util.getConnection("sync_delete_reference");
			target = util.getConnection("sync_delete_target");

			sourceStmt= source.getSqlConnection().createStatement();
			String create = "CREATE table person (" +
				"id1 integer not null, " +
				"id2 integer not null, " +
				"firstname varchar(20), " +
				"lastname varchar(20), primary key(id1, id2))";
			sourceStmt.executeUpdate(create);

			targetStmt = target.getSqlConnection().createStatement();
			targetStmt.executeUpdate(create.replace(" person ", " person_t "));

			for (int i=0; i < rowCount; i++)
			{
				sourceStmt.executeUpdate(
          "INSERT INTO person (id1, id2, firstname, lastname) " +
					"VALUES (" + i + ", " + (i + 1) + ", 'first" + i + "', 'last" + i + "')");
			}

			for (int i=0; i < rowCount; i++)
			{
				targetStmt.executeUpdate(
          "INSERT INTO person_t (id1, id2, firstname, lastname) " +
					"VALUES (" + i + ", " + (i + 1) + ", 'first" + i + "', 'last" + i + "')");
			}

			for (int i=10000; i < 10000 + toDelete; i++)
			{
				targetStmt.executeUpdate(
          "INSERT INTO person_t (id1, id2, firstname, lastname) " +
					"VALUES (" + i + ", " + (i + 1) + ", 'first" + i + "', 'last" + i + "')");
			}
			source.commit();
			target.commit();
		}
		finally
		{
			SqlUtil.closeStatement(sourceStmt);
			SqlUtil.closeStatement(targetStmt);
		}
	}

	@Test
	public void testDeleteTarget()
		throws Exception
	{
    createTables();

    TableDeleteSync sync = new TableDeleteSync(target, source);
    sync.setTableName(new TableIdentifier("person"), new TableIdentifier("person_t"));
    sync.doSync();
    long deleted = sync.getDeletedRows();
    assertEquals(toDelete, deleted);

    int count = TestUtil.getNumberValue(target, "select count(*) from person_t");
    assertEquals("Wrong row count in target table", rowCount, count);
	}

	@Test
	public void testCreateScript()
		throws Exception
	{
    createTables();
    TableDeleteSync sync = new TableDeleteSync(target, source);
    StringWriter writer = new StringWriter();
    sync.setOutputWriter(writer, "\n", "UTF-8");
    sync.setTableName(new TableIdentifier("person"), new TableIdentifier("person_t"));
    sync.doSync();

    String sql = writer.toString();
    ScriptParser parser = new ScriptParser(sql);
    int count = parser.getSize();
    assertEquals("Wrong DELETE count", toDelete, count);

    writer = new StringWriter();
    sync.setOutputWriter(writer, "\n", "UTF-8");
    Set<String> pk = CollectionUtil.caseInsensitiveSet("firstname", "lastname");
    sync.setTableName(new TableIdentifier("person"), new TableIdentifier("person_t"), pk);
    sync.doSync();
    sql = writer.toString();
    assertFalse(sql.indexOf("WHERE ID1 =") > -1);
    assertFalse(sql.indexOf("AND ID2 =") > -1);
    assertTrue(sql.indexOf("WHERE FIRSTNAME =") > -1);
    assertTrue(sql.indexOf("AND LASTNAME =") > -1);
	}

  @Test
  public void testPKOnly()
    throws Exception
  {
    WbConnection sourceConn = util.getConnection("sync_delete_reference");
		WbConnection targetConn = util.getConnection("sync_delete_target");

    TestUtil.executeScript(sourceConn,
      "create table link_table (pid integer not null, sid integer not null, primary key (pid, sid));\n" +
      "insert into link_table values (1,1);\n" +
      "insert into link_table values (2,2);\n" +
      "insert into link_table values (3,3);\n" +
      "insert into link_table values (4,4);\n" +
      "commit;\n");

    TestUtil.executeScript(targetConn,
      "create table lnk_table (pid integer not null, sid integer not null, primary key (pid, sid));\n" +
      "insert into lnk_table values (1,1);\n" +
      "insert into lnk_table values (2,2);\n" +
      "insert into lnk_table values (3,3);\n" +
      "insert into lnk_table values (4,4);\n" +
      "insert into lnk_table values (5,5);\n" +
      "commit;\n");

    TableDeleteSync sync = new TableDeleteSync(targetConn, sourceConn);
    sync.setTableName(new TableIdentifier("link_table"), new TableIdentifier("lnk_table"));
    sync.doSync();

    int count = TestUtil.getNumberValue(targetConn, "select count(*) from lnk_table where pid = 5");
    assertEquals(0, count);
    count = TestUtil.getNumberValue(targetConn, "select count(*) from lnk_table;");
    assertEquals(4, count);
  }

}
