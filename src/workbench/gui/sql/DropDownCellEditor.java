/*
 * DropDownCellEditor.java
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
package workbench.gui.sql;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

import workbench.resource.GuiSettings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbTable;


/**
 * A cell editor using a JComboBox to edit the value.
 *
 * @author Thomas Kellerer
 */
public class DropDownCellEditor
	implements TableCellEditor
{
	private JComboBox input;
	private WbTable table;

	public DropDownCellEditor(WbTable dataTable)
	{
		table = dataTable;
		input = new JComboBox();
		input.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
		input.setEditable(GuiSettings.getVariablesDDEditable());
	}

	public void setValues(List<String> values)
	{
		input.setModel(new DefaultComboBoxModel(values.toArray()));
	}

	public String getText()
	{
		Object o = input.getSelectedItem();
		if (o == null)
		{
			o = input.getEditor().getItem();
		}
		if (o instanceof String)
		{
			return (String)o;
		}
		return null;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,int row, int column)
	{
		input.setSelectedItem(value);
		WbSwingUtilities.requestFocus((JComponent)input.getEditor().getEditorComponent());
		return input;
	}

	@Override
	public Object getCellEditorValue()
	{
		return getText();
	}

	@Override
	public boolean isCellEditable(EventObject anEvent)
	{
		return true;
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent)
	{
		return true;
	}

	@Override
	public boolean stopCellEditing()
	{
		if (table != null)
		{
			table.editingStopped(new ChangeEvent(this));
		}
		return true;
	}

	@Override
	public void cancelCellEditing()
	{
		if (table != null)
		{
			table.editingCanceled(new ChangeEvent(this));
		}
	}

	@Override
	public void addCellEditorListener(CellEditorListener l)
	{
	}

	@Override
	public void removeCellEditorListener(CellEditorListener l)
	{
	}

	public void addActionListener(ActionListener l)
	{
		input.addActionListener(l);
	}

}
