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
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelListener;

/**
 *
 * @author Thomas Kellerer
 */
public class TableRowHeader
	extends JTable
	implements TableModelListener
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
		setBorder(new EmptyBorder(0, 0, 0, 1));
		setRowSelectionAllowed(false);
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
}
