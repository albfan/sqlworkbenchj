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

	public void testOldFormat()
	{
		WbProperties props = new WbProperties(null, 0);
		props.setProperty("dbexplorer.visible", "1");
		props.setProperty("dbexplorer6.currentschema", "*");
		props.setProperty("dbexplorer6.locked", "false");
		props.setProperty("dbexplorer6.procedurelist.divider", "200");
		props.setProperty("dbexplorer6.procedurelist.procedurelist.history", "");
		props.setProperty("dbexplorer6.procedurelist.procedurelist.lastvalue", "");
		props.setProperty("dbexplorer6.tabledata.autoloadrowcount", "true");
		props.setProperty("dbexplorer6.tabledata.autoretrieve", "true");
		props.setProperty("dbexplorer6.tabledata.maxrows", "500");
		props.setProperty("dbexplorer6.tabledata.warningthreshold", "1500");
		props.setProperty("dbexplorer6.tablelist.divider", "324");
		props.setProperty("dbexplorer6.tablelist.exportedtreedivider", "100");
		props.setProperty("dbexplorer6.tablelist.importedtreedivider", "100");
		props.setProperty("dbexplorer6.tablelist.objecttype", "TABLE");
		props.setProperty("dbexplorer6.tablesearcher.column-function", "$col$");
		props.setProperty("dbexplorer6.tablesearcher.criteria", "");
		props.setProperty("dbexplorer6.tablesearcher.divider", "200");
		props.setProperty("dbexplorer6.tablesearcher.excludelobs", "true");
		props.setProperty("dbexplorer6.tablesearcher.maxrows", "0");
		props.setProperty("dbexplorer6.triggerlist.divider", "200");
		props.setProperty("dbexplorer6.triggerlist.triggerlist.history", "");
		props.setProperty("dbexplorer6.triggerlist.triggerlist.lastvalue", "");
		props.setProperty("tab.selected", "5");
		props.setProperty("tab0.append.results", "false");
		props.setProperty("tab0.divider.lastlocation", "363");
		props.setProperty("tab0.divider.location", "351");
		props.setProperty("tab0.locked", "false");
		props.setProperty("tab0.maxrows", "0");
		props.setProperty("tab0.timeout", "0");
		props.setProperty("tab0.title", "Statement");
		props.setProperty("tab1.append.results", "false");
		props.setProperty("tab1.divider.lastlocation", "318");
		props.setProperty("tab1.divider.location", "318");
		props.setProperty("tab1.locked", "false");
		props.setProperty("tab1.maxrows", "0");
		props.setProperty("tab1.timeout", "0");
		props.setProperty("tab1.title", "Statement");
		props.setProperty("tab2.append.results", "false");
		props.setProperty("tab2.divider.lastlocation", "347");
		props.setProperty("tab2.divider.location", "348");
		props.setProperty("tab2.locked", "false");
		props.setProperty("tab2.maxrows", "0");
		props.setProperty("tab2.timeout", "0");
		props.setProperty("tab2.title", "Depot");
		props.setProperty("tab3.append.results", "false");
		props.setProperty("tab3.divider.lastlocation", "314");
		props.setProperty("tab3.divider.location", "314");
		props.setProperty("tab3.locked", "false");
		props.setProperty("tab3.maxrows", "0");
		props.setProperty("tab3.timeout", "0");
		props.setProperty("tab3.title", "Statement");
		props.setProperty("tab4.append.results", "false");
		props.setProperty("tab4.divider.lastlocation", "340");
		props.setProperty("tab4.divider.location", "340");
		props.setProperty("tab4.locked", "false");
		props.setProperty("tab4.maxrows", "0");
		props.setProperty("tab4.timeout", "0");
		props.setProperty("tab4.title", "Statement");
		props.setProperty("tab5.append.results", "false");
		props.setProperty("tab5.divider.lastlocation", "371");
		props.setProperty("tab5.divider.location", "371");
		props.setProperty("tab5.locked", "false");
		props.setProperty("tab5.maxrows", "0");
		props.setProperty("tab5.timeout", "0");
		props.setProperty("tab5.title", "Statement");

		WbWorkspace wksp = new WbWorkspace(props);
		assertEquals(7, wksp.getEntryCount());

		assertEquals(PanelType.sqlPanel, wksp.getPanelType(0));
		assertEquals(PanelType.sqlPanel, wksp.getPanelType(5));
		assertEquals(PanelType.dbExplorer, wksp.getPanelType(6));
	}

	
}
