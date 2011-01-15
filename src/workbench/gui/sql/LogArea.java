/*
 * LogArea.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.TextComponentMouseListener;
import workbench.resource.Settings;

/**
 * @author Thomas Kellerer
 */
public class LogArea
	extends JTextArea
	implements PropertyChangeListener
{
	public LogArea()
	{
		super();
		setBorder(WbSwingUtilities.EMPTY_BORDER);
		setFont(Settings.getInstance().getMsgLogFont());
		setEditable(false);
		setLineWrap(true);
		setWrapStyleWord(true);

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
