/*
 * TabButtonComponent.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import workbench.resource.GuiSettings;
import workbench.resource.IconMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.lnf.LnFHelper;

/**
 * Component to be used as tabComponent.
 *
 * Contains a JLabel to show the text and a JButton to close the tab it belongs to
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
	private static final Insets INSETS = new Insets(0, 0, 1, 4);

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

		label = new JLabel(title);

		int imgSize = IconMgr.getInstance().getSizeForComponentFont(label);
		ImageIcon img = IconMgr.getInstance().getPngIcon("closePanel", imgSize);
		closeButton = new WbButton(img);
		closeButton.setBackground(getBackground());
		Dimension d = new Dimension(img.getIconWidth() + 4, img.getIconHeight() + 4);
		closeButton.setPreferredSize(d);
		closeButton.setMinimumSize(d);
		closeButton.enableBasicRollover();
		closeButton.setFocusable(false);
		closeButton.addActionListener(this);

		setupComponents();
		if (!showButton) closeButton.setVisible(showButton);
		setOpaque(opaque);

		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_RESULTTAB_CLOSE_BUTTON_RIGHT);
	}

	@Override
	public void removeNotify()
	{
		super.removeNotify();
		Settings.getInstance().removePropertyChangeListener(this);
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
	public final void setOpaque(boolean isOpaque)
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

	@Override
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

	private void setupComponents()
	{
		boolean buttonOnRight = GuiSettings.getShowCloseButtonOnRightSide();

		remove(label);
		remove(closeButton);

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridy = 0;
		c.gridx = 0;

		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.insets = INSETS;
		if (buttonOnRight)
		{
			add(label, c);
		}
		else
		{
			add(closeButton, c);
		}

		c.gridx ++;
		c.anchor = GridBagConstraints.LINE_END;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.insets = WbSwingUtilities.EMPTY_INSETS;
		if (buttonOnRight)
		{
			add(closeButton, c);
		}
		else
		{
			add(label, c);
		}
		validate();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		setupComponents();
	}

}


