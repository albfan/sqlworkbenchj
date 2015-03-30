/*
 * ResultSetInfoPanel.java
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
package workbench.gui.actions;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import workbench.resource.GuiSettings;

import workbench.db.WbConnection;

import workbench.gui.components.ColumnWidthOptimizer;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.WbTable;
import workbench.gui.renderer.RendererSetup;
import workbench.gui.renderer.SqlTypeRenderer;
import workbench.gui.sql.DwPanel;

import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.ResultInfoDisplayBuilder;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultSetInfoPanel
	extends JPanel
{
	private WbTable display;

	public ResultSetInfoPanel(DwPanel data)
	{
		super(new BorderLayout());
		JScrollPane scroll = new javax.swing.JScrollPane();
		display = new WbTable(false, false, false);
		display.setRendererSetup(new RendererSetup(false));
		scroll.setViewportView(display);

		add(scroll, BorderLayout.CENTER);
		DataStore ds = data.getDataStore();

		if (ds != null)
		{
			ResultInfo info = ds.getResultInfo();
			boolean showComments = GuiSettings.getRetrieveQueryComments();
			WbConnection conn = ds.getOriginalConnection();
			boolean showTablename = false;
			if (conn != null)
			{
				showTablename = conn.getDbSettings().supportsResultMetaGetTable();
			}

			DataStore infoDs = ResultInfoDisplayBuilder.getDataStore(info, showComments, showTablename);
			DataStoreTableModel model = new DataStoreTableModel(infoDs);
			display.setAutoCreateColumnsFromModel(true);
			display.setModel(model);

			TableColumnModel colmod = display.getColumnModel();
			int index = colmod.getColumnIndex("JDBC Type");
			TableColumn col = colmod.getColumn(index);
			col.setCellRenderer(new SqlTypeRenderer(true));

			ColumnWidthOptimizer optimizer = new ColumnWidthOptimizer(display);
			optimizer.optimizeAllColWidth(true);
		}
	}
}
