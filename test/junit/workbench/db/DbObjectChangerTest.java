/*
 * DbObjectChangerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.sql.ScriptParser;
import workbench.util.CollectionUtil;

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
		DbSettings settings = new DbSettings("postgresql", "PostgreSQL");
		DbObjectChanger changer = new DbObjectChanger(settings);
		TableIdentifier table = new TableIdentifier("person_address");

		ColumnIdentifier pid = new ColumnIdentifier("person_id", Types.INTEGER);
		ColumnIdentifier aid = new ColumnIdentifier("address_id", Types.INTEGER);

		List<ColumnIdentifier> cols = CollectionUtil.arrayList(pid, aid);

		String sql = changer.getAddPK(table, cols);
		assertNotNull(sql);
		assertTrue(sql.contains("PRIMARY KEY (person_id, address_id)"));

		table.setPrimaryKeyName("pk_person_address");
		sql = changer.getDropPK(table);
		assertEquals("ALTER TABLE person_address DROP CONSTRAINT pk_person_address CASCADE", sql);

		TableIdentifier renamedTable = new TableIdentifier("pers_addr");
		sql = changer.getRename(table, renamedTable);
		assertEquals("ALTER TABLE person_address RENAME TO pers_addr", sql);

		TableIdentifier newTable = table.createCopy();
		newTable.setComment("new comment");
		sql = changer.getCommentSql(table, newTable);
		assertEquals("COMMENT ON TABLE person_address IS 'new comment'", sql);

		Map<DbObject, DbObject> changed = new HashMap<DbObject, DbObject>();
		newTable = new TableIdentifier("pers_addr");
		newTable.setComment("new comment");
		changed.put(table, newTable);
		sql = changer.getAlterScript(changed);
		ScriptParser p = new ScriptParser(sql);
		assertEquals(3, p.getSize());
		assertEquals("COMMENT ON TABLE person_address IS 'new comment'", p.getCommand(0));
		assertEquals("ALTER TABLE person_address RENAME TO pers_addr", p.getCommand(1));
		assertEquals("COMMIT", p.getCommand(2));
	}
	
}
