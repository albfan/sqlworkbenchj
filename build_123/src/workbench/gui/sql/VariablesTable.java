/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.sql;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.table.TableCellEditor;

import workbench.gui.components.WbTable;

import workbench.sql.VariablePool;

import workbench.util.CollectionUtil;


/**
 *
 * @author Thomas Kellerer
 */
public abstract class VariablesTable
	extends WbTable
{
	private DropDownCellEditor dropDownEditor;

	public VariablesTable()
	{
		super();
		defaultEditor.addActionListener(this);
		dropDownEditor = new DropDownCellEditor(this);
		dropDownEditor.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		int editRow = getEditingRow();
		stopEditing();
		userStoppedEditing(editRow);
	}

	@Override
	public TableCellEditor getCellEditor(int row, int column)
	{
		if (column == 0)
		{
			return super.getCellEditor(row, column);
		}
		String varName = (String)getValueAt(row, 0);

		List<String> values = VariablePool.getInstance().getLookupValues(varName);

		if (CollectionUtil.isEmpty(values))
		{
			return super.getCellEditor(row, column);
		}
		dropDownEditor.setValues(values);
		return dropDownEditor;
	}

	public abstract void userStoppedEditing(int row);

}
