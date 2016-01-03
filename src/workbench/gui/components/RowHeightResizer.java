/*
 * RowHeightResizer.java
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

import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;

/**
 *
 * @author Thomas Kellerer
 */
public class RowHeightResizer
	extends MouseInputAdapter
{
	private JTable table;
	private boolean active;
	private boolean rowSelectionAllowed;
	private int row;
	private int startY;
	private int startHeight;

	private static final int PIXELS = 5;
	private Cursor lastCursor;
	private static Cursor resizeCursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);

	public RowHeightResizer(JTable tbl)
	{
		super();
		this.table = tbl;
		this.table.addMouseListener(this);
		this.table.addMouseMotionListener(this);
		this.row = -1;
	}

	public void done()
	{
		if (this.table == null) return;
		this.table.removeMouseListener(this);
		this.table.removeMouseMotionListener(this);
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		Point p = e.getPoint();

		if (this.isMouseOverRowMargin(p))
		{
			if (this.lastCursor == null)
			{
				this.lastCursor = this.table.getCursor();
			}
			this.table.setCursor(resizeCursor);
		}
		else
		{
			this.table.setCursor(this.lastCursor);
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		Point p = e.getPoint();

		if (this.isMouseOverRowMargin(p))
		{
			this.active = true;
			this.startY = p.y;
			this.startHeight = table.getRowHeight(row);
			this.rowSelectionAllowed = this.table.getRowSelectionAllowed();
			this.table.setRowSelectionAllowed(false);
		}
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		if (!active) return;

		int newHeight = startHeight + e.getY() - startY;
		newHeight = Math.max(1, newHeight);
		table.setRowHeight(row, newHeight);
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				TableRowHeader header = TableRowHeader.getRowHeader(table);
				if (header != null)
				{
					header.rowHeightChanged(row);
				}
			}
		});
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (!active) return;

		this.table.setRowSelectionAllowed(this.rowSelectionAllowed);
		this.active = false;
		this.row = -1;
	}

	private boolean isMouseOverRowMargin(Point p)
	{
		if (!table.isEnabled()) return false;
		this.row = table.rowAtPoint(p);
		int column = table.columnAtPoint(p);

		if (row == -1 || column == -1) return false;

		Rectangle r = table.getCellRect(row, column, true);

		if (p.y >= r.y + r.height - PIXELS)
		{
			return true;
		}
		return false;
	}

}

