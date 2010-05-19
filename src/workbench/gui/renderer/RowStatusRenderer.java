/*
 * RowStatusRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import workbench.resource.ResourceMgr;
import workbench.storage.RowData;

/**
 * A renderer to display the status of a row in the result set.
 * <br/>
 * It displays different icons for new and modified, depending on the status of the row.
 *
 * @author  Thomas Kellerer
 */
public class RowStatusRenderer
	extends DefaultTableCellRenderer
{
	private static final ImageIcon STATUS_MODIFIED_ICON = ResourceMgr.getPicture("modifiedrow");
	private static final ImageIcon STATUS_NOT_MODIFIED_ICON = ResourceMgr.getPicture("blank");
	private static final ImageIcon STATUS_NEW_ICON = ResourceMgr.getPicture("newrow");

	private final String newTip = ResourceMgr.getString("TxtRowNew");
	private final String modifiedTip = ResourceMgr.getString("TxtRowModified");
	private final String notModifiedTip = ResourceMgr.getString("TxtRowNotModified");

	public RowStatusRenderer()
	{
		super();
		Dimension dim = new Dimension(18, 18);
		this.setMaximumSize(dim);
		this.setMinimumSize(dim);
		this.setPreferredSize(dim);
		this.setText(null);
		this.setIconTextGap(0);
		this.setHorizontalAlignment(JLabel.LEFT);
	}

	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		try
		{
			int status = (value == null ? RowData.NOT_MODIFIED : ((Integer)value).intValue());
			switch (status)
			{
				case RowData.NEW:
					this.setIcon(STATUS_NEW_ICON);
					this.setToolTipText(newTip);
					break;
				case RowData.MODIFIED:
					this.setIcon(STATUS_MODIFIED_ICON);
					this.setToolTipText(modifiedTip);
					break;
				default:
					this.setIcon(STATUS_NOT_MODIFIED_ICON);
					this.setToolTipText(notModifiedTip);
			}
		}
		catch (Exception e)
		{
			this.setIcon(STATUS_NOT_MODIFIED_ICON);
			this.setToolTipText(notModifiedTip);
		}
		return this;
	}

}
