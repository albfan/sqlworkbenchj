/*
 * RowNumberTable.java
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import workbench.util.StringUtil;
/**
 *
 * @author support@sql-workbench.net
 */
public class RowNumberTable
	extends JTable
{
	private RowNumberRenderer renderer;
	
	public RowNumberTable(TableModel model)
	{
		super(model);
		setAutoCreateColumnsFromModel(true);
		setBackground(UIManager.getColor("Label.background"));
		setForeground(UIManager.getColor("Label.foreground"));
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		renderer = new RowNumberRenderer();
		this.setDefaultRenderer(Integer.class, renderer);
		calculateWidth();
	}

	public void tableChanged(TableModelEvent evt)
	{
		super.tableChanged(evt);
		calculateWidth();
	}
	
	private void calculateWidth()
	{
		if (renderer == null) return;
		int rows = getRowCount();
		StringBuffer b = new StringBuffer(Integer.toString(rows));
		int digits = b.length();
		for (int i=0; i < digits; i++) b.setCharAt(i, '0');
		
		Font f = renderer.getFont();
		if (f == null) f = UIManager.getFont("Label.font".intern());
		
		FontMetrics fm = getFontMetrics(f);
		int width = fm.stringWidth(b.toString()) + 15;
		Dimension d = new Dimension(width, 32768);
		this.setPreferredSize(d);
		this.setPreferredScrollableViewportSize(d);
		this.setMaximumSize(d);
	}
}

class RowNumberRenderer
	implements TableCellRenderer
{
	private JLabel label;
	
	public RowNumberRenderer()
	{
		label = new JLabel();
		label.setBorder(new EmptyBorder(0,0,0,2));
		label.setHorizontalAlignment(SwingConstants.RIGHT);
		label.setOpaque(true);
	}

	public Font getFont() { return label.getFont(); }
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		//label.setText((value == null ? StringUtil.EMPTY_STRING : value.toString()));
		label.setText(value.toString());
		return label;
	}
}
