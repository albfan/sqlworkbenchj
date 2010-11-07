/*
 * TestColumnDropperPostgres
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.WbTestCase;
import workbench.db.ColumnDropper;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.sql.ScriptParser;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnDropperPostgresTest
	extends WbTestCase
{
	private static final String TEST_ID = "columndropperpg";

	public ColumnDropperPostgresTest()
	{
		super(TEST_ID);
	}

	@BeforeClass
	public static void setUpClass()
		throws Exception
	{
		PostgresTestUtil.initTestCase(TEST_ID);
	}

	@AfterClass
	public static void tearDownClass()
		throws Exception
	{
		PostgresTestUtil.cleanUpTestCase(TEST_ID);
	}

	@Test
	public void testDropObjects()
		throws Exception
	{
		WbConnection con = PostgresTestUtil.getPostgresConnection();
		if (con == null) return;

		Statement stmt = con.createStatement();
		stmt.executeUpdate("create table person (nr integer, firstname varchar(20), lastname varchar(20), dummy1 integer, dummy2 date)");
		con.commit();
		TableIdentifier table = con.getMetadata().findTable(new TableIdentifier("person"));
		List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
		cols.add(new ColumnIdentifier("dummy1"));
		cols.add(new ColumnIdentifier("dummy2"));

		ColumnDropper dropper = new ColumnDropper(con, table, cols);
		String sql = dropper.getScript().toString();

		assertNotNull(sql);
		System.out.println(sql);
		ScriptParser p = new ScriptParser(sql.trim());
		p.setReturnStartingWhitespace(false);
		assertEquals(4, p.getSize());

		assertEquals("ALTER TABLE person DROP COLUMN dummy1 CASCADE", p.getCommand(0).trim());
		assertEquals("COMMIT", p.getCommand(1).trim());
		assertEquals("ALTER TABLE person DROP COLUMN dummy2 CASCADE", p.getCommand(2).trim());
		assertEquals("COMMIT", p.getCommand(3).trim());

		dropper.dropObjects();

		List<ColumnIdentifier> tableCols = con.getMetadata().getTableColumns(table);
		assertEquals(3, tableCols.size());
		assertEquals("nr", tableCols.get(0).getColumnName());
		assertEquals("firstname", tableCols.get(1).getColumnName());
		assertEquals("lastname", tableCols.get(2).getColumnName());
	}
}
