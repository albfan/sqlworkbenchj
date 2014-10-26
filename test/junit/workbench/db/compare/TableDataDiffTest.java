/*
 * TableDataDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.db.compare;

import java.io.StringWriter;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Test;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;

import static org.junit.Assert.*;

import workbench.db.ConnectionMgr;

import workbench.resource.Settings;

import workbench.sql.parser.ScriptParser;

import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class TableDataDiffTest
	extends WbTestCase
{

	private WbConnection source;
	private WbConnection target;
	private TestUtil util;

	public TableDataDiffTest()
	{
		super("TableDataDiffTest");
		util = getTestUtil();
	}

	@AfterClass
	public static void afterClass()
	{
		ConnectionMgr.getInstance().disconnectAll();
	}

	protected void createDefaultDb()
		throws Exception
	{
		Statement sourceStmt = null;
		Statement targetStmt = null;
		try
		{
			source = util.getConnection("data_diff_reference");
			target = util.getConnection("data_diff_target");
			sourceStmt= source.getSqlConnection().createStatement();
			String create = "CREATE table person (" +
				"id1 integer not null, " +
				"id2 integer not null, " +
				"firstname varchar(20), " +
				"lastname varchar(20), primary key(id1, id2))";
			sourceStmt.executeUpdate(create);

			targetStmt = target.getSqlConnection().createStatement();
			targetStmt.executeUpdate(create.replace(" person ", " person_t "));

			int rowCount = 187;

			for (int i=0; i < rowCount; i++)
			{
				sourceStmt.executeUpdate("INSERT INTO person (id1, id2, firstname, lastname) " +
					"VALUES (" + i + ", " + (i + 1) + ", 'first" + i + "', 'last" + i + "')");
			}

			for (int i=0; i < rowCount; i++)
			{
				targetStmt.executeUpdate("INSERT INTO person_t (id1, id2, firstname, lastname) " +
					"VALUES (" + i + ", " + (i + 1) + ", 'first" + i + "', 'last" + i + "')");
			}

			targetStmt.executeUpdate("update person_t set firstname = 'Wrong' where id1 = 4");
			targetStmt.executeUpdate("update person_t set lastname = 'Wrong' where id1 = 8");
			targetStmt.executeUpdate("update person_t set lastname = 'Wrong' where id1 = 21");
			targetStmt.executeUpdate("update person_t set lastname = 'Wrong' where id1 = 69");
			targetStmt.executeUpdate("update person_t set firstname = 'Wrong', lastname = 'Also Wrong' where id1 = 83");
			targetStmt.executeUpdate("delete from person_t where id1 = 6");
			targetStmt.executeUpdate("delete from person_t where id1 = 12");
			targetStmt.executeUpdate("delete from person_t where id1 = 17");
			targetStmt.executeUpdate("delete from person_t where id1 = 53");
			targetStmt.executeUpdate("delete from person_t where id1 = 67");

			source.commit();
			target.commit();
		}
		finally
		{
			SqlUtil.closeStatement(sourceStmt);
			SqlUtil.closeStatement(targetStmt);
		}
	}

	/**
	 * Test of doSync method, of class TableDataDiff.
	 */
	@Test
	public void testDoSync()
		throws Exception
	{
		createDefaultDb();
		TableDataDiff diff = new TableDataDiff(source, target);
		StringWriter updates = new StringWriter(2500);
		StringWriter inserts = new StringWriter(2500);
		diff.setOutputWriters(updates, inserts, "\n", "UTF-8");
		diff.setTableName(new TableIdentifier("person"), new TableIdentifier("person_t"));
		diff.doSync();
//		System.out.println("Sync script: \n" + inserts.toString() + "\n---\n" + updates.toString() + "------");

		// Apply the generated scripts. After that the sync must not return any differences
		TestUtil.executeScript(target, inserts.toString());
		TestUtil.executeScript(target, updates.toString());
		target.commit();
		updates = new StringWriter(2500);
		inserts = new StringWriter(2500);
		diff.setOutputWriters(updates, inserts, "\n", "UTF-8");
		diff.doSync();
		assertTrue(updates.toString().length() == 0);
		assertTrue(inserts.toString().length() == 0);
	}


	protected void createCaseDifferentDb()
		throws Exception
	{
		Statement sourceStmt = null;
		Statement targetStmt = null;
		try
		{
			source = util.getConnection("data_diff_reference");
			target = util.getConnection("data_diff_target");
			sourceStmt= source.getSqlConnection().createStatement();
			String create = "CREATE table person (" +
				"id1 integer not null, " +
				"id2 integer not null, " +
				"firstname varchar(20), " +
				"lastname varchar(20), primary key(id1, id2))";
			sourceStmt.executeUpdate(create);

			targetStmt = target.getSqlConnection().createStatement();
			create = "CREATE table person (" +
				"id1 integer not null, " +
				"id2 integer not null, " +
				"\"firstName\" varchar(20), " +
				"lastname varchar(20), primary key(id1, id2))";
			targetStmt.executeUpdate(create);

			int rowCount = 187;

			for (int i=0; i < rowCount; i++)
			{
				sourceStmt.executeUpdate("INSERT INTO person (id1, id2, firstname, lastname) " +
					"VALUES (" + i + ", " + (i + 1) + ", 'first" + i + "', 'last" + i + "')");
			}

			for (int i=0; i < rowCount; i++)
			{
				targetStmt.executeUpdate("INSERT INTO person (id1, id2, \"firstName\", lastname) " +
					"VALUES (" + i + ", " + (i + 1) + ", 'first" + i + "', 'last" + i + "')");
			}

			targetStmt.executeUpdate("update person set \"firstName\" = 'Wrong' where id1 = 4");
			targetStmt.executeUpdate("update person set lastname = 'Wrong' where id1 = 8");
			targetStmt.executeUpdate("update person set lastname = 'Wrong' where id1 = 21");
			targetStmt.executeUpdate("update person set lastname = 'Wrong' where id1 = 69");
			targetStmt.executeUpdate("update person set \"firstName\" = 'Wrong', lastname = 'Also Wrong' where id1 = 83");
			targetStmt.executeUpdate("delete from person where id1 = 6");
			targetStmt.executeUpdate("delete from person where id1 = 12");
			targetStmt.executeUpdate("delete from person where id1 = 17");
			targetStmt.executeUpdate("delete from person where id1 = 53");
			targetStmt.executeUpdate("delete from person where id1 = 67");

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
	public void testDifferentCase()
		throws Exception
	{
		createCaseDifferentDb();
		TableDataDiff diff = new TableDataDiff(source, target);
		StringWriter updates = new StringWriter(2500);
		StringWriter inserts = new StringWriter(2500);
		diff.setOutputWriters(updates, inserts, "\n", "UTF-8");
		diff.setTableName(new TableIdentifier("person"), new TableIdentifier("person"));
		diff.doSync();
//		String output = updates.toString() + "\n" + inserts.toString();
//		System.out.println("----- sync script start \n" + output.toString() + "----- sync script end");
	}

	@Test
	public void testAlternatePK()
		throws Exception
	{
		source = util.getConnection("data_diff_reference");
		target = util.getConnection("data_diff_target");
		String create = "CREATE table person (" +
			"id integer not null, " +
			"firstname varchar(20), " +
			"lastname varchar(20), " +
			"some_data varchar(100), " +
			"primary key(id))";
		TestUtil.executeScript(source, create);
		TestUtil.executeScript(target, create);

		String insert =
			"insert into person (id, firstname, lastname, some_data) " +
			"values (1, 'Arthur', 'Dent', 'foobar');\n " +
			"insert into person (id, firstname, lastname, some_data) " +
			"values (2, 'Zaphod', 'Beeblebrox', 'zoobar');\n " +
			"insert into person (id, firstname, lastname, some_data) " +
			"values (3, 'Ford', 'Prefect', 'fordbar');\n " +
			"insert into person (id, firstname, lastname, some_data) " +
			"values (4, 'Tricia', 'McMillian', 'nothing');\n " +
			"insert into person (id, firstname, lastname, some_data) " +
			"values (42, 'Prostetnic', 'Jeltz', 'something');\n " +
			"commit;\n";
		TestUtil.executeScript(source, insert);
		String insert2 =
			"insert into person (id, firstname, lastname, some_data) " +
			"values (3, 'Arthur', 'Dent', 'foobar');\n " +
			"insert into person (id, firstname, lastname, some_data) " +
			"values (4, 'Zaphod', 'Beeblebrox', 'zoobar');\n " +
			"insert into person (id, firstname, lastname, some_data) " +
			"values (5, 'Ford', 'Prefect', 'fordbar');\n " +
			"insert into person (id, firstname, lastname, some_data) " +
			"values (6, 'Tricia', 'McMillian', 'something');\n " +
			"commit;\n";
		TestUtil.executeScript(target, insert2);

		boolean oldDel = Settings.getInstance().getDoFormatDeletes();
		boolean oldIns = Settings.getInstance().getDoFormatInserts();
		boolean oldUpd = Settings.getInstance().getDoFormatUpdates();

		try
		{
			Settings.getInstance().setDoFormatDeletes(false);
			Settings.getInstance().setDoFormatInserts(false);
			Settings.getInstance().setDoFormatUpdates(false);
			TableDataDiff diff = new TableDataDiff(source, target);
			StringWriter updates = new StringWriter(2500);
			StringWriter inserts = new StringWriter(2500);
			diff.setOutputWriters(updates, inserts, "\n", "UTF-8");
			Map<String, Set<String>> alternatePk = new HashMap<String, Set<String>>();
			alternatePk.put("person", CollectionUtil.caseInsensitiveSet("firstname", "lastname"));
			diff.setAlternateKeys(alternatePk);

			diff.setTableName(new TableIdentifier("person"), new TableIdentifier("person"));
			diff.setExcludeRealPK(false);
			diff.doSync();
	//		System.out.println(updates.toString());
	//		System.out.println(inserts.toString());
			ScriptParser p = new ScriptParser(updates.toString());
			assertEquals(1, p.getSize());
			String sync = p.getCommand(0);
			assertTrue(sync.indexOf("SET SOME_DATA = 'nothing'") > -1);
			assertTrue(sync.indexOf("WHERE FIRSTNAME = 'Tricia' AND LASTNAME = 'McMillian'") > -1);

			p = new ScriptParser(inserts.toString());
			assertEquals(1, p.getSize());
			sync = SqlUtil.makeCleanSql(p.getCommand(0), false, false);
			assertTrue(sync.startsWith("INSERT INTO PERSON"));
			assertTrue(sync.endsWith("(42,'Prostetnic','Jeltz','something')"));

			diff = new TableDataDiff(source, target);
			updates = new StringWriter(2500);
			inserts = new StringWriter(2500);
			diff.setOutputWriters(updates, inserts, "\n", "UTF-8");
			diff.setExcludeRealPK(true);
			diff.setAlternateKeys(alternatePk);
			diff.setTableName(new TableIdentifier("person"), new TableIdentifier("person"));
			diff.doSync();

			p = new ScriptParser(inserts.toString());
			sync = SqlUtil.makeCleanSql(p.getCommand(0), false, false);
			assertTrue(sync.startsWith("INSERT INTO PERSON"));
			assertTrue(sync.endsWith("('Prostetnic','Jeltz','something')"));
		}
		finally
		{
			Settings.getInstance().setDoFormatDeletes(oldDel);
			Settings.getInstance().setDoFormatInserts(oldIns);
			Settings.getInstance().setDoFormatUpdates(oldUpd);
		}

	}
}
