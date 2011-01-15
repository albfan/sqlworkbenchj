/*
 * AliasTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class AliasTest
{

	@Test
	public void testGetAlias()
	{
		Alias alias = new Alias("f123 as first_name");
		assertEquals("first_name", alias.getAlias());
		assertEquals("f123", alias.getObjectName());
		assertEquals("first_name", alias.getNameToUse());

		alias = new Alias("f123");
		assertNull(alias.getAlias());
		assertEquals("f123", alias.getNameToUse());
		assertEquals("f123", alias.getObjectName());

		alias = new Alias("some_schema.my_table as bla");
		assertEquals("bla", alias.getAlias());
		assertEquals("some_schema.my_table", alias.getObjectName());

		alias = new Alias("\"Imbecile Schema Name\".\"Daft table name\"");
		assertEquals("\"Imbecile Schema Name\".\"Daft table name\"", alias.getObjectName());
		assertNull(alias.getAlias());
	}
}
