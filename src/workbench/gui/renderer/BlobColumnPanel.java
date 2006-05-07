/*
 * BlobColumnRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import workbench.gui.components.BlobHandler;
import workbench.resource.ResourceMgr;
import workbench.storage.NullValue;

/**
 * Renderer for BLOB datatype...
 * @author  support@sql-workbench.net
 */
public class BlobColumnPanel 
	extends JPanel
{
	private static final int BUTTON_WIDTH = 16;
	private JButton openButton = new JButton("...");
	private JLabel label = new JLabel();
	private BlobHandler blobHandler = new BlobHandler();
	
	public BlobColumnPanel()
	{
		super();
		setLayout(new BorderLayout(0,0));
		Dimension d = new Dimension(BUTTON_WIDTH,BUTTON_WIDTH);
		openButton.setMaximumSize(d);
		openButton.setPreferredSize(d);
		openButton.setMinimumSize(d);
		openButton.setEnabled(true);
		openButton.setFocusable(false);
		openButton.setBorder(BorderFactory.createEtchedBorder());
		add(label,BorderLayout.WEST);
		add(openButton,BorderLayout.EAST);
		openButton.setVisible(true);
		this.setToolTipText(ResourceMgr.getDescription("LblShowBlobInfo", true));
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
		if (value == null || value instanceof NullValue)
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
	
	public void setFont(Font f)
	{
		super.setFont(f);
		if (label != null) label.setFont(f);
	}
	
	public String getLabel() 
	{
		return label.getText();
	}
	
	public void setBackground(Color c)
	{
		super.setBackground(c);
		if (label != null) label.setBackground(c);
	}
	
	public void setForeground(Color c)
	{
		super.setForeground(c);
		if (label != null) label.setForeground(c);
	}
	
}
