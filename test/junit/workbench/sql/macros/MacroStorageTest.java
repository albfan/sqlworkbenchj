/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */
package workbench.sql.macros;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.KeyStroke;
import junit.framework.TestCase;
import workbench.TestUtil;
import workbench.resource.StoreableKeyStroke;
import workbench.util.WbFile;
import workbench.util.WbPersistence;

/**
 *
 * @author support@sql-workbench.net
 */
public class MacroStorageTest
	extends TestCase
{

	public MacroStorageTest(String testName)
	{
		super(testName);
	}

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
		File f = new File(util.getBaseDir(), "macros.xml");
		macros.saveMacros(f);
		MacroStorage newStorage = new MacroStorage();
		newStorage.loadMacros(f);
		MacroDefinition m2 = newStorage.getMacro("sessions");
		StoreableKeyStroke key2 = m2.getShortcut();
		assertEquals(key, key2);
	}

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
	
	public void testConvertOld()
		throws Exception
	{
		TestUtil util = new TestUtil(this.getName());
		Map<String, String> old = new HashMap<String, String>();
		old.put("macro1", "select 42 from dual;");
		old.put("macro2", "explain ${current_statement}");
		old.put("macro3", "select * from pg_stat_activity");
		WbFile oldfile = new WbFile(util.getBaseDir(), "OldMacros.xml");
		WbPersistence writer = new WbPersistence(oldfile.getFullPath());
		writer.writeObject(old);

		MacroStorage newStorage = new MacroStorage();
		newStorage.loadMacros(oldfile);

		MacroDefinition m1 = newStorage.getMacro("macro1");
		assertEquals(old.get("macro1"), m1.getText());

		WbFile newfile = new WbFile(util.getBaseDir(), "WbMacros.xml");
		newStorage.saveMacros(newfile);

		MacroStorage new2 = new MacroStorage();
		new2.loadMacros(newfile);
		assertEquals(m1, new2.getMacro("macro1"));
	}

}
