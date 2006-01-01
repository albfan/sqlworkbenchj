/*
 * ExportOptionsPanel.java
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import workbench.db.exporter.DataExporter;
import workbench.interfaces.EncodingSelector;

import workbench.resource.ResourceMgr;



/**
 *
 * @author  support@sql-workbench.net
 */
public class ExportOptionsPanel
	extends JPanel
	implements ActionListener, EncodingSelector
{
	private JRadioButton typeSql;
	private JRadioButton typeText;
	private JRadioButton typeXml;
	private JRadioButton typeHtml;
	
	private JCheckBox createTableOption;
	private JCheckBox includeHeadersOption;
	private JTextField commitEvery;
	private JLabel commitLabel;
	private EncodingPanel encPanel;

	public ExportOptionsPanel()
	{
		this.setLayout(new GridBagLayout());
		ButtonGroup type = new ButtonGroup();

		JLabel label = new JLabel(ResourceMgr.getString("LabelExportTypeDesc"));
		this.typeSql = new JRadioButton(ResourceMgr.getString("LabelExportTypeSql"));

		this.typeText = new JRadioButton(ResourceMgr.getString("LabelExportTypeText"));
		this.typeXml = new JRadioButton(ResourceMgr.getString("LabelExportTypeXml"));
		this.typeHtml = new JRadioButton(ResourceMgr.getString("LabelExportTypeHtml"));
		
		this.typeSql.addActionListener(this);
		this.typeText.addActionListener(this);
		this.typeXml.addActionListener(this);
		this.typeHtml.addActionListener(this);

		type.add(typeSql);
		type.add(typeText);
		type.add(typeXml);
		type.add(typeHtml);

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.anchor = GridBagConstraints.WEST;
		Insets leftMarginInsets = new Insets(0, 5, 0, 0);
		gbc.insets = leftMarginInsets;
		this.add(label, gbc);

		Insets emptyInsets = new Insets(0, 0, 0, 0);
		gbc.gridy++;
		gbc.insets = emptyInsets;
		this.add(typeSql, gbc);
		gbc.gridy++;
		

		this.createTableOption = new JCheckBox(ResourceMgr.getString("LabelExportIncludeCreateTable"));
		
		this.add(this.createTableOption, gbc);
		gbc.gridy ++;
		
		this.commitEvery = new JTextField(5);
		this.commitLabel = new JLabel(ResourceMgr.getString("LabelDPCommitEvery"));
		gbc.gridy++;
		gbc.gridwidth = 1;
		
		this.add(this.commitLabel, gbc);

		gbc.gridx ++;
		gbc.gridwidth = 1;
		gbc.insets = leftMarginInsets;
		gbc.anchor = GridBagConstraints.WEST;
		this.add(this.commitEvery, gbc);

		// Modifies gbc.gridy!!!!
		this.addDivider(gbc);

		gbc.gridx--;
		gbc.gridwidth = 2;
		gbc.gridy++;
		gbc.insets = emptyInsets;
		this.add(this.typeText, gbc);

		this.includeHeadersOption = new JCheckBox(ResourceMgr.getString("LabelExportIncludeHeaders"));
		gbc.gridy++;
		this.add(this.includeHeadersOption, gbc);

		// Modifies gbc.gridy!!!!
		this.addDivider(gbc);

		gbc.gridx--;
		gbc.gridwidth = 2;
		gbc.gridy++;
		gbc.insets = emptyInsets;
		this.add(this.typeXml, gbc);

		// Modifies gbc.gridy!!!!
		this.addDivider(gbc);

		gbc.gridx = 0;
		gbc.gridy ++;
		this.encPanel = new EncodingPanel();
		this.add(encPanel, gbc);

		gbc.gridx = 0;
		gbc.gridy ++;

		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		JPanel dummy = new JPanel();
		this.add(dummy, gbc);

		this.typeSql.setSelected(true);
		this.includeHeadersOption.setSelected(false);
		this.includeHeadersOption.setEnabled(false);

	}

	public void setEncoding(String enc)
	{
		this.encPanel.setEncoding(enc);
	}

	public String getEncoding()
	{
		return this.encPanel.getEncoding();
	}

	public void setTypeXml()
	{
		this.typeXml.setSelected(true);
		this.checkOptions();
	}

	public boolean isTypeXml()
	{
		return this.typeXml.isSelected();
	}

	public boolean isTypeHtml()
	{
		return false;
	}
	
	public void setTypeHtml()
	{
		this.typeSql.setSelected(false);
		this.typeXml.setSelected(false);
		this.typeText.setSelected(false);
		this.checkOptions();
	}

	public void setTypeSql()
	{
		this.typeSql.setSelected(true);
		this.checkOptions();
	}

	public boolean isTypeSql()
	{
		return this.typeSql.isSelected();
	}

	public void setTypeText()
	{
		this.typeText.setSelected(true);
		this.checkOptions();
	}

	public boolean isTypeText()
	{
		return this.typeText.isSelected();
	}

	public boolean getIncludeTextHeader()
	{
		return this.includeHeadersOption.isSelected();
	}

	public void setCommitEvery(int aNumber)
	{
		this.commitEvery.setText(Integer.toString(aNumber));
	}

	public boolean getCreateTable()
	{
		return this.createTableOption.isSelected();
	}

	public int getCommitEvery()
	{
		String every = this.commitEvery.getText();
		int result = -1;
		try
		{
			result = Integer.parseInt(every);
		}
		catch (Exception e)
		{
			result = -1;
		}
		return result;
	}

	private void checkOptions()
	{
		this.includeHeadersOption.setEnabled(this.typeText.isSelected());
		this.createTableOption.setEnabled(this.typeSql.isSelected());
		this.commitEvery.setEnabled(this.typeSql.isSelected());
		this.commitLabel.setForeground(this.includeHeadersOption.getForeground());
		int type = -1;
		if (this.isTypeSql())
		{
				type = DataExporter.EXPORT_SQL;
		}
		else if (this.isTypeText())
		{
			type = DataExporter.EXPORT_TXT;
		}
		else if (this.isTypeXml())
		{
			type = DataExporter.EXPORT_XML;
		}
		else if (this.isTypeHtml())
		{
			type = DataExporter.EXPORT_HTML;
		}
		this.firePropertyChange("exportType", -1, type);
	}
	
	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		this.checkOptions();
	}

	private void addDivider(GridBagConstraints current)
	{
		GridBagConstraints gbcDiv = new GridBagConstraints();
		DividerBorder b = new DividerBorder(DividerBorder.HORIZONTAL_MIDDLE);
		JPanel divider = new JPanel();
		divider.setBorder(b);
		gbcDiv.gridx = 0;
		gbcDiv.gridy = current.gridy + 1;
		current.gridy += 2;
		gbcDiv.gridwidth = GridBagConstraints.REMAINDER;
		gbcDiv.insets = new Insets(0, 0, 3, 0);
		gbcDiv.fill = GridBagConstraints.HORIZONTAL;
		gbcDiv.anchor = GridBagConstraints.WEST;
		this.add(divider, gbcDiv);
	}
}
