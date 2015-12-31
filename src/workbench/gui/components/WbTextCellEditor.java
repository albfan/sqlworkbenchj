/*
 * WbTextCellEditor.java
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import workbench.interfaces.NullableEditor;
import workbench.resource.ResourceMgr;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.RestoreDataAction;
import workbench.gui.actions.SelectFkValueAction;
import workbench.gui.actions.SetNullAction;
import workbench.gui.actions.WbAction;

import workbench.util.WbDateFormatter;

/**
 * A cell editor using a JTextField to edit the value.
 *
 * This editor is usable for nearly all kinds of data, except multiline text.
 *
 * For Multiline text use {@link WbCellEditor}
 *
 * @author Thomas Kellerer
 * @see WbCellEditor
 */
public class WbTextCellEditor
	extends DefaultCellEditor
	implements MouseListener, DocumentListener, NullableEditor
{
	private JTextField textField;
	private WbTable parentTable;
	private Color defaultBackground;
	private boolean changed;
	private boolean isNull;
	private RestoreDataAction restoreValue;
	private SetNullAction setNull;
	private SelectFkValueAction selectFk;
	private TextComponentMouseListener contextMenu;

	public static WbTextCellEditor createInstance()
	{
		return createInstance(null);
	}

	public static WbTextCellEditor createInstance(WbTable parent)
	{
		JTextField field = new JTextField();
		WbTextCellEditor editor = new WbTextCellEditor(parent, field);
		return editor;
	}

	public WbTextCellEditor(WbTable parent, JTextField field)
	{
		super(field);
		defaultBackground = field.getBackground();
		parentTable = parent;
		textField = field;
		textField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		textField.addMouseListener(this);
		restoreValue = new RestoreDataAction(this);
		contextMenu = new TextComponentMouseListener();

		setNull = new SetNullAction(this);
		contextMenu.addAction(setNull);
		setNull.addToInputMap(textField);

		contextMenu.addAction(restoreValue);
		restoreValue.addToInputMap(textField);

		selectFk = new SelectFkValueAction(parent);
		contextMenu.addAction(selectFk);
		selectFk.addToInputMap(textField);

		textField.addMouseListener(contextMenu);
		textField.getDocument().addDocumentListener(this);
		super.addCellEditorListener(parent);
	}

	public void addActionListener(ActionListener l)
	{
		if (textField != null)
		{
			textField.addActionListener(l);
		}
	}

	public void removeActionListener(ActionListener l)
	{
		if (textField != null)
		{
			textField.removeActionListener(l);
		}
	}

	public void dispose()
	{
		WbSwingUtilities.removeAllListeners(textField);
		WbAction.dispose(restoreValue, setNull, selectFk);
		contextMenu.dispose();
		if (textField != null)
		{
			ActionListener[] listeners = textField.getActionListeners();
			if (listeners != null)
			{
				for (ActionListener l : listeners)
				{
					textField.removeActionListener(l);
				}
			}
		}
	}

	@Override
	public void restoreOriginal()
	{
		int row = parentTable.getEditingRow();
		int col = parentTable.getEditingColumn();
		if (row >= 0 && col >= 0)
		{
			Object oldValue = parentTable.restoreColumnValue(row, col);
			if (oldValue != null)
			{
				textField.setText(oldValue.toString());
			}
		}
	}

	public String getText()
	{
		return textField.getText();
	}

	public void setFont(Font aFont)
	{
		textField.setFont(aFont);
	}

	public Color getDefaultBackground()
	{
		return defaultBackground;
	}

	public void requestFocus()
	{
		textField.requestFocusInWindow();
	}

	public void selectAll()
	{
		textField.selectAll();
	}

	@Override
	public Object getCellEditorValue()
	{
		if (isNull) return null;
		return textField.getText();
	}

	@Override
	public void setNull(boolean setToNull)
	{
		if (setToNull)
		{
			textField.setText("");
		}
		isNull = setToNull;
	}

	@Override
	public JTextComponent getEditor()
	{
		return textField;
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,int row, int column)
	{
		String displayValue = WbDateFormatter.getDisplayValue(value);
		Component result = super.getTableCellEditorComponent(table, displayValue, isSelected, row, column);

		textField.selectAll();
		changed = false;
		isNull = false;

		WbTable tbl = (WbTable)table;
		setEditable(!tbl.isReadOnly());
		DataStoreTableModel model = tbl.getDataStoreTableModel();
		if (model != null)
		{
			restoreValue.setEnabled(model.isColumnModified(row, column));
		}
		else
		{
			restoreValue.setEnabled(false);
		}
		return result;
	}

	public void setBackground(Color c)
	{
		textField.setBackground(c);
	}

	@Override
	public boolean shouldSelectCell(EventObject anEvent)
	{
		boolean shouldSelect = super.shouldSelectCell(anEvent);
		if (shouldSelect)
		{
			textField.selectAll();
		}
		return shouldSelect;
	}

	@Override
	public void mouseClicked(java.awt.event.MouseEvent evt)
	{
		if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1)
		{
			this.openEditWindow();
		}
	}

	@Override
	public void mouseEntered(java.awt.event.MouseEvent mouseEvent)
	{
	}

	@Override
	public void mouseExited(java.awt.event.MouseEvent mouseEvent)
	{
	}

	@Override
	public void mousePressed(java.awt.event.MouseEvent mouseEvent)
	{
	}

	@Override
	public void mouseReleased(java.awt.event.MouseEvent mouseEvent)
	{
	}

	@Override
	public void cancelCellEditing()
	{
		super.cancelCellEditing();
		fireEditingCanceled();
	}

	@Override
	public boolean stopCellEditing()
	{
		boolean result = super.stopCellEditing();
		if (result)
		{
			fireEditingStopped();
		}
		return result;
	}

	public void openEditWindow()
	{
		if (this.parentTable == null)
		{
			Frame owner = (Frame) SwingUtilities.getWindowAncestor(this.textField);
			String title = ResourceMgr.getString("TxtEditWindowTitle");
			String value = textField.getText();
			EditWindow w = new EditWindow(owner, title, value);

			try
			{
				w.setVisible(true);
				if (!w.isCancelled())
				{
					this.textField.setText(w.getText());
				}
			}
			finally
			{
				w.dispose();
			}
		}
		else
		{
			CellWindowEdit edit = new CellWindowEdit(parentTable);
			edit.openEditWindow();
		}
	}

	public boolean isModified()
	{
		return changed;
	}

	@Override
	public void insertUpdate(DocumentEvent arg0)
	{
		changed = true;
		setNull(false);
	}

	@Override
	public void removeUpdate(DocumentEvent arg0)
	{
		changed = true;
		setNull(false);
	}

	@Override
	public void changedUpdate(DocumentEvent arg0)
	{
		changed = true;
		setNull(false);
	}

	public void setEditable(boolean flag)
	{
		textField.setEditable(flag);
		if (!flag)
		{
			textField.setBackground(defaultBackground);
		}
		textField.getCaret().setVisible(true);
	}

}
