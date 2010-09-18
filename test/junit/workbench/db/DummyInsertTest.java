/*
 * DummyInsertTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import workbench.TestUtil;
import workbench.resource.Settings;
import workbench.util.SqlUtil;
import static org.junit.Assert.*;
import org.junit.Test;

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
			Statement stmt = con.createStatement();
			stmt.executeUpdate("create table person (nr integer, firstname varchar(20), lastname varchar(20))");
			con.commit();
			TableIdentifier person = con.getMetadata().findTable(new TableIdentifier("PERSON"));
			DummyInsert insert = new DummyInsert(person);
			String sql = insert.getSource(con).toString();

			String verb = SqlUtil.getSqlVerb(sql);
			assertEquals("INSERT", verb);
			assertTrue(sql.indexOf("(NR_value, 'FIRSTNAME_value', 'LASTNAME_value')") > -1);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}

	@Test
	public void testSelectedColumns()
		throws Exception
	{
		TestUtil util = new TestUtil("dummyInsertGen1");
		WbConnection con = util.getConnection();

		try
		{
			Statement stmt = con.createStatement();
			stmt.executeUpdate("create table person (nr integer, firstname varchar(20), lastname varchar(20))");
			con.commit();
			TableIdentifier person = con.getMetadata().findTable(new TableIdentifier("PERSON"));
			List<ColumnIdentifier> cols = new ArrayList<ColumnIdentifier>();
			cols.add(new ColumnIdentifier("NR"));

			DummyInsert insert = new DummyInsert(person, cols);
			String sql = insert.getSource(con).toString();
//			System.out.println("*********\n"+sql);
			String le = Settings.getInstance().getInternalEditorLineEnding();
			assertTrue(sql.trim().equals("INSERT INTO PERSON" + le + "(NR)" + le + "VALUES" + le + "(NR_value);"));
		}
		finally
		{
			ConnectionMgr.getInstance().disconnectAll();
		}
	}
}
