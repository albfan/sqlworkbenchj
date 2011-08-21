/*
 * WbTextCellEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
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

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.RestoreDataAction;
import workbench.gui.actions.SetNullAction;
import workbench.interfaces.NullableEditor;
import workbench.resource.ResourceMgr;

/**
 *
 * @author Thomas Kellerer
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
		TextComponentMouseListener menu = new TextComponentMouseListener();
		menu.addAction(new SetNullAction(this));
		menu.addAction(restoreValue);
		textField.addMouseListener(menu);
		textField.getDocument().addDocumentListener(this);
		super.addCellEditorListener(parent);
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
		Component result = super.getTableCellEditorComponent(table, value, isSelected, row, column);
		textField.selectAll();
		setEditable(!(parentTable != null && parentTable.isReadOnly()));
		changed = false;
		isNull = false;
		DataStoreTableModel model = parentTable.getDataStoreTableModel();
		if (model != null)
		{
			restoreValue.setEnabled(model.isColumnModified(row, column));
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
			this.parentTable.openEditWindow();
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
