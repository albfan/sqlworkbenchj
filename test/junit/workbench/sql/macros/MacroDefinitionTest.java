/*
 * MacroDefinitionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.macros;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.resource.StoreableKeyStroke;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class MacroDefinitionTest
{

	@Test
	public void testCreateCopy()
	{
		MacroDefinition macro = new MacroDefinition("test", "select 42 from dual");
		assertTrue(macro.isVisibleInMenu());
		macro.setVisibleInMenu(false);
		macro.setExpandWhileTyping(true);
		macro.setSortOrder(5);
		macro.setAppendResult(true);
		StoreableKeyStroke key = new StoreableKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		macro.setShortcut(key);

		MacroDefinition copy = macro.createCopy();
		assertTrue(copy.getExpandWhileTyping());
		StoreableKeyStroke key2 = copy.getShortcut();
		assertEquals(key, key2);
		assertTrue(copy.isAppendResult());
		assertFalse(copy.isVisibleInMenu());
		assertFalse(copy.isModified());
		assertEquals(5, copy.getSortOrder());
		assertEquals(macro.getName(), copy.getName());
		assertEquals(macro.getText(), copy.getText());
	}

	@Test
	public void testModified()
	{
		MacroDefinition macro = new MacroDefinition("test", "select 42 from dual");
		assertFalse(macro.isModified());
		macro.setExpandWhileTyping(true);
		assertTrue(macro.isModified());

		macro.resetModified();
		macro.setAppendResult(true);
		assertTrue(macro.isModified());

		macro.resetModified();
		macro.setVisibleInMenu(false);
		assertTrue(macro.isModified());
		macro.setAppendResult(false);
		assertTrue(macro.isModified());
		macro.setExpandWhileTyping(false);
		assertTrue(macro.isModified());
	}
}
