/*
 * EncodingPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import workbench.interfaces.EncodingSelector;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author info@sql-workbench.net
 */
public class EncodingPanel
		extends JPanel
		implements EncodingSelector
{
	protected JComboBox encodings = new JComboBox();
	private static String[] charsets;

	public EncodingPanel()
	{
		this(null);
	}

	public static String[] getEncodings()
	{
		if (charsets == null)
		{
			Map sets = java.nio.charset.Charset.availableCharsets();
			Iterator names = sets.keySet().iterator();
			charsets = new String[sets.size()];
			int i=0;
			while (names.hasNext())
			{
				charsets[i] = (String)names.next();
				i++;
			}
		}
		return charsets;
	}

	public EncodingPanel(String encoding)
	{
		getEncodings();
		int count = charsets.length;
		for (int i=0; i < count; i++)
		{
			encodings.addItem(charsets[i]);
		}

		if (encoding == null)
		{
			String defaultEncoding = Settings.getInstance().getDefaultFileEncoding();
			encodings.setSelectedItem(defaultEncoding);
		}
		else
		{
			encodings.setSelectedItem(encoding);
		}
		Dimension d = new Dimension(300, 22);
		encodings.setMaximumSize(d);
		this.setLayout(new GridBagLayout());
		JLabel l =  new JLabel(ResourceMgr.getString("LabelFileEncoding"));

		GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
		c.insets = new java.awt.Insets(0, 5, 0, 5);
    c.fill = java.awt.GridBagConstraints.HORIZONTAL;
    c.anchor = java.awt.GridBagConstraints.NORTHWEST;

		this.add(l, c);

		c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 1;
		c.insets = new java.awt.Insets(5, 5, 0, 5);
    c.fill = java.awt.GridBagConstraints.HORIZONTAL;
    c.anchor = java.awt.GridBagConstraints.NORTHWEST;
    c.weightx = 1.0;
    c.weighty = 1.0;

		this.add(encodings, c);
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