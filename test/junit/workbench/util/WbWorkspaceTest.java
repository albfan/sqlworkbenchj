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
package workbench.util;

import java.io.InputStream;
import junit.framework.TestCase;
import workbench.gui.sql.PanelType;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbWorkspaceTest
	extends TestCase
{

	public WbWorkspaceTest(String testName)
	{
		super(testName);
	}

	public void testOldFormat2()
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
