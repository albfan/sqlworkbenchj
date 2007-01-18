/*
 * ProfileKeyTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
		ProfileKey key = new ProfileKey("{Group }/ ProfileName ");
		assertEquals("Wrong group detected", "Group", key.getGroup());
		assertEquals("Wrong group detected", "ProfileName", key.getName());
	}
	
	
}
