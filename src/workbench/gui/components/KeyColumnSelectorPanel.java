/*
 * KeyColumnSelectorPanel.java
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JButton;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import workbench.db.ColumnIdentifier;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  support@sql-workbench.net
 */
public class KeyColumnSelectorPanel
	extends ColumnSelectorPanel
{
	private ColumnIdentifier[] columns;
	
	public KeyColumnSelectorPanel(ColumnIdentifier[] cols, String table)
	{
		super(cols);
		String msg = ResourceMgr.getString("MsgSelectKeyColumns").replaceAll("%tablename%", table);
		this.setInfoText(msg);
		this.setSelectionLabel(ResourceMgr.getString("LabelHeaderKeyColumnPKFlag"));
		this.columns = new ColumnIdentifier[cols.length];
		for (int i=0; i < this.columns.length; i++)
		{
			this.columns[i] = cols[i].createCopy();
			this.setColumnSelected(i, cols[i].isPkColumn());
		}
	}

	public ColumnIdentifier[] getColumns()
	{
		for (int i=0; i < this.columns.length; i++)
		{
			columns[i].setIsPkColumn(this.isColumnSelected(i));
		}
		return this.columns;
	}
}

