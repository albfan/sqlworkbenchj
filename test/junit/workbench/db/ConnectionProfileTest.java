/*
 * ConnectionProfileTest.java
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
import workbench.sql.DelimiterDefinition;

/**
 * @author support@sql-workbench.net
 */
public class ConnectionProfileTest
	extends TestCase
{
	public ConnectionProfileTest(String testName)
	{
		super(testName);
	}

	public void testCreateCopy()
	{
		try
		{
			ConnectionProfile old = new ConnectionProfile();
			old.setAlternateDelimiter(new DelimiterDefinition("/", true));
			old.setAutocommit(false);
			old.setConfirmUpdates(true);
			old.setDriverName("Postgres");
			old.setEmptyStringIsNull(true);
			old.setUseSeparateConnectionPerTab(true);
			old.setIgnoreDropErrors(true);
			old.setStoreExplorerSchema(true);
			old.setName("First");
			old.setStorePassword(true);
			old.setUrl("jdbc:some:database");

			ConnectionProfile copy = old.createCopy();
			assertFalse(copy.getAutocommit());
			assertTrue(copy.getConfirmUpdates());
			assertEquals("Postgres", copy.getDriverName());
			assertEquals("First", copy.getName());
			assertTrue(copy.getStorePassword());
			assertTrue(copy.getUseSeparateConnectionPerTab());
			assertTrue(copy.getStoreExplorerSchema());
			assertTrue(copy.getIgnoreDropErrors());
			assertEquals("jdbc:some:database", copy.getUrl());
			DelimiterDefinition delim = copy.getAlternateDelimiter();
			assertNotNull(delim);
			assertEquals("/", delim.getDelimiter());
			assertTrue(delim.isSingleLine());
			
			old.setAlternateDelimiter(null);
			copy = old.createCopy();
			assertNull(copy.getAlternateDelimiter());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
