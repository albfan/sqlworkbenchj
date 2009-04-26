/*
 * WbLabelField.java
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

import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicTextFieldUI;
import workbench.gui.actions.WbAction;
import workbench.resource.Settings;

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
		setUI(new BasicTextFieldUI());
		setEditable(false);
		setOpaque(true);
		mouseListener = new TextComponentMouseListener();
		addMouseListener(mouseListener);
		setBorder(new EmptyBorder(2, 5, 2, 2));
		Font f = UIManager.getFont("Label.font");
		if (f == null)
		{
			f = Settings.getInstance().getStandardFont();
		}
		setFont(f);
		setBackground(UIManager.getColor("Label.background"));
		setForeground(UIManager.getColor("Label.foreground"));
	}

	public void addPopupAction(WbAction a)
	{
		mouseListener.addAction(a);
	}
}
