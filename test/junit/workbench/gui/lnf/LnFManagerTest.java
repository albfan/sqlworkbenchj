/*
 * LnFManagerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.lnf;

import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class LnFManagerTest
	extends WbTestCase
{
	public LnFManagerTest(String testName)
	{
		super(testName);
	}

	@Override
	protected void setUp()
		throws Exception
	{
		super.setUp();
		TestUtil util = getTestUtil();
		util.emptyBaseDirectory();
	}

	public void testSeparatorConvert()
	{
		LnFManager mgr = new LnFManager();
		LnFDefinition lnf = new LnFDefinition("Test", "someclass", "lib1.jar" + StringUtil.getPathSeparator() + "lib2.jar");
		mgr.addDefinition(lnf);
		mgr.saveLookAndFeelDefinitions();

		Settings set = Settings.getInstance();
		int count = set.getIntProperty("workbench.lnf.count", -1);
		assertEquals("Wrong count", 1, count);

		String libs = set.getProperty("workbench.lnf.0.classpath", null);
		assertNotNull(libs);
		assertEquals("Wrong separator", true, libs.indexOf(LnFDefinition.LNF_PATH_SEPARATOR) > -1);

		List<LnFDefinition> lnflist = mgr.getAvailableLookAndFeels();
		LnFDefinition def = null;
		int realCount = 0;
		for (LnFDefinition l : lnflist)
		{
			if (!l.isBuiltInLnF())
			{
				def = l;
				realCount++;
			}
		}
		assertEquals(1, realCount);

		String deflibs = def.getLibrary();
		assertEquals("Wrong separator", true, deflibs.indexOf(StringUtil.getPathSeparator()) > -1);
	}

}
