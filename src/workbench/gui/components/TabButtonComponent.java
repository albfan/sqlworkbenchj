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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import workbench.gui.WbSwingUtilities;
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

	public TabButtonComponent(String title, final WbTabbedPane tabPane)
	{
		super(new GridBagLayout());
		pane = tabPane;

		String lnf = UIManager.getLookAndFeel().getClass().getName();
		boolean opaque = Settings.getInstance().getBoolProperty("workbench.gui.closebutton.opaque", false);

		if (lnf.startsWith("com.jgoodies.looks.plastic"))
		{
			String tabStyle = System.getProperty("Plastic.tabStyle","Default");
			if (!"Metal".equalsIgnoreCase(tabStyle))
			{
				opaque = true;
			}
		}
		setOpaque(opaque);

		label = new JLabel(title);
		closeButton = new WbButton(ResourceMgr.getPng("closePanel"))
		{
			public Insets getInsets()
			{
				return WbSwingUtilities.EMPTY_INSETS;
			}
		};

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


