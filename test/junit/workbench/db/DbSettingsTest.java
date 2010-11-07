/*
 * DbSettingsTest.java
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

import static org.junit.Assert.*;
import org.junit.Test;
import workbench.WbTestCase;

/**
 * @author Thomas Kellerer
 */
public class DbSettingsTest
	extends WbTestCase
{

	public DbSettingsTest()
	{
		super("DbSettingsTest");
	}
	
	@Test
	public void testOraDefaults()
	{
		DbSettings pg = new DbSettings("postgresql", "Postgres Test");
		assertFalse(pg.getConvertDateInExport());
	}

	@Test
	public void testPgDefaults()
	{
		DbSettings pg = new DbSettings("postgresql", "Postgres Test");
		assertFalse(pg.getConvertDateInExport());
	}

	@Test
	public void testGetIdentifierCase()
	{
		DbSettings test = new DbSettings("dummy", "Dummy Test");

		IdentifierCase idCase = test.getObjectNameCase();
		assertEquals(IdentifierCase.unknown, idCase);

		test.setObjectNameCase("mixed");
		idCase = test.getObjectNameCase();
		assertEquals(IdentifierCase.mixed, idCase);

		test.setObjectNameCase("gaga");
		idCase = test.getObjectNameCase();
		assertEquals(IdentifierCase.unknown, idCase);

		test.setObjectNameCase("lower");
		idCase = test.getObjectNameCase();
		assertEquals(IdentifierCase.lower, idCase);
	}
}
