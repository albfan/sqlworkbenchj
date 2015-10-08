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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

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
	private static final Insets JGOODIES_INSETS = new Insets(2, 0, 0, 0);
  private final Insets insets;

	public TabButtonComponent(String title, final WbTabbedPane tabPane, boolean showButton)
	{
    super(new BorderLayout(4,0));
		pane = tabPane;

		boolean jGoodies = LnFHelper.isJGoodies();
		if (jGoodies)
		{
      insets = JGOODIES_INSETS;
		}
    else
    {
      insets = WbSwingUtilities.EMPTY_INSETS;
    }

    label = new JLabel(title)
    {
      @Override
      public Color getBackground()
      {
        return TabButtonComponent.this.getBackground();
      }
    };
    label.setOpaque(true);

		int imgSize = IconMgr.getInstance().getSizeForComponentFont(label);
		ImageIcon img = IconMgr.getInstance().getPngIcon("close-panel", imgSize);
    closeButton = new WbButton(img)
    {
      @Override
      public Color getBackground()
      {
        return TabButtonComponent.this.getBackground();
      }
    };
    closeButton.setOpaque(true);

		Dimension d = new Dimension(imgSize,imgSize);
		closeButton.setPreferredSize(d);
		closeButton.setMinimumSize(d);
		closeButton.enableBasicRollover();
		closeButton.setFocusable(false);
		closeButton.addActionListener(this);

		setupComponents();
		if (!showButton)
    {
      closeButton.setVisible(showButton);
    }

		setOpaque(jGoodies);

		Settings.getInstance().addPropertyChangeListener(this, GuiSettings.PROPERTY_RESULTTAB_CLOSE_BUTTON_RIGHT);
	}

  @Override
  public Color getBackground()
  {
    Color c = null;
    if (pane != null && pane.getTabComponentAt(pane.getSelectedIndex()) == this)
    {
      c = UIManager.getColor("TabbedPane.selected");
    }
    else
    {
      c = UIManager.getColor("TabbedPane.background");
    }

    if (c == null)
    {
      c = super.getBackground();
    }
    return c;
  }

  @Override
  public Insets getInsets(Insets insets)
  {
    return this.insets;
  }

  @Override
  public Insets getInsets()
  {
    return this.insets;
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

  	add(label, BorderLayout.CENTER);

		if (buttonOnRight)
		{
      add(closeButton, BorderLayout.LINE_END);
		}
		else
		{
      add(closeButton, BorderLayout.LINE_START);
		}
		validate();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		setupComponents();
	}

}
