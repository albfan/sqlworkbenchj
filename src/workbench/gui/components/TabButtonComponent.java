/*
 * TabButtonComponent.java
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import workbench.gui.WbSwingUtilities;
import workbench.gui.lnf.LnFHelper;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * Component to be used as tabComponent;
 * Contains a JLabel to show the text and
 * a JButton to close the tab it belongs to
 *
 * @author support@sql-workbench.net
 */
public class TabButtonComponent
	extends JPanel
	implements ActionListener
{
	private final WbTabbedPane pane;
	private final JLabel label;
	private final WbButton closeButton;

	public TabButtonComponent(String title, final WbTabbedPane tabPane, boolean showButton)
	{
		super(new GridBagLayout());
		pane = tabPane;

		boolean opaque = Settings.getInstance().getBoolProperty("workbench.gui.closebutton.opaque", false);

		boolean jGoodies = LnFHelper.isJGoodies();
		if (jGoodies)
		{
			String tabStyle = System.getProperty("Plastic.tabStyle","Default");
			if (!"Metal".equalsIgnoreCase(tabStyle))
			{
				opaque = true;
			}
		}
		setOpaque(opaque);

		label = new JLabel(title);
		label.setOpaque(true);
		closeButton = new WbButton(ResourceMgr.getPng("closePanel"));
		closeButton.setOpaque(true);
		Dimension d = new Dimension(14, 16);
		closeButton.setPreferredSize(d);
		closeButton.setMinimumSize(d);
		closeButton.enableBasicRollover();
		closeButton.setFocusable(false);
		closeButton.addActionListener(this);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0,0,1,4);
		add(label, c);

		c.gridx = 1;
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = WbSwingUtilities.EMPTY_INSETS;
		add(closeButton);
		if (!showButton) closeButton.setVisible(showButton);
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


	public void setButtonVisible(boolean flag)
	{
		closeButton.setVisible(flag);
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


