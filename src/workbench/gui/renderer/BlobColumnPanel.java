/*
 * BlobColumnPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.FlatButton;
import workbench.resource.ResourceMgr;

/**
 * A panel with a button to open the BlobInfo dialog
 * <br/>
 * If blob data is available the panel will display (BLOB) if the data
 * is null, nothing will be displayed.
 * <br/>
 * @author  Thomas Kellerer
 */
public class BlobColumnPanel
	extends JPanel
{
	private final int BUTTON_WIDTH = 16;
	private FlatButton openButton = new FlatButton("...");
	private JLabel label = new JLabel();
	private Insets insets = ToolTipRenderer.getDefaultInsets();

	public BlobColumnPanel()
	{
		super();
		setLayout(new GridBagLayout());
		Dimension d = new Dimension(BUTTON_WIDTH,BUTTON_WIDTH);
		openButton.setBasicUI();
		openButton.setFlatLook();
		openButton.setBorder(WbSwingUtilities.FLAT_BUTTON_BORDER);
//		openButton.setMaximumSize(d);
		openButton.setPreferredSize(d);
		openButton.setMinimumSize(d);
		openButton.setEnabled(true);
		openButton.setFocusable(false);
		label.setHorizontalTextPosition(SwingConstants.LEFT);
		label.setVerticalTextPosition(SwingConstants.TOP);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHWEST;
		add(label, c);

		c.gridx = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		add(openButton, c);

		openButton.setVisible(true);
		this.setToolTipText(ResourceMgr.getDescription("LblShowBlobInfo", true));
	}

	@Override
	public Insets getInsets()
	{
		return insets;
	}


	public int getButtonWidth()
	{
		if (openButton != null && openButton.isVisible())
			return BUTTON_WIDTH;
		else
			return 0;
	}

	public void setValue(Object value)
	{
		if (value == null)
		{
			this.label.setText("");
		}
		else
		{
			this.label.setText("(BLOB)");
		}
	}

	public void addActionListener(ActionListener l)
	{
		if (openButton != null) openButton.addActionListener(l);
	}

	public void removeActionListener(ActionListener l)
	{
		if (openButton != null) openButton.removeActionListener(l);
	}

	@Override
	public void setFont(Font f)
	{
		super.setFont(f);
		if (label != null) label.setFont(f);
	}

	public String getLabel()
	{
		return label.getText();
	}

	@Override
	public void setBackground(Color c)
	{
		super.setBackground(c);
		if (label != null) label.setBackground(c);
	}

	@Override
	public void setForeground(Color c)
	{
		super.setForeground(c);
		if (label != null) label.setForeground(c);
	}

}
