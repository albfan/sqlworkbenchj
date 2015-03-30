/*
 * BlobColumnRenderer.java
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
package workbench.gui.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.components.*;
import workbench.resource.GuiSettings;

/**
 * A class to render and edit BLOB columns in a result set.
 * <br/>
 * It uses a BlobColumnPanel to display the information to the user. The renderer
 * is registered with the BlobColumnPanel's button as an actionlistener and will then
 * display a dialog with details about the blob.
 * <br/>
 *
 * @see workbench.gui.components.BlobHandler#showBlobInfoDialog(java.awt.Frame, Object, boolean)
 *
 * @author  Thomas Kellerer
 */
public class BlobColumnRenderer
	extends AbstractCellEditor
	implements TableCellEditor, ActionListener, TableCellRenderer, WbRenderer
{
	private BlobColumnPanel displayPanel;
	private Object currentValue;
	private WbTable currentTable;
	private int currentRow;
	private int currentColumn;
	private Color alternateColor = GuiSettings.getAlternateRowColor();
	private Color nullColor = GuiSettings.getNullColor();

	private boolean useAlternatingColors = GuiSettings.getUseAlternateRowColor();

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

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,int row, int column)
	{
		return getComponent(table, value, true, isSelected, row, column);
	}

	@Override
	public int getHorizontalAlignment()
	{
		return SwingConstants.LEFT;
	}

	@Override
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

	public void setBackground(Color c)
	{
		this.displayPanel.setBackground(c);
	}

	@Override
	public Object getCellEditorValue()
	{
		return currentValue;
	}

	@Override
	public String getDisplayValue()
	{
		return displayPanel.getLabel();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		cancelCellEditing();
		boolean ctrlPressed = WbAction.isCtrlPressed(e);
		boolean shiftPressed = WbAction.isShiftPressed(e);
		BlobHandler handler = new BlobHandler();

		boolean allowEditing = true;
		TableModel model = currentTable.getModel();
		if (model instanceof DataStoreTableModel)
		{
			allowEditing = ((DataStoreTableModel)model).getAllowEditing();
		}

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
			handler.showBlobInfoDialog(WbManager.getInstance().getCurrentWindow(), currentValue, !allowEditing);
		}

		if (allowEditing)
		{
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

	@Override
	public void prepareDisplay(Object value)
	{
		// nothing to do
	}

}
