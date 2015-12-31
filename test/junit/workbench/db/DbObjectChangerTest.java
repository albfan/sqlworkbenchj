/*
 * DbObjectChangerTest.java
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

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.sql.parser.ScriptParser;

import workbench.util.CollectionUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectChangerTest
	extends WbTestCase
{

	public DbObjectChangerTest()
	{
		super("DbObjectChangerTest");
	}

	@Test
	public void testGetDropPK_H2()
		throws Exception
	{
		try
		{
			TestUtil util = getTestUtil();
			WbConnection con = util.getConnection();
			String sql = "create table person (nr integer primary key, firstname varchar(100));\n" +
				"commit;\n";
			TestUtil.executeScript(con, sql);
			TableIdentifier table = new TableIdentifier("PERSON");
			table.setType("TABLE");
			DbObjectChanger changer = new DbObjectChanger(con);
			String drop = changer.getDropPK(table);
			TestUtil.executeScript(con, drop);
			TableDefinition tbl = con.getMetadata().getTableDefinition(table);
			assertNull(tbl.getTable().getPrimaryKeyName());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testPostgreSQL()
		throws Exception
	{
		DbSettings settings = new DbSettings("postgresql");
		DbObjectChanger changer = new DbObjectChanger(settings);
		TableIdentifier table = new TableIdentifier("person_address");

		ColumnIdentifier pid = new ColumnIdentifier("person_id", Types.INTEGER);
		ColumnIdentifier aid = new ColumnIdentifier("address_id", Types.INTEGER);

		List<ColumnIdentifier> cols = CollectionUtil.arrayList(pid, aid);

		String sql = changer.getAddPK(table, cols);
		assertNotNull(sql);
		assertTrue(sql.contains("PRIMARY KEY (person_id, address_id)"));

		PkDefinition pk = new PkDefinition("pk_person_address", CollectionUtil.arrayList(new IndexColumn("person_id", 1)));
		table.setPrimaryKey(pk);
		sql = changer.getDropPK(table);
    System.out.println(sql);
		assertEquals("ALTER TABLE person_address DROP CONSTRAINT pk_person_address CASCADE", sql);

		TableIdentifier renamedTable = new TableIdentifier("pers_addr");
		sql = changer.getRename(table, renamedTable);
		assertEquals("ALTER TABLE person_address RENAME TO pers_addr", sql);

		TableIdentifier newTable = table.createCopy();
		newTable.setComment("new comment");
		sql = changer.getCommentSql(table, newTable);
		assertEquals("COMMENT ON TABLE person_address IS 'new comment'", sql);

		Map<DbObject, DbObject> changed = new HashMap<>();
		newTable = new TableIdentifier("pers_addr");
		newTable.setComment("new comment");
		changed.put(table, newTable);
		sql = changer.getAlterScript(changed);
		ScriptParser p = new ScriptParser(sql);
		assertEquals(3, p.getSize());
		assertEquals("COMMENT ON TABLE person_address IS 'new comment'", p.getCommand(0));
		assertEquals("ALTER TABLE person_address RENAME TO pers_addr", p.getCommand(1));
		assertEquals("COMMIT", p.getCommand(2));

		table = new TableIdentifier("public", "bar");
		newTable = new TableIdentifier("foo", "bar");
		sql = changer.getSchemaChange(table, newTable);
		assertNotNull(sql);
		assertEquals("ALTER TABLE public.bar SET SCHEMA foo", sql);

		changed.clear();
		changed.put(table, newTable);
		sql = changer.getAlterScript(changed);
		p = new ScriptParser(sql);
		assertEquals(2, p.getSize());
		assertEquals("ALTER TABLE public.bar SET SCHEMA foo", p.getCommand(0));
	}

	@Test
	public void testSQLServer()
		throws Exception
	{
		DbSettings settings = new DbSettings("microsoft_sql_server");
		DbObjectChanger changer = new DbObjectChanger(settings);

		TableIdentifier oldTable = new TableIdentifier("dbo", "bar");
		TableIdentifier newTable = new TableIdentifier("foo", "bar");
		String sql = changer.getSchemaChange(oldTable, newTable);
		assertNotNull(sql);
		assertEquals("ALTER SCHEMA foo TRANSFER dbo.bar", sql);
	}

	@Test
	public void testMySQL()
		throws Exception
	{
		DbSettings settings = new DbSettings("mysql");
		DbObjectChanger changer = new DbObjectChanger(settings);

		TableIdentifier oldTable = new TableIdentifier("first_db", null, "bar");
		TableIdentifier newTable = new TableIdentifier("second_db", null, "bar");
		String sql = changer.getCatalogChange(oldTable, newTable);
		assertEquals("RENAME TABLE first_db.bar TO second_db.bar", sql);
	}

}
