/*
 * UpdateTableSelector.java
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
package workbench.gui.components;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import workbench.resource.ResourceMgr;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.storage.DataStore;

import workbench.util.Alias;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class UpdateTableSelector
{
	private WbTable tableData;

	public UpdateTableSelector(WbTable client)
	{
		tableData = client;
	}

	public TableIdentifier selectUpdateTable()
	{
		if (tableData == null) return null;

		DataStore data = tableData.getDataStore();
		if (data == null) return null;

		String csql = data.getGeneratingSql();
		WbConnection conn = data.getOriginalConnection();
		List<Alias> tables = SqlUtil.getTables(csql, false, conn);

		TableIdentifier table = null;

		if (tables.size() > 1)
		{
			List<String> tableNames = new ArrayList<>(tables.size());
			for (Alias a : tables)
			{
				tableNames.add(a.getObjectName());
			}
			SelectTablePanel p = new SelectTablePanel(tableNames);

			boolean ok = ValidatingDialog.showConfirmDialog(SwingUtilities.getWindowAncestor(tableData), p, ResourceMgr.getString("MsgSelectTableTitle"));
			String selectedTable = null;
			if (ok)
			{
				selectedTable = p.getSelectedTable();
			}
			if (selectedTable != null)
			{
				table = new TableIdentifier(selectedTable, conn);
			}
		}
		else if (tables.size() == 1)
		{
			table = data.getUpdateTable();
			if (table == null)
			{
				table = new TableIdentifier(tables.get(0).getObjectName(), conn);
			}
		}
		return table;
	}

}
