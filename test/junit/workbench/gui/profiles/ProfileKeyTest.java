/*
 * ProfileKeyTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

/**
 *
 * @author support@sql-workbench.net
 */
public class ProfileKeyTest extends junit.framework.TestCase
{
	
	public ProfileKeyTest(String testName)
	{
		super(testName);
	}
	
	public void testCreate()
	{
		ProfileKey key = new ProfileKey(" { Group } / ProfileName ");
		assertEquals("Wrong group detected", "Group", key.getGroup());
		assertEquals("Wrong name detected", "ProfileName", key.getName());
		
		key = new ProfileKey("{Group}/ProfileName ");
		assertEquals("Wrong group detected", "Group", key.getGroup());
		assertEquals("Wrong name detected", "ProfileName", key.getName());

		// Allow group definition without a slash
		key = new ProfileKey("{Group}ProfileName ");
		assertEquals("Wrong group detected", "Group", key.getGroup());
		assertEquals("Wrong name detected", "ProfileName", key.getName());
	}
	
	public void testCompare()
	{
		ProfileKey key1 = new ProfileKey("Profile1");
		ProfileKey key2 = new ProfileKey("Profile1", "Default Group");
		assertEquals(key1, key2);

		key1 = new ProfileKey("Profile1");
		key2 = new ProfileKey("Profile1");
		assertEquals(key1, key2);
		
		key1 = new ProfileKey("{DefaultGroup}/Profile1");
		key2 = new ProfileKey("Profile1", "Other Group");
		assertNotSame(key1, key2);
		
		key1 = new ProfileKey("Profile1", "Default Group");
		key2 = new ProfileKey("Profile2", "Default Group");
		assertNotSame(key1, key2);

		key1 = new ProfileKey("Profile1", "Default Group");
		key2 = new ProfileKey("{ Default Group} / Profile1");
		assertEquals(key1, key2);
	}

}
