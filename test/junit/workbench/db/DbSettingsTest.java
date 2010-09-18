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

/**
 * @author Thomas Kellerer
 */
public class DbSettingsTest
{
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

}
