/*
 * PostgresDataTypeResolverTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.Types;
import junit.framework.TestCase;

/**
 * @author support@sql-workbench.net
 */
public class PostgresDataTypeResolverTest
	extends TestCase
{
	public PostgresDataTypeResolverTest(String testName)
	{
		super(testName);
	}

	public void testGetSqlTypeDisplay()
	{
		PostgresDataTypeResolver resolver = new PostgresDataTypeResolver();
		
		String display = resolver.getSqlTypeDisplay("NUMERIC", Types.NUMERIC, 65535, 0, 0);
		assertEquals("NUMERIC", display);
		
		display = resolver.getSqlTypeDisplay("NUMERIC", Types.NUMERIC, 131089, 0, 0);
		assertEquals("NUMERIC", display);

		display = resolver.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 300, 0, 0);
		assertEquals("VARCHAR(300)", display);
	}
	
}
