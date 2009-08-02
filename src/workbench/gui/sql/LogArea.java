/*
 * LogArea.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import workbench.gui.components.TextComponentMouseListener;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net
 */
public class LogArea
	extends JTextArea
	implements PropertyChangeListener
{
	private static final Border logBorder = new EmptyBorder(0,2,0,0);

	public LogArea()
	{
		super();
		// Save the default background while the log component is enabled/editable
		// because we want to use that color when turning off editing again
		// The JGoodies look and feel displays the area in gray if it is not editable
		Color bg = getBackground();

		setBorder(logBorder);
		setFont(Settings.getInstance().getMsgLogFont());
		setEditable(false);
		setLineWrap(true);
		setWrapStyleWord(true);

		// Now that the text area is set to readonly, re-apply the default background color
		setBackground(bg);

		initColors();
		addMouseListener(new TextComponentMouseListener());
		Settings.getInstance().addPropertyChangeListener(this,
			Settings.PROPERTY_EDITOR_FG_COLOR,
			Settings.PROPERTY_EDITOR_BG_COLOR);
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		initColors();
	}

	private void initColors()
	{
		setBackground(Settings.getInstance().getEditorBackgroundColor());
		setForeground(Settings.getInstance().getEditorTextColor());
	}
}
