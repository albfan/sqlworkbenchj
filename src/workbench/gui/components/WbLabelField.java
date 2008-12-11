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

import java.awt.Color;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
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
	private boolean defaultOpaque = false;
	
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

	public void setDefaultBackground()
	{
		String cls = UIManager.getLookAndFeel().getClass().getName();
		if ("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel".equals(cls))
		{
			// setting the background to a transparent color seems to be
			// the only way to get a "Label"-like look with the nimbus
			setOpaque(defaultOpaque);
			setBackground(new Color(0, 0, 0, 0));
		}
		else
		{
			setOpaque(true);
			this.setBackground(UIManager.getColor("Label.background"));
			this.setForeground(UIManager.getColor("Label.foreground"));
		}
	}
	
	private void init()
	{
		defaultOpaque = isOpaque();
		setEditable(false);
		mouseListener = new TextComponentMouseListener();
		addMouseListener(mouseListener);
		setBorder(new EmptyBorder(2, 5, 2, 2));
		setDefaultBackground();
	}

	public void addPopupAction(WbAction a)
	{
		mouseListener.addAction(a);
	}
}
