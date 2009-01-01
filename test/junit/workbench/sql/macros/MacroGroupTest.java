/*
 * MacroGroupTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.macros;

import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class MacroGroupTest
	extends TestCase
{

	public MacroGroupTest(String testName)
	{
		super(testName);
	}

	public void testCreateCopy()
	{
		MacroGroup group = new MacroGroup("Default Group");
		group.setSortOrder(2);
		group.setVisibleInMenu(false);

		group.addMacro(new MacroDefinition("one", "test one"));

		MacroGroup copy = group.createCopy();
		assertFalse(copy.isModified());
		assertFalse(copy.isVisibleInMenu());
		assertEquals(2, copy.getSortOrder());
		assertEquals(1, copy.getSize());
	}
}
