/*
 * DomainIdentifierTest.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class DomainIdentifierTest
	extends TestCase
{

	public DomainIdentifierTest(String testName)
	{
		super(testName);
	}

	public void testDomain()
		throws Exception
	{
		DomainIdentifier domain = new DomainIdentifier("public", "pagila", "year");
		domain.setCheckConstraint("CHECK (VALUE >= 1901 AND VALUE <= 2155)");
		domain.setDataType("integer");
		domain.setDefaultValue(null);
		domain.setNullable(false);
		String source = domain.getSummary();
		assertEquals("integer NOT NULL CHECK (VALUE >= 1901 AND VALUE <= 2155);", source);

		domain.setNullable(true);
		source = domain.getSummary();
		assertEquals("integer CHECK (VALUE >= 1901 AND VALUE <= 2155);", source);

		domain.setDefaultValue("2009");
		domain.setNullable(false);
		source = domain.getSummary();
		assertEquals("integer NOT NULL DEFAULT 2009 CHECK (VALUE >= 1901 AND VALUE <= 2155);", source);
	}

}
