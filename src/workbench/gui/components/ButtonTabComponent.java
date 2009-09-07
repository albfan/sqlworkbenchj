/*
 * ButtonTabComponent.java
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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import workbench.gui.WbSwingUtilities;
import workbench.resource.ResourceMgr;

/**
 * Component to be used as tabComponent;
 * Contains a JLabel to show the text and
 * a JButton to close the tab it belongs to
 * 
 * @author support@sql-workbench.net
 */
public class ButtonTabComponent
	extends JPanel
	implements ActionListener
{
	private final WbTabbedPane pane;
	private final JLabel label;
	private final WbButton closeButton;
	
	public ButtonTabComponent(String title, final WbTabbedPane pane)
	{
		super(new GridBagLayout());
		this.pane = pane;
		setOpaque(false);

		label = new JLabel();
		label.setOpaque(false);
		label.setText(title);
		closeButton = new WbButton(ResourceMgr.getPng("closePanel"))
		{
			public Insets getInsets()
			{
				return WbSwingUtilities.EMPTY_INSETS;
			}
		};
		closeButton.setOpaque(false);
		Dimension d = new Dimension(14, 14);
		closeButton.setPreferredSize(d);
		closeButton.setFocusable(false);
		closeButton.enableToolbarRollover();
		closeButton.addActionListener(this);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 0;
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0,0,0,4);
		add(label, c);

		c.gridx = 1;
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(0,0,0,0);
		add(closeButton);
	}

	public void setDisplayedMnemonicIndex(int index)
		throws IllegalArgumentException
	{
		label.setDisplayedMnemonicIndex(index);
	}

	public void setDisplayedMnemonic(char aChar)
	{
		label.setDisplayedMnemonic(aChar);
	}

	public void setDisplayedMnemonic(int key)
	{
		label.setDisplayedMnemonic(key);
	}

	
	public void setEnabled(boolean flag)
	{
		closeButton.setEnabled(flag);
	}
	
	public void setTitle(String title)
	{
		label.setText(title);
	}

	public void setIcon(Icon icon)
	{
		label.setIcon(icon);
	}
	
	@Override
	public void actionPerformed(ActionEvent e)
	{
		int i = pane.indexOfTabComponent(this);
		if (i != -1)
		{
			pane.closeButtonClicked(i);
		}
	}

}


