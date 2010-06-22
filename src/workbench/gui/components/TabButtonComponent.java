/*
 * TabButtonComponent.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import workbench.gui.WbSwingUtilities;
import workbench.gui.lnf.LnFHelper;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * Component to be used as tabComponent;
 * Contains a JLabel to show the text and
 * a JButton to close the tab it belongs to
 *
 * @author Thomas Kellerer
 */
public class TabButtonComponent
	extends JPanel
	implements ActionListener, PropertyChangeListener
{
	private final WbTabbedPane pane;
	private final JLabel label;
	private final WbButton closeButton;

	public TabButtonComponent(String title, final WbTabbedPane tabPane, boolean showButton)
	{
		super(new GridBagLayout());
		pane = tabPane;

		boolean opaque = Settings.getInstance().getBoolProperty("workbench.gui.closebutton.opaque", false);
		boolean buttonOnRight = GuiSettings.getShowCloseButtonOnRightSide();

		boolean jGoodies = LnFHelper.isJGoodies();
		if (jGoodies)
		{
			String tabStyle = System.getProperty("Plastic.tabStyle","Default");
			if (!"Metal".equalsIgnoreCase(tabStyle))
			{
				opaque = true;
			}
		}

		label = new JLabel(title);

		closeButton = new WbButton(ResourceMgr.getPng("closePanel"));
		closeButton.setBackground(getBackground());
		Dimension d = new Dimension(12, 15);
		closeButton.setPreferredSize(d);
		closeButton.setMinimumSize(d);
		closeButton.enableBasicRollover();
		closeButton.setFocusable(false);
		closeButton.addActionListener(this);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = (buttonOnRight ? 0 : 1);
		c.gridy = 0;
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = (buttonOnRight ? new Insets(0,0,1,4) : new Insets(0,0,1,0));
		add(label, c);

		c.gridx = (buttonOnRight ? 1 : 0);
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = (buttonOnRight ? WbSwingUtilities.EMPTY_INSETS : new Insets(0,0,0,4));
		add(closeButton, c);
		
		if (!showButton) closeButton.setVisible(showButton);
		setOpaque(opaque);

		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_RESULTTAB_CLOSE_BUTTON_RIGHT);
	}

	public void setRolloverEnabled(boolean flag)
	{
		if (flag)
		{
			closeButton.enableBasicRollover();
		}
		else
		{
			closeButton.disableBasicRollover();
		}
	}
	
	@Override
	public void setOpaque(boolean isOpaque)
	{
		super.setOpaque(isOpaque);
		if (label != null) label.setOpaque(isOpaque);
		if (closeButton != null) closeButton.setOpaque(isOpaque);
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

	private void setComponenentOrder()
	{
		boolean buttonOnRight = GuiSettings.getShowCloseButtonOnRightSide();
		remove(label);
		remove(closeButton);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = (buttonOnRight ? 0 : 1);
		c.gridy = 0;
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = (buttonOnRight ? new Insets(0,0,1,4) : new Insets(0,0,1,0));
		add(label, c);

		c.gridx = (buttonOnRight ? 1 : 0);
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = (buttonOnRight ? WbSwingUtilities.EMPTY_INSETS : new Insets(0,0,0,4));
		add(closeButton, c);
		validate();
	}
	
	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		setComponenentOrder();
	}

}


