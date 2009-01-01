/*
 * MacroDefinitionTest.java
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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;
import junit.framework.TestCase;
import workbench.resource.StoreableKeyStroke;

/**
 *
 * @author support@sql-workbench.net
 */
public class MacroDefinitionTest
	extends TestCase
{

	public MacroDefinitionTest(String testName)
	{
		super(testName);
	}

	public void testCreateCopy()
	{
		MacroDefinition macro = new MacroDefinition("test", "select 42 from dual");
		assertTrue(macro.isVisibleInMenu());
		macro.setVisibleInMenu(false);
		macro.setSortOrder(5);
		StoreableKeyStroke key = new StoreableKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		macro.setShortcut(key);

		MacroDefinition copy = macro.createCopy();
		StoreableKeyStroke key2 = copy.getShortcut();
		assertEquals(key, key2);
		assertFalse(copy.isVisibleInMenu());
		assertFalse(copy.isModified());
		assertEquals(5, copy.getSortOrder());
		assertEquals(macro.getName(), copy.getName());
		assertEquals(macro.getText(), copy.getText());
	}
}
