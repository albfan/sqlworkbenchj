/*
 * FilteredPropertiesTest.java
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

import workbench.interfaces.PropertyStorage;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class FilteredPropertiesTest
{

	@Test
	public void testCopyTo()
	{
		PropertyStorage old = new WbProperties();
		old.setProperty("dbexplorer1.prop1", "first");
		old.setProperty("dbexplorer1.prop2", "second");
		old.setProperty("panel2.prop1", "third");

		FilteredProperties instance = new FilteredProperties(old, "dbexplorer1");
		PropertyStorage target = new WbProperties();

		instance.copyTo(target, "dbexplorer5");
		assertEquals("first", target.getProperty("dbexplorer5.prop1", ""));
		assertEquals("second", target.getProperty("dbexplorer5.prop2", ""));

		assertEquals("xxxx", target.getProperty("dbexplorer1.prop1", "xxxx"));
		assertEquals("xxxx", target.getProperty("panel2.prop1", "xxxx"));

	}
}
