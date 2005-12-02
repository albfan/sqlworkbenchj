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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;

import javax.swing.JLabel;

import workbench.db.ColumnIdentifier;
import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.PkMapping;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class KeyColumnSelectorPanel
	extends ColumnSelectorPanel
{
	private ColumnIdentifier[] columns;
	private String tableName;
	private JButton saveButton;
	
	public KeyColumnSelectorPanel(ColumnIdentifier[] cols, String table)
	{
		super(cols);
		this.tableName = table;
		configureInfoPanel();
		this.doLayout();
		this.setSelectionLabel(ResourceMgr.getString("LabelHeaderKeyColumnPKFlag"));
		this.columns = new ColumnIdentifier[cols.length];
		for (int i=0; i < this.columns.length; i++)
		{
			this.columns[i] = cols[i].createCopy();
			this.setColumnSelected(i, cols[i].isPkColumn());
		}
	}

	protected void configureInfoPanel()
	{
		if (this.tableName == null) return;
		
		String msg = ResourceMgr.getString("MsgSelectKeyColumns").replaceAll("%tablename%", tableName);
		JLabel infoLabel = new JLabel(msg);
		this.infoPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.BOTH;
    c.anchor = GridBagConstraints .NORTHWEST;
    c.weightx = 1.0;
		
		this.infoPanel.add(infoLabel,c);
		
		msg = ResourceMgr.getString("MsgSavePKMapping");
		this.saveButton = new JButton(msg);
		c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 1;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.weighty = 1.0;
    c.insets = new Insets(5, 0, 5, 0);
		this.saveButton.setBorder(WbSwingUtilities.FLAT_BUTTON_BORDER);
		this.saveButton.addActionListener(this);
		this.infoPanel.add(saveButton, c);
	}
	
	public ColumnIdentifier[] getColumns()
	{
		for (int i=0; i < this.columns.length; i++)
		{
			columns[i].setIsPkColumn(this.isColumnSelected(i));
		}
		return this.columns;
	}

	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		super.actionPerformed(e);
		if (e.getSource() == this.saveButton)
		{
			String fileName = Settings.getInstance().getPKMappingFilename();
			if (fileName == null) 
			{
				File dir = new File(Settings.getInstance().getConfigDir());
				JFileChooser dialog = new JFileChooser(dir);
				int result = dialog.showSaveDialog(this);
				if (result == JFileChooser.APPROVE_OPTION)
				{
					File target = dialog.getSelectedFile();
					fileName = target.getAbsolutePath();
					Settings.getInstance().setPKMappingFilename(fileName);
				}
			}
			
			if (fileName != null)
			{
				ColumnIdentifier[] cols = this.getColumns();
				StringBuffer colNames = new StringBuffer(50);
				for (int i = 0; i < cols.length; i++)
				{
					if (cols[i].isPkColumn())
					{
						if (colNames.length() > 0) colNames.append(',');
						colNames.append(cols[i].getColumnName());
					}
				}
				PkMapping.getInstance().addMapping(this.tableName, colNames.toString());
				PkMapping.getInstance().saveMapping(fileName);
				String msg = ResourceMgr.getString("MsgPkMappingSaved");
				msg = StringUtil.replace(msg, "%filename%", fileName);
				WbSwingUtilities.showMessage(this, msg);
			}
			
		}
	}
	
}

