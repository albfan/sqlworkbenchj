/*
 * WbLabelField.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import javax.swing.JTextField;
import javax.swing.UIManager;
import workbench.gui.actions.WbAction;

/**
 * A label that is built from a JTextField so that the text can
 * be selected and copied into the clipboard
 * 
 * @author support@sql-workbench.net
 */
public class WbLabelField
	extends JTextField
{
	private TextComponentMouseListener mouseListener;

	public WbLabelField()
	{
		super();
		init();
	}
	
	public WbLabelField(String text)
	{
		super(text);
		init();
	}

	private void init()
	{
		setBorder(null);
		setEditable(false);
		mouseListener =	new TextComponentMouseListener();
		addMouseListener(mouseListener);
		this.setBackground(UIManager.getColor("Label.background"));
		this.setForeground(UIManager.getColor("Label.foreground"));
	}

	public void addPopupAction(WbAction a)
	{
		mouseListener.addAction(a);
	}
	
	@Override
	public void setText(String text)
	{
		if (text != null && text.charAt(0) == ' ')
		{
			// Due to some strange border rendering the first
			// character is not displayed correctly without a leading space
			// even if I add an EmptyBorder
			super.setText(" " + text);
		}
		else
		{
			super.setText(text);
		}
	}

}
