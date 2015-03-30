/*
 * DbExplorerTester.java
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
package workbench.gui.tools;

import javax.swing.JTabbedPane;

import workbench.db.DbMetadata;
import workbench.db.TableColumnsDatastore;
import workbench.db.TableIdentifier;

import workbench.gui.NamedComponentChooser;
import workbench.gui.components.WbTable;
import workbench.gui.dbobjects.DbObjectSourcePanel;
import workbench.gui.dbobjects.TableDataPanel;
import workbench.gui.dbobjects.TableDefinitionPanel;
import workbench.gui.dbobjects.TableListPanel;

import workbench.storage.DataStore;

import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.operators.JComponentOperator;

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
			Thread.sleep(150);
		} while (tPanel.isBusy());

		chooser.setName("dbtablelist");
		JComponentOperator listComp = new JComponentOperator(tableComp, chooser);
		WbTable tableList = (WbTable)listComp.getSource();
		assertTrue(tableList.getRowCount() > 0);

		int personRow = -1;
		for (int row=0; row < tableList.getRowCount(); row++)
		{
			String tableName = tableList.getValueAsString(0, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
			if (tableName.equals("PERSON"))
			{
				personRow = row;
				break;
			}
		}
		assertTrue(personRow > -1);
		tableList.getSelectionModel().setSelectionInterval(personRow, personRow);

		QueueTool tool = new QueueTool();
		tool.waitEmpty();

		chooser.setName("tabledefinition");
		JComponentOperator defComp = new JComponentOperator(tableComp, chooser);
		TableDefinitionPanel defPanel = (TableDefinitionPanel)defComp.getSource();
		do
		{
			Thread.sleep(150);
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
			Thread.sleep(150);
		} while (tPanel.isBusy());
		String sql = source.getText();
		assertTrue(sql.indexOf("CREATE TABLE PERSON") > -1);

		// data panel
		tabbedPane.setSelectedIndex(2);
		tool.waitEmpty();
		TableDataPanel dataPanel = (TableDataPanel)tabbedPane.getSelectedComponent();

		do
		{
			Thread.sleep(150);
		} while (dataPanel.isRetrieving());

		WbTable table = dataPanel.getData();
		assertEquals(1, table.getRowCount());

		// index list
		tabbedPane.setSelectedIndex(3);
		do
		{
			Thread.sleep(150);
		} while (tPanel.isBusy());

		chooser.setName("indexlist");

		JComponentOperator indexComp = new JComponentOperator(tableComp, chooser);
		WbTable indexTable = (WbTable)indexComp.getSource();
		assertEquals(1, indexTable.getRowCount());
	}

}
