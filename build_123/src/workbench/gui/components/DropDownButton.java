/*
 * DropDownButton.java
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
package workbench.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import workbench.resource.IconMgr;

/**
 * @author Thomas Kellerer
 */
public class DropDownButton
	extends WbButton
	implements ActionListener, PopupMenuListener
{
	private JPopupMenu popup;
	private boolean popupVisible;
	private Border menuBorder = BorderFactory.createLineBorder(Color.GRAY, 1);

	public DropDownButton(String label)
	{
		super(label);
		init();
	}

	public DropDownButton(Icon i)
	{
		super(i);
		init();
	}

	private void init()
	{
		setFocusable(false);
		addActionListener(this);
		setBorderPainted(false);
		enableToolbarRollover();

    int iconSize = IconMgr.getInstance().getSizeForLabel();
    Dimension bs = getPreferredSize();

    int hmargin = (int)((bs.height - iconSize) / 2);
    int vmargin = (int)((bs.height - iconSize) / 2);
    setMargin(new Insets(vmargin, hmargin, vmargin, hmargin));
	}

	public void dispose()
	{
		if (this.popup != null)
		{
			this.popup.removePopupMenuListener(this);
			this.popup.setVisible(false);
			this.popup.removeAll();
			this.popup = null;
		}
	}

	public void setDropDownMenu(JPopupMenu menu)
	{
		if (this.popup != null)
		{
			this.popup.removePopupMenuListener(this);
			this.popup.setVisible(false);
			this.popup.removeAll();
		}
		popup = menu;
		popup.setBorder(this.menuBorder);
		popup.addPopupMenuListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent evt)
	{
		if (this.popup == null) return;

		if (popupVisible)
		{
			popup.setVisible(false);
			popupVisible = false;
		}
		else
		{
			popup.show(this, 0, getHeight() - 2);
			popupVisible = true;
		}
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e)
	{
		popupVisible = true;
	}

	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
	{
		popupVisible = false;
	}

	@Override
	public void popupMenuCanceled(PopupMenuEvent e)
	{
		popupVisible = false;
	}
}
