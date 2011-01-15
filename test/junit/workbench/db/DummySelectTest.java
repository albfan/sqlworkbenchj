/*
 * DummySelectTest.java
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

import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.resource.Settings;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

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
