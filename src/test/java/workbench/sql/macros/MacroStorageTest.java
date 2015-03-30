/*
 * MacroStorageTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import java.util.HashMap;
import java.util.Map;

import javax.swing.KeyStroke;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.StoreableKeyStroke;

import workbench.util.WbFile;
import workbench.util.WbPersistence;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class MacroStorageTest
	extends WbTestCase
{

	public MacroStorageTest()
	{
		super("MacroStorageTest");
	}

	@Test
	public void testSave()
	{
		MacroStorage macros = new MacroStorage();
		assertFalse(macros.isModified());

		macros.addMacro("Default", "sessions", "select * from v$session");
		macros.addMacro("Default", "WHO", "sp_who2");
		macros.addMacro("Default", "clean", "delete from $[table]");

		MacroDefinition macro = macros.getMacro("sessions");
		StoreableKeyStroke key = new StoreableKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
		macro.setShortcut(key);

		TestUtil util = new TestUtil("SaveMacros");
		WbFile f = new WbFile(util.getBaseDir(), "macros.xml");
		macros.saveMacros(f);
		MacroStorage newStorage = new MacroStorage(f);
		MacroDefinition m2 = newStorage.getMacro("sessions");
		StoreableKeyStroke key2 = m2.getShortcut();
		assertEquals(key, key2);
	}

	@Test
	public void testCopy()
	{
		MacroStorage macros = new MacroStorage();
		assertFalse(macros.isModified());

		macros.addMacro("Default", "sessions", "select * from v$session");
		macros.addMacro("Default", "WHO", "sp_who2");
		macros.addMacro("Default", "clean", "delete from $[table]");
		macros.addMacro("Oracle", "explain ora", "explain plan ${current_statement}; select * from plan_table;");
		assertTrue(macros.isModified());
		macros.resetModified();
		assertFalse(macros.isModified());

		MacroDefinition def = macros.getMacro("who");
		assertEquals("sp_who2", def.getText());
		def.setText("exec sp_who2");
		assertTrue(macros.isModified());

		MacroStorage copy = macros.createCopy();
		assertFalse(copy.isModified());

		MacroDefinition def2 = copy.getMacro("who");
		assertNotNull(def2);
		assertEquals("exec sp_who2", def2.getText());
		def2.setText("sp_who");

		copy.addMacro("Default", "new", "select 42 from dual");

		macros.copyFrom(copy);
		assertTrue(macros.isModified());

		MacroDefinition def3 = macros.getMacro("who");
		assertEquals("sp_who", def3.getText());

		MacroDefinition def4 = macros.getMacro("new");
		assertEquals("select 42 from dual", def4.getText());
		macros.resetModified();
		def4.setText("select 43 from dual");
		assertTrue(macros.isModified());

		MacroDefinition exp = macros.getMacro("explain ora");
		assertNotNull(exp);

	}

	@Test
	public void testEmpty()
	{
		MacroStorage macros = new MacroStorage();
		MacroGroup group1 = new MacroGroup("FirstGroup");
		group1.setVisibleInMenu(true);
		macros.addGroup(group1);

		MacroGroup group2 = new MacroGroup("SecondGroup");
		group2.setVisibleInMenu(true);
		macros.addGroup(group2);

		assertEquals(2, macros.getGroups().size());
		MacroDefinition macro1 = new MacroDefinition("macro1", "macro1");
		MacroDefinition macro2 = new MacroDefinition("macro2", "macro2");
		MacroDefinition macro3 = new MacroDefinition("macro3", "macro3");
		MacroDefinition macro4 = new MacroDefinition("macro4", "macro4");
		MacroDefinition macro5 = new MacroDefinition("macro5", "macro5");

		macros.addMacro(group1, macro1);
		macros.addMacro(group1, macro2);

		macros.addMacro(group2, macro3);
		macros.addMacro(group2, macro4);
		macros.addMacro(group2, macro5);

		assertEquals(2, macros.getGroups().get(0).getSize());
		assertEquals(3, macros.getGroups().get(1).getSize());

		// a group should always be added at the end
		MacroGroup group3 = new MacroGroup("ThirdGroup");
		group3.setSortOrder(0);
		macros.addGroup(group3);
		assertEquals(2, group3.getSortOrder());
	}

	@Test
	public void testConvertOld()
		throws Exception
	{
		TestUtil util = new TestUtil(this.getName());
		Map<String, String> old = new HashMap<>();
		old.put("macro1", "select 42 from dual;");
		old.put("macro2", "explain ${current_statement}");
		old.put("macro3", "select * from pg_stat_activity");
		WbFile oldfile = new WbFile(util.getBaseDir(), "OldMacros.xml");
		WbPersistence writer = new WbPersistence(oldfile.getFullPath());
		writer.writeObject(old);

		MacroStorage newStorage = new MacroStorage(oldfile);

		MacroDefinition m1 = newStorage.getMacro("macro1");
		assertEquals(old.get("macro1"), m1.getText());

		WbFile newfile = new WbFile(util.getBaseDir(), "WbMacros.xml");
		newStorage.saveMacros(newfile);

		MacroStorage new2 = new MacroStorage(newfile);
		assertEquals(m1, new2.getMacro("macro1"));
	}
}
