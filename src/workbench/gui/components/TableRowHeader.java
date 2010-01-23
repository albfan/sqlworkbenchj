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

/**
 *
 * @author Thomas Kellerer
 */
public class TableRowHeader
	extends JList
{
	private TableRowHeaderModel rowModel;

	public TableRowHeader(JTable client)
	{
		super();
		rowModel = new TableRowHeaderModel(client);
		setModel(rowModel);
		setCellRenderer(new RowHeaderRenderer(this, client));
		if (client.getRowCount() == 0)
		{
			setFixedCellWidth(8);
		}
		setSelectionModel(client.getSelectionModel());
		setBackground(client.getBackground());
		setOpaque(false);
	}

	public void modelChanged(int row)
	{
		rowModel.fireModelChanged(row);
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
				JViewport rowHeaderViewPort = scrollPane.getRowHeader();
				if (rowHeaderViewPort != null)
				{
					Component c = rowHeaderViewPort.getView();
					if (c instanceof TableRowHeader)
					{
						ListCellRenderer r = ((TableRowHeader)c).getCellRenderer();
						if (r instanceof RowHeaderRenderer)
						{
							((RowHeaderRenderer)r).dispose();
						}
					}
				}
				scrollPane.setRowHeader(null);
			}
		}
	}
}
