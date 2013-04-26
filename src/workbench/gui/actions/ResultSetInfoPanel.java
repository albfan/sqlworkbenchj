/*
 * ResultSetInfoPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.gui.actions;

import java.awt.BorderLayout;
import java.sql.Types;

import javax.swing.JScrollPane;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import workbench.db.ColumnIdentifier;
import workbench.gui.components.ColumnWidthOptimizer;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.WbTable;
import workbench.gui.renderer.RendererSetup;
import workbench.gui.renderer.SqlTypeRenderer;
import workbench.gui.sql.DwPanel;
import workbench.resource.GuiSettings;
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;

/**
 *
 * @author Thomas Kellerer
 */
public class ResultSetInfoPanel
	extends javax.swing.JPanel
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
			String[] cols;
			int[] types;

			boolean showComments = GuiSettings.getRetrieveQueryComments();
			if (showComments)
			{
				cols = new String[] { "INDEX", "COLUMN_NAME", "ALIAS", "DATA_TYPE", "JDBC Type", "REMARKS", "BASE TABLE", "CLASS_NAME"};
				types = new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR , Types.VARCHAR };
			}
			else
			{
				cols = new String[] { "INDEX", "COLUMN_NAME", "ALIAS", "DATA_TYPE", "JDBC Type", "CLASS_NAME"};
				types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.VARCHAR, Types.VARCHAR };
			}

			DataStore infoDs = new DataStore(cols, types);
			for (ColumnIdentifier col : info.getColumns())
			{
				int row = infoDs.addRow();
				int colIndex = 0;
				infoDs.setValue(row, colIndex++, col.getPosition());
				infoDs.setValue(row, colIndex++, col.getColumnName());
				infoDs.setValue(row, colIndex++, col.getColumnAlias());
				infoDs.setValue(row, colIndex++, col.getDbmsType());
				infoDs.setValue(row, colIndex++, col.getDataType());
				if (showComments)
				{
					infoDs.setValue(row, colIndex++, col.getComment());
					infoDs.setValue(row, colIndex++, col.getSourceTableName());
				}
				infoDs.setValue(row, colIndex++, col.getColumnClassName());
			}

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
