package workbench.gui.components;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.event.MouseInputAdapter;

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

	public RowHeightResizer(JTable table)
	{
		this.table = table;
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

	public void mouseDragged(MouseEvent e)
	{
		if (!active) return;

		int newHeight = startHeight + e.getY() - startY;
		newHeight = Math.max(1, newHeight);
		this.table.setRowHeight(row, newHeight);
	}

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

