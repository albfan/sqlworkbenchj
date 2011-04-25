/*
 * VersionNumberTest.java
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
public class VersionNumberTest
{

	@Test
	public void testPatchLevel()
	{
		VersionNumber one = new VersionNumber("2.1");
		VersionNumber two = new VersionNumber("2.1.1");
		assertTrue(two.isNewerThan(one));
		assertFalse(one.isNewerThan(two));

		one = new VersionNumber("2.1.1");
		two = new VersionNumber("2.1.4");
		assertTrue(two.isNewerThan(one));
		assertFalse(one.isNewerThan(two));

		one = new VersionNumber("2.1.0");
		two = new VersionNumber("2.1.1");
		assertTrue(two.isNewerThan(one));
		assertFalse(one.isNewerThan(two));
	}

	@Test
	public void testVersion()
	{
		VersionNumber one = new VersionNumber("94");
		assertEquals(94, one.getMajorVersion());
		assertEquals(-1, one.getMinorVersion());

		VersionNumber two = new VersionNumber("94.2");
		assertEquals(94, two.getMajorVersion());
		assertEquals(2, two.getMinorVersion());

		assertTrue(two.isNewerThan(one));
		assertFalse(one.isNewerThan(two));

		VersionNumber na = new VersionNumber(null);
		assertFalse(na.isNewerThan(two));
		assertTrue(two.isNewerThan(na));

		VersionNumber dev = new VersionNumber("@BUILD_NUMBER@");
		assertFalse(one.isNewerThan(dev));
		assertTrue(dev.isNewerThan(one));

		assertTrue(dev.isNewerThan(two));
		assertFalse(two.isNewerThan(dev));

		VersionNumber current = new VersionNumber("96.8");
		VersionNumber stable = new VersionNumber("97");
		assertTrue(stable.isNewerThan(current));
		assertFalse(current.isNewerThan(stable));

		VersionNumber v2 = new VersionNumber("96.9");
		assertTrue(v2.isNewerOrEqual(current));
		VersionNumber v3 = new VersionNumber("96.8");
		assertTrue(v3.isNewerOrEqual(current));
	}

	@Test
	public void testPoiVersion()
	{
		VersionNumber strange = new VersionNumber("3.5-FINAL-20090928");
		assertEquals(3, strange.getMajorVersion());
		assertEquals(5, strange.getMinorVersion());
	}

}
