/*
 * VersionNumberTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class VersionNumberTest extends TestCase
{
	
	public VersionNumberTest(String testName)
	{
		super(testName);
	}
	
	public void testVersion()
	{
		VersionNumber one = new VersionNumber("94");
		assertEquals(one.getMajorVersion(), 94);
		assertEquals(one.getMinorVersion(), -1);
		
		VersionNumber two = new VersionNumber("94.2");
		assertEquals(two.getMajorVersion(), 94);
		assertEquals(two.getMinorVersion(), 2);
		
		assertTrue(two.isNewerThan(one));
		
		VersionNumber na = new VersionNumber(null);
		assertFalse(na.isNewerThan(two));
		
		VersionNumber dev = new VersionNumber("@BUILD_NUMBER@");
		assertEquals(one.isNewerThan(dev), false);
	}
	
}
