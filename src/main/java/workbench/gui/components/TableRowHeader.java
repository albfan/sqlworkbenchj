/*
 * TableRowHeader.java
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
package workbench.gui.components;

import java.beans.PropertyChangeEvent;
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import workbench.gui.WbSwingUtilities;

/**
 *
 * @author Thomas Kellerer
 */
public class TableRowHeader
	extends JTable
	implements ChangeListener, PropertyChangeListener
{
	private TableRowHeaderModel rowModel;
	private RowHeaderRenderer renderer;
	private JTable clientTable;

	public TableRowHeader(JTable client)
	{
		super();
		rowModel = new TableRowHeaderModel(client);
		setModel(rowModel);
		clientTable = client;
		renderer = new RowHeaderRenderer(this, client);
		getColumnModel().getColumn(0).setCellRenderer(renderer);
		setSelectionModel(client.getSelectionModel());
		setBackground(client.getBackground());
		setOpaque(false);
		setBorder(WbSwingUtilities.EMPTY_BORDER);
		setRowSelectionAllowed(false);
		setAutoscrolls(false);
		setFocusable(false);
		clientTable.addPropertyChangeListener("font", this);
	}

	@Override
	public void setFont(Font f)
	{
		super.setFont(f);
		if (renderer != null)
		{
			renderer.setFont(f);
		}
	}

	@Override
	public void addNotify()
	{
		super.addNotify();

		Component c = getParent();

		//  Keep scrolling of the row table in sync with the main table.
		if (c instanceof JViewport)
		{
			JViewport viewport = (JViewport)c;
			viewport.addChangeListener(this);
		}
		setRowHeight(clientTable.getRowHeight());
	}

	/**
	 * Adjust the height of the specified row according to the table's
	 * height for that row
	 */
	public void rowHeightChanged(int row)
	{
		setRowHeight(row, clientTable.getRowHeight(row));
	}

	/**
	 * Adjust the height of all rows according to the clientTable.
	 */
	public void rowHeightChanged()
	{
		if (clientTable == null) return;

		int count = clientTable.getRowCount();

		for (int row = 0; row < count; row++)
		{
			setRowHeight(row, clientTable.getRowHeight(row));
		}
	}

	public static void showRowHeader(JTable table)
	{
		Container p = table.getParent();
		if (p instanceof JViewport)
		{
			Container gp = p.getParent();
			if (gp instanceof JScrollPane)
			{
				JScrollPane scrollPane = (JScrollPane) gp;
				scrollPane.setRowHeaderView(new TableRowHeader(table));
			}
		}
	}

	public static boolean isRowHeaderVisible(JTable table)
	{
		return getRowHeader(table) != null;
	}

	public static TableRowHeader getRowHeader(JTable table)
	{
		Container p = table.getParent();
		if (p instanceof JViewport)
		{
			Container gp = p.getParent();
			if (gp instanceof JScrollPane)
			{
				JScrollPane scrollPane = (JScrollPane) gp;
				JViewport rowHeaderViewPort = scrollPane.getRowHeader();
				if (rowHeaderViewPort != null)
				{
					Component c = rowHeaderViewPort.getView();
					if (c instanceof TableRowHeader)
					{
						return (TableRowHeader)c;
					}
				}
			}
		}
		return null;
	}

	public static void removeRowHeader(JTable table)
	{
		Container p = table.getParent();
		if (p instanceof JViewport)
		{
			Container gp = p.getParent();
			if (gp instanceof JScrollPane)
			{
				JScrollPane scrollPane = (JScrollPane) gp;
				TableRowHeader header = getRowHeader(table);
				if (header != null && header.clientTable != null)
				{
					header.clientTable.getModel().removeTableModelListener(header);
				}
				scrollPane.setRowHeader(null);
			}
		}
	}

	@Override
	public void stateChanged(ChangeEvent e)
	{
		JViewport viewport = (JViewport) e.getSource();
		if (viewport == null) return;

		JScrollPane scrollPane = (JScrollPane)viewport.getParent();
		if (scrollPane == null) return;

		JScrollBar bar = scrollPane.getVerticalScrollBar();
		if (bar == null) return;
		bar.setValue(viewport.getViewPosition().y);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getSource() == clientTable && "font".equals(evt.getPropertyName()))
		{
			Font f = clientTable.getFont();
			setFont(f);
		}
	}
}
