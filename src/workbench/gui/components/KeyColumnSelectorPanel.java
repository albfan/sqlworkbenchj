/*
 * KeyColumnSelectorPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JCheckBox;

import javax.swing.JLabel;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  support@sql-workbench.net
 */
public class KeyColumnSelectorPanel
	extends ColumnSelectorPanel
{
	private ColumnIdentifier[] columns;
	private String tableName;
	private JCheckBox saveCheckBox;

	public KeyColumnSelectorPanel(ColumnIdentifier[] cols, TableIdentifier table)
	{
		super(cols);
		this.tableName = (table == null ? "" : table.getTableName());
		this.setSelectionLabel(ResourceMgr.getString("LblHeaderKeyColumnPKFlag"));
		configureInfoPanel();
		this.doLayout();
		this.columns = new ColumnIdentifier[cols.length];
		for (int i=0; i < this.columns.length; i++)
		{
			this.columns[i] = cols[i].createCopy();
			this.setColumnSelected(i, cols[i].isPkColumn());
		}
	}

	protected void configureInfoPanel()
	{
		if (this.tableName == null)
		{
			return;
		}

		String msg = ResourceMgr.getString("MsgSelectKeyColumns").replace("%tablename%", tableName);
		JLabel infoLabel = new JLabel(msg);
		this.infoPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 1.0;

		this.infoPanel.add(infoLabel, c);

		this.saveCheckBox = new JCheckBox(ResourceMgr.getString("LblRememberPKMapping"));
		this.saveCheckBox.setToolTipText(ResourceMgr.getDescription("LblRememberPKMapping"));
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weighty = 1.0;
		c.weightx = 0.0;
		c.insets = new Insets(5, 0, 5, 0);
		this.infoPanel.add(saveCheckBox, c);
	}

	public boolean getSaveToGlobalPKMap()
	{
		return this.saveCheckBox.isSelected();
	}

	public ColumnIdentifier[] getColumns()
	{
		for (int i = 0; i < this.columns.length; i++)
		{
			columns[i].setIsPkColumn(this.isColumnSelected(i));
		}
		return this.columns;
	}
}

