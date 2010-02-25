/*
 * TableRowHeader
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.components;

import javax.swing.*;
import java.awt.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelListener;
import workbench.gui.WbSwingUtilities;

/**
 *
 * @author Thomas Kellerer
 */
public class TableRowHeader
	extends JTable
	implements TableModelListener, ChangeListener
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
			viewport.addChangeListener( this );
		}
	}

	public void rowHeightChanged(int row)
	{
		setRowHeight(row, clientTable.getRowHeight(row));
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
}
