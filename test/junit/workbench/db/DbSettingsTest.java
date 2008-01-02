/*
 * DbSettingsTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import junit.framework.TestCase;

/**
 * @author support@sql-workbench.net
 */
public class DbSettingsTest
	extends TestCase
{
	public DbSettingsTest(String testName)
	{
		super(testName);
	}

	public void testOraDefaults()
	{
		DbSettings pg = new DbSettings("postgresql", "Postgres Test");
		assertFalse(pg.getConvertDateInExport());
	}

	public void testPgDefaults()
	{
		DbSettings pg = new DbSettings("postgresql", "Postgres Test");
		assertFalse(pg.getConvertDateInExport());
	}

}
