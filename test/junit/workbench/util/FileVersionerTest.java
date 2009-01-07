/*
 * FileVersionerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import java.util.List;
import junit.framework.TestCase;
import workbench.TestUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class FileVersionerTest
	extends TestCase
{

	public FileVersionerTest(String testName)
	{
		super(testName);
	}

	public void testBackupDir()
		throws Exception
	{
		TestUtil util = new TestUtil("FileVersioner");
		File bckDir = new File(util.getBaseDir(), "backup");
		FileVersioner v = new FileVersioner(3, bckDir.getAbsolutePath(), ";");
		util.emptyBaseDirectory();
		File main = new File(util.getBaseDir(), "mystuff.conf");
		TestUtil.writeFile(main, "this is a test\n");
		v.createBackup(main);
		File bck = new File(bckDir, "mystuff.conf;1");
		assertTrue(bck.exists());
	}

	public void testCreateBackup()
		throws Exception
	{
		FileVersioner v = new FileVersioner(3);
		TestUtil util = new TestUtil("FileVersioner");
		util.emptyBaseDirectory();
		File main = new File(util.getBaseDir(), "mystuff.conf");
		TestUtil.writeFile(main, "this is a test\n");
		
		v.createBackup(main);
		TestUtil.writeFile(main, "1 this is a test\n");
		File bck = new File(util.getBaseDir(), "mystuff.conf.1");
		assertTrue(bck.exists());
		List<String> lines = TestUtil.readLines(bck);
		assertEquals("this is a test", lines.get(0));

		// second backup
		v.createBackup(main);
		TestUtil.writeFile(main, "2 this is a test\n");
		bck = new File(util.getBaseDir(), "mystuff.conf.2");
		assertTrue(bck.exists());
		lines = TestUtil.readLines(bck);
		assertEquals("1 this is a test", lines.get(0));

		// test that version 1 is still the original file
		bck = new File(util.getBaseDir(), "mystuff.conf.1");
		assertTrue(bck.exists());
		lines = TestUtil.readLines(bck);
		assertEquals("this is a test", lines.get(0));

		// third backup, still no rollover expected
		v.createBackup(main);
		TestUtil.writeFile(main, "3 this is a test\n");
		bck = new File(util.getBaseDir(), "mystuff.conf.3");
		assertTrue(bck.exists());
		lines = TestUtil.readLines(bck);
		assertEquals("2 this is a test", lines.get(0));

		// version two and one should still contain the same contents
		bck = new File(util.getBaseDir(), "mystuff.conf.1");
		assertTrue(bck.exists());
		lines = TestUtil.readLines(bck);
		assertEquals("this is a test", lines.get(0));

		bck = new File(util.getBaseDir(), "mystuff.conf.2");
		assertTrue(bck.exists());
		lines = TestUtil.readLines(bck);
		assertEquals("1 this is a test", lines.get(0));

		// forth backup, rollover expected
		v.createBackup(main);
		TestUtil.writeFile(main, "4 this is a test\n");
		for (int i=1; i <= 3; i++)
		{
			bck = new File(util.getBaseDir(), "mystuff.conf." + i);
			assertTrue(bck.exists());
			lines = TestUtil.readLines(bck);
			assertEquals(i + " this is a test", lines.get(0));
		}
		util.emptyBaseDirectory();
	}
}
