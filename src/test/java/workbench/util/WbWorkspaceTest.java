/*
 * WbWorkspaceTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.util;

import java.io.InputStream;

import workbench.gui.sql.PanelType;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbWorkspaceTest
{

	@Test
	public void testOldFormat()
		throws Exception
	{
		InputStream in = getClass().getResourceAsStream("tabinfo_1.properties");
		WbProperties props = new WbProperties(null, 0);
		props.load(in);
		in.close();
		WbWorkspace wksp = new WbWorkspace(props);
		assertEquals(8, wksp.getEntryCount());
		assertEquals(PanelType.sqlPanel, wksp.getPanelType(0));
		assertEquals(PanelType.dbExplorer, wksp.getPanelType(7));

	}
}
