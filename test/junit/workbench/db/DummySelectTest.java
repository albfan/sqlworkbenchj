/*
 * DummySelectTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import junit.framework.TestCase;
import workbench.TestUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class DummySelectTest
	extends TestCase
{

	public DummySelectTest(String testName)
	{
		super(testName);
	}

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
//			System.out.println("+++++++++++++++++++\n" + selectSql + "\n**********\n" + expected + "\n-------------------");
			assertEquals(expected, selectSql);
		}
		finally
		{
			con.disconnect();
		}
	}
}
