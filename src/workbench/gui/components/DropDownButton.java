/*
 * DropDownButton.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

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
		enableToolbarRollover();
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

	public void popupMenuWillBecomeVisible(PopupMenuEvent e)
	{
		popupVisible = true;
	}

	public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
	{
		popupVisible = false;
	}

	public void popupMenuCanceled(PopupMenuEvent e)
	{
		popupVisible = false;
	}
}
