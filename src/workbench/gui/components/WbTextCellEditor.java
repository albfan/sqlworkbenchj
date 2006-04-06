/*
 * WbTextCellEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import javax.swing.ActionMap;

import javax.swing.DefaultCellEditor;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.OpenEditWindowAction;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  thomas
 */
public class WbTextCellEditor
	extends DefaultCellEditor
	implements MouseListener
{
	private JTextField textField;
	private WbTable parentTable;
	private Color defaultBackground;
	
	public static final WbTextCellEditor createInstance()
	{
		return createInstance(null);
	}

	public static final WbTextCellEditor createInstance(WbTable parent)
	{
		JTextField field = new JTextField();
		WbTextCellEditor editor = new WbTextCellEditor(parent, field);
		return editor;
	}

	public WbTextCellEditor(WbTable parent, JTextField field)
	{
		super(field);
		defaultBackground = field.getBackground();
		this.parentTable = parent;
		this.textField = field;
		this.textField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.textField.addMouseListener(this);
		this.textField.addMouseListener(new TextComponentMouseListener());
		
//		OpenEditWindowAction a = new OpenEditWindowAction(this);
//		InputMap im = textField.getInputMap(JComponent.WHEN_FOCUSED);
//		ActionMap am = textField.getActionMap();
//		a.addToInputMap(im, am);
		super.addCellEditorListener(parent);
	}

	public void setFont(Font aFont)
	{
		this.textField.setFont(aFont);
	}

	public Color getDefaultBackground()
	{
		return defaultBackground;
	}
	
	public void requestFocus()
	{
		this.textField.requestFocusInWindow();
	}
	
	public void selectAll()
	{
  	this.textField.selectAll();
	}
	
	public Component getTableCellEditorComponent(JTable table, Object value,
							boolean isSelected,int row, int column)
	{
  	Component result = super.getTableCellEditorComponent(table, value, isSelected, row, column);
		textField.selectAll();
		return result;
  }

	public void setBackground(Color c)
	{
		this.textField.setBackground(c);
	}
	
	public boolean shouldSelectCell(EventObject anEvent)
	{
		boolean shouldSelect = super.shouldSelectCell(anEvent);
		if (shouldSelect)
		{
			this.textField.selectAll();
		}
		return shouldSelect;
	}

	public void mouseClicked(java.awt.event.MouseEvent evt)
	{
		if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1)
		{
			this.openEditWindow();
		}
	}

	public void mouseEntered(java.awt.event.MouseEvent mouseEvent)
	{
	}

	public void mouseExited(java.awt.event.MouseEvent mouseEvent)
	{
	}

	public void mousePressed(java.awt.event.MouseEvent mouseEvent)
	{
	}

	public void mouseReleased(java.awt.event.MouseEvent mouseEvent)
	{
	}

	public void cancelCellEditing()
	{
		super.cancelCellEditing();
		fireEditingCanceled();
	}
	
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
			Frame owner = (Frame)SwingUtilities.getWindowAncestor(this.textField);
			String title = ResourceMgr.getString("TxtEditWindowTitle");

			EditWindow w = new EditWindow(owner, title, this.textField.getText());
			w.show();
			if (!w.isCancelled())
			{
				this.textField.setText(w.getText());
			}
			w.dispose();
		}
		else
		{
			this.parentTable.openEditWindow();
		}
	}
}
