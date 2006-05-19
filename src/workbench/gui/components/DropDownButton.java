/*
 * DropDownButton.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * @author support@sql-workbench.net
 */
public class DropDownButton
	extends JButton
	implements ActionListener, PopupMenuListener
{
	private JPopupMenu popup;
	private boolean popupVisible = false;
	
	public DropDownButton(String label)
	{
		super(label);
		init();
	}
	
	public DropDownButton(Action a)
	{
		super(a);
		init();
	}
	
	private void init()
	{
//		setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(1, 5, 1, 5)));
		setFocusable(false);
		addActionListener(this);
		setMargin(new Insets(1,5,1,5));
	}

	public void setDropDownMenu(JPopupMenu menu)
	{
		popup = menu;
		popup.addPopupMenuListener(this);
	}

	public void actionPerformed(ActionEvent evt)
	{
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
