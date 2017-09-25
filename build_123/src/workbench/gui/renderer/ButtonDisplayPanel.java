/*
 * BlobColumnPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.gui.renderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import workbench.resource.IconMgr;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.FlatButton;

import workbench.util.StringUtil;

/**
 * A panel with a button to open the BlobInfo dialog
 * <br/>
 * If blob data is available the panel will display (BLOB) if the data
 * is null, nothing will be displayed.
 * <br/>
 * @author  Thomas Kellerer
 * @see BlobColumnRenderer
 */
public class ButtonDisplayPanel
	extends JPanel
  implements WbRenderer
{
	private final int BUTTON_WIDTH = IconMgr.getInstance().getSizeForLabel();
	private FlatButton openButton = new FlatButton("...");
	private JLabel label = new JLabel();
	private Insets insets = ToolTipRenderer.getDefaultInsets();

	public ButtonDisplayPanel()
	{
		super();
		setLayout(new GridBagLayout());
		Dimension d = new Dimension(BUTTON_WIDTH,BUTTON_WIDTH);
		openButton.setBasicUI();
		openButton.setFlatLook();
		openButton.setBorder(WbSwingUtilities.FLAT_BUTTON_BORDER);
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
	}

	@Override
	public Insets getInsets()
	{
		return insets;
	}

  public int getButtonWidth()
  {
    if (openButton != null && openButton.isVisible())
    {
      return BUTTON_WIDTH;
    }
    else
    {
      return 0;
    }
  }

	public void setDisplayValue(String value)
	{
    this.label.setText(StringUtil.coalesce(value, ""));
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

  @Override
  public String getDisplayValue()
  {
    return getLabel();
  }

  @Override
  public int getHorizontalAlignment()
  {
    return SwingConstants.LEFT;
  }

  @Override
  public void prepareDisplay(Object value)
  {
  }

  @Override
  public int calculateDisplaySize(Object value)
  {
    int button = (int)(BUTTON_WIDTH * 1.1);
    if (value == null)
    {
      return button;
    }
    Font f = getFont();
    if (f == null) return -1;
    FontMetrics fm = getFontMetrics(getFont());
    if (fm == null) return -1;

    int width = fm.stringWidth(getLabel()) + button + insets.left + insets.right;
    return width;
  }

}
