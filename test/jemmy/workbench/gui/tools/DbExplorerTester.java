/*
 * DbExplorerTester
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.tools;

import workbench.gui.dbobjects.DbObjectSourcePanel;
import javax.swing.JTabbedPane;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.operators.JComponentOperator;
import workbench.db.DbMetadata;
import workbench.db.TableColumnsDatastore;
import workbench.db.TableIdentifier;
import workbench.gui.NamedComponentChooser;
import workbench.gui.components.WbTable;
import workbench.gui.dbobjects.TableDataPanel;
import workbench.gui.dbobjects.TableDefinitionPanel;
import workbench.gui.dbobjects.TableListPanel;
import workbench.storage.DataStore;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DbExplorerTester
{

	/**
	 * Test the dbExplorer panel.
	 *
	 * The test assumes that exactly one table is defined in the database with the name PERSON
	 */
	public void testPanel(JComponentOperator dbExplorer)
		throws InterruptedException
	{
		NamedComponentChooser chooser = new NamedComponentChooser();
		chooser.setName("tablelistpanel");
		JComponentOperator tableComp = new JComponentOperator(dbExplorer, chooser);
		TableListPanel tPanel = (TableListPanel)tableComp.getSource();

		// wait until the table list has been retrieved
		do
		{
			Thread.sleep(500);
		} while (tPanel.isBusy());

		chooser.setName("dbtablelist");
		JComponentOperator listComp = new JComponentOperator(tableComp, chooser);
		WbTable tableList = (WbTable)listComp.getSource();
		assertEquals(3, tableList.getRowCount());

		int row = 0;
		for (int i=0; i < 3; i++)
		{
			String tableName = tableList.getValueAsString(i, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			if (tableName.equals("PERSON"))
			{
				row = i;
			}
		}
		tableList.getSelectionModel().setSelectionInterval(row, row);

		QueueTool tool = new QueueTool();
		tool.waitEmpty();

		chooser.setName("tabledefinition");
		JComponentOperator defComp = new JComponentOperator(tableComp, chooser);
		TableDefinitionPanel defPanel = (TableDefinitionPanel)defComp.getSource();
		do
		{
			Thread.sleep(250);
		} while (defPanel.isBusy());

		DataStore ds = defPanel.getDataStore();
		assertEquals(3, ds.getRowCount());
		for (int i=0; i < 3; i++)
		{
			if (i == 0)
			{
				assertEquals("NR", ds.getValueAsString(i, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_COL_NAME));
			}
			if (i == 1)
			{
				assertEquals("FIRSTNAME", ds.getValueAsString(i, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_COL_NAME));
			}
			if (i == 2)
			{
				assertEquals("LASTNAME", ds.getValueAsString(i, TableColumnsDatastore.COLUMN_IDX_TABLE_DEFINITION_COL_NAME));
			}
		}

		TableIdentifier tbl = defPanel.getObjectTable();
		assertNotNull(tbl);
		assertEquals("PERSON", tbl.getTableName());

		chooser.setName("displaytab");
		JComponentOperator tabComp = new JComponentOperator(tableComp, chooser);
		JTabbedPane tabbedPane = (JTabbedPane)tabComp.getSource();

		// Index list
		tabbedPane.setSelectedIndex(1);
		tool.waitEmpty();

		DbObjectSourcePanel source = (DbObjectSourcePanel)tabbedPane.getSelectedComponent();
		do
		{
			Thread.sleep(250);
		} while (tPanel.isBusy());
		String sql = source.getText();
		assertTrue(sql.indexOf("CREATE TABLE PERSON") > -1);

		// data panel
		tabbedPane.setSelectedIndex(2);
		tool.waitEmpty();
		TableDataPanel dataPanel = (TableDataPanel)tabbedPane.getSelectedComponent();

		do
		{
			Thread.sleep(250);
		} while (dataPanel.isRetrieving());

		WbTable table = dataPanel.getData();
		assertEquals(1, table.getRowCount());

		// index list
		tabbedPane.setSelectedIndex(3);
		do
		{
			Thread.sleep(250);
		} while (tPanel.isBusy());

		chooser.setName("indexlist");

		JComponentOperator indexComp = new JComponentOperator(tableComp, chooser);
		WbTable indexTable = (WbTable)indexComp.getSource();
		assertEquals(1, indexTable.getRowCount());
	}

}
