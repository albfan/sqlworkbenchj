/*
 * LnFManagerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.gui.lnf;

import java.util.List;

import workbench.WbTestCase;
import workbench.resource.Settings;

import workbench.util.CollectionUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class LnFManagerTest
	extends WbTestCase
{
	public LnFManagerTest()
	{
		super("LnFManagerTest");
	}

	@Test
	public void testSeparatorConvert()
	{
		LnFManager mgr = new LnFManager();

		LnFDefinition lnf = new LnFDefinition("Test", "someclass", CollectionUtil.arrayList("lib1.jar", "lib2.jar"));
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

		List<String> deflibs = def.getLibraries();
		assertEquals(2, deflibs.size());
	}

}
