/*
 * CellWindowEdit.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.components;

import java.awt.Frame;
import java.awt.Window;

import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

import workbench.resource.ResourceMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class CellWindowEdit
{
	private WbTable table;

	public CellWindowEdit(WbTable table)
	{
		this.table = table;
	}

	public void openEditWindow()
	{
		if (!table.isEditing()) return;

		int col = table.getEditingColumn();
		int row = table.getEditingRow();
		String data = null;
		TableCellEditor editor = table.getCellEditor();

		if (editor instanceof WbTextCellEditor)
		{
			WbTextCellEditor wbeditor = (WbTextCellEditor)editor;
			if (table.isEditing() && wbeditor.isModified())
			{
				data = wbeditor.getText();
			}
			else
			{
				data = table.getValueAsString(row, col);
			}
		}
		else
		{
			data = (String)editor.getCellEditorValue();
		}

		Window owner = SwingUtilities.getWindowAncestor(table);
		Frame ownerFrame = null;
		if (owner instanceof Frame)
		{
			ownerFrame = (Frame)owner;
		}

		String title = ResourceMgr.getString("TxtEditWindowTitle");
		EditWindow w = new EditWindow(ownerFrame, title, data);
		try
		{
			w.setVisible(true);
			if (editor != null)
			{
				// we need to "cancel" the editor so that the data
				// in the editor component will not be written into the
				// table model!
				editor.cancelCellEditing();
			}
			if (!w.isCancelled())
			{
				table.setValueAt(w.getText(), row, col);
			}
		}
		finally
		{
			w.dispose();
		}

	}
}
