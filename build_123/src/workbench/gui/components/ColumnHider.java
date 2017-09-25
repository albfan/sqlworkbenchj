/*
 * ColumnHider.java
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
package workbench.gui.components;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnHider
{

	private List<TableColumn> hiddenColumns;
	private TableColumnModel columnModel;

	public ColumnHider(TableColumnModel model)
	{
		columnModel = model;
		hiddenColumns = new ArrayList<TableColumn>(model.getColumnCount());
	}

	public void hideColumn(Object identifier)
	{
		if (isHidden(identifier)) return;

		int index = columnModel.getColumnIndex(identifier);
		if (index > -1)
		{
			TableColumn col = columnModel.getColumn(index);
			this.hiddenColumns.add(col);
			this.columnModel.removeColumn(col);
		}
	}

	public void showColumn(Object identifier)
	{
		TableColumn col = getHiddenColumn(identifier);
		if (col != null)
		{
			columnModel.addColumn(col);
		}
	}

	private boolean isHidden(Object identifier)
	{
		return getHiddenColumn(identifier) != null;
	}

	private TableColumn getHiddenColumn(Object identifier)
	{
		if (identifier == null) return null;
		for (TableColumn col : hiddenColumns)
		{
			Object id = col.getIdentifier();
			if (id != null)
			{
				if (col.getIdentifier().equals(identifier)) return col;
			}
		}
		return null;
	}

}
