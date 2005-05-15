/*
 * WbTextCellEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

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

import workbench.gui.WbSwingUtilities;
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
	private boolean autoSelect = false;

	/*
	public static final WbTextCellEditor createInstance()
	{
		return createInstance(null, false);
	}
	*/
	public static final WbTextCellEditor createInstance(boolean doAutoSelect)
	{
		return createInstance(null, doAutoSelect);
	}

	public static final WbTextCellEditor createInstance(WbTable parent, boolean doAutoSelect)
	{
		JTextField field = new JTextField();
		WbTextCellEditor editor = new WbTextCellEditor(parent, field, doAutoSelect);
		return editor;
	}

	public WbTextCellEditor(WbTable parent, final JTextField aTextField, boolean doAutoSelect)
	{
		super(aTextField);
		this.parentTable = parent;
		this.textField = aTextField;
		this.autoSelect = doAutoSelect;
		this.textField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.textField.addMouseListener(this);
		this.textField.addMouseListener(new TextComponentMouseListener());
	}

	public void setAutoSelect(boolean aFlag) {  this.autoSelect = aFlag; }
	public boolean getAutoSelect() { return this.autoSelect; }

	public void setFont(Font aFont)
	{
		this.textField.setFont(aFont);
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
  	this.textField.selectAll();
		return result;
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

	private void openEditWindow()
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
