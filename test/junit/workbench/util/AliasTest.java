/*
 * AliasTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import junit.framework.TestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class AliasTest
	extends TestCase
{

	public AliasTest(String testName)
	{
		super(testName);
	}

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
	}

}
