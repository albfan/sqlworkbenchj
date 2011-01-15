/*
 * WbVersionReaderTest.java
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
public class WbVersionReaderTest
{

	@Test
	public void testAvailableUpdate()
	{
		VersionNumber current = new VersionNumber("96.8");
		VersionNumber dev = new VersionNumber("97.1");
		VersionNumber stable = new VersionNumber("97");

		WbVersionReader reader = new WbVersionReader(dev, stable);
		UpdateVersion upd = reader.getAvailableUpdate(current);
		assertEquals(UpdateVersion.devBuild, upd);
	}
}
