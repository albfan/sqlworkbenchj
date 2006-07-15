/*
 * EncodingPanel.java
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

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import workbench.interfaces.EncodingSelector;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.EncodingUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class EncodingPanel
		extends JPanel
		implements EncodingSelector
{
	protected JComboBox encodings = new JComboBox();
	private JLabel label;
	
	public EncodingPanel()
	{
		this(System.getProperty("file.encoding"), true);
	}

	public EncodingPanel(String encoding)
	{
		this(encoding, true);
	}

	public EncodingPanel(String encoding, boolean showLabel)
	{
		String[] charsets = EncodingUtil.getEncodings();
		int count = charsets.length;
		for (int i=0; i < count; i++)
		{
			encodings.addItem(charsets[i]);
		}

		if (encoding != null)
		{
			encodings.setSelectedItem(encoding);
		}
		Dimension d = new Dimension(300, 22);
		encodings.setMaximumSize(d);
		this.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		if (showLabel)
		{
			label =  new JLabel(ResourceMgr.getString("LblFileEncoding"));
			c.gridx = 0;
			c.gridy = 0;
			c.insets = new java.awt.Insets(0, 0, 0, 0);
			c.fill = java.awt.GridBagConstraints.HORIZONTAL;
			c.anchor = java.awt.GridBagConstraints.NORTHWEST;

			this.add(label, c);
		}
		c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 1;
		c.insets = new java.awt.Insets(5, 0, 0, 0);
    c.fill = java.awt.GridBagConstraints.HORIZONTAL;
    c.anchor = java.awt.GridBagConstraints.NORTHWEST;
    c.weightx = 1.0;
    c.weighty = 1.0;

		this.add(encodings, c);
	}

	public void setLabelVisible(boolean flag)
	{
		if (this.label == null) return;
		this.label.setVisible(flag);
	}
	
	public boolean isLabelVisible()
	{
		if (this.label == null) return false;
		return this.label.isVisible();
	}
	
	public void setEncoding(String enc)
	{
		encodings.setSelectedItem(enc);
	}

	public String getEncoding()
	{
		String enc = (String)this.encodings.getSelectedItem();
		return enc;
	}

}
