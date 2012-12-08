/*
 * MySQLShowTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import workbench.WbTestCase;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class MySQLShowTest
	extends WbTestCase
{
	public MySQLShowTest()
	{
		super("MySQLShowTest");
	}

	@Test
	public void testCheckSyntax()
		throws Exception
	{
		MySQLShow show = new MySQLShow();
		assertTrue(show.isInnoDBStatus("show engine innodb status"));
		assertFalse(show.isInnoDBStatus("show engine innodb mutex"));
		assertFalse(show.isInnoDBStatus("show create table foobar"));
	}

}
