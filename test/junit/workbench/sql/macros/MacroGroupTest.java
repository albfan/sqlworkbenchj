/*
 * MacroGroupTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.macros;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class MacroGroupTest
{

	@Test
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
