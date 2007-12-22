/*
 * BlobColumnRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.EventObject;
import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.components.*;
import workbench.resource.Settings;

/**
 * @author  support@sql-workbench.net
 */
public class BlobColumnRenderer
	extends AbstractCellEditor
	implements TableCellEditor, ActionListener, TableCellRenderer, WbRenderer
{
	private BlobColumnPanel displayPanel;
	private Object currentValue;
	private WbTable currentTable;
	private boolean isPrinting = false;
	private int currentRow;
	private int currentColumn;
	private Color alternateColor = Settings.getInstance().getAlternateRowColor();
	private Color nullColor = Settings.getInstance().getNullColor();
	
	private boolean useAlternatingColors = Settings.getInstance().getUseAlternateRowColor();
	
	public BlobColumnRenderer()
	{
		super();
		this.displayPanel = new BlobColumnPanel();
		this.displayPanel.addActionListener(this);
	}

	public void setFont(Font aFont)
	{
		this.displayPanel.setFont(aFont);
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,int row, int column)
	{
		return getComponent(table, value, true, isSelected, row, column);
	}

	public int getHorizontalAlignment()
	{
		return SwingConstants.LEFT;
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value, 
		                                             boolean isSelected, 
		                                             boolean hasFocus, int row, int column)
	{
		return getComponent(table, value, isSelected, hasFocus, row, column);
	}
	
	private Component getComponent(JTable table, Object value,
							boolean isSelected, boolean hasFocus, int row, int column)
	{
		if (isSelected)
		{
			this.displayPanel.setForeground(table.getSelectionForeground());
			this.displayPanel.setBackground(table.getSelectionBackground());
		}
		else
		{
			this.displayPanel.setForeground(table.getForeground());
			if (value == null && nullColor != null)
			{
				this.displayPanel.setBackground(nullColor);
			}
			else
			{
				if (useAlternatingColors && ((row % 2) == 1))
				{
					this.displayPanel.setBackground(this.alternateColor);
				}
				else
				{
					this.displayPanel.setBackground(table.getBackground());
				}
			}
		}
		if (hasFocus)
		{
			this.displayPanel.setBorder(WbSwingUtilities.FOCUSED_CELL_BORDER);
		}
		else
		{
			this.displayPanel.setBorder(WbSwingUtilities.EMPTY_BORDER);
		}
		
		currentValue = value;
		currentRow = row;
		currentColumn = column;
		currentTable = (WbTable)table;
		displayPanel.setValue(value);
		return displayPanel;
  }
	
	public boolean isCellEditable(EventObject e)
	{
		if (e instanceof MouseEvent)
		{
			MouseEvent evt = (MouseEvent)e;
			this.currentTable = (WbTable)e.getSource();
			
			int clickedColumn = this.currentTable.columnAtPoint(evt.getPoint());
			TableColumnModel model = this.currentTable.getColumnModel();
			int columnOffset = 0;
			for (int i = 0; i < clickedColumn; i++)
			{
				columnOffset += model.getColumn(i).getWidth();
			}
			TableColumn col = model.getColumn(clickedColumn);
			int posInCol = ((int)evt.getPoint().getX() - columnOffset);
			int buttonStart = (col.getWidth() - displayPanel.getButtonWidth());
			boolean buttonClicked = (posInCol >= buttonStart);
			return buttonClicked;
		}
		return false;
	}
	
	public void setBackground(Color c)
	{
		this.displayPanel.setBackground(c);
	}

	public Object getCellEditorValue()
	{
		return currentValue;
	}
	
	public String getDisplayValue() 
	{ 
		return displayPanel.getLabel(); 
	}
	
	public void setUseAlternatingColors(boolean flag)
	{
		this.useAlternatingColors = flag;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		cancelCellEditing(); 
		boolean ctrlPressed = WbAction.isCtrlPressed(e);
		boolean shiftPressed = WbAction.isShiftPressed(e);
		BlobHandler handler = new BlobHandler();
		if (ctrlPressed)
		{
			handler.showBlobAsText(currentValue);
		}		
		else if (shiftPressed)
		{
			handler.showBlobAsImage(currentValue);
		}
		else
		{
			handler.showBlobInfoDialog(WbManager.getInstance().getCurrentWindow(), currentValue);
		}
		File f = handler.getUploadFile();
		if (f != null) 
		{
			currentTable.setValueAt(f, currentRow, currentColumn);
		}
		else if (handler.isChanged())
		{
			currentTable.setValueAt(handler.getNewValue(), currentRow, currentColumn);
		}
		else if (handler.setToNull())
		{
			currentTable.setValueAt(null, currentRow, currentColumn);
		}
	}

}
