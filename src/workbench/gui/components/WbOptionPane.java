/*
 * WbOptionPane.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.components;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import javax.swing.Icon;
import javax.swing.JOptionPane;

/**
 *
 * @author Thomas Kellerer
 */
public class WbOptionPane
	extends JOptionPane
{
	public WbOptionPane(Object message, int messageType, int optionType)
	{
		super(message, messageType, optionType);
	}

	public WbOptionPane(Object message, int messageType, int optionType, Icon icon, Object[] options)
	{
		super(message, messageType, optionType, icon, options);
	}

	public WbOptionPane(Object message, int messageType, int optionType, Icon icon, Object[] options, Object initialValue)
	{
		super(message, messageType, optionType, icon, options, initialValue);
	}

	private boolean allowAnyLength(Object message)
	{
		if (message instanceof String)
		{
			// if the message is a HTML formatted string, we cannot count the characters
			// as the HTML tags will not be displayed and we don't have a way to figure out the real (display) length
			return Boolean.valueOf(((String)message).startsWith("<html>"));
		}
		else
		{
			// if component is used as the message, do not interfere with the formatting either
			return Boolean.TRUE;
		}
	}

	@Override
	public int getMaxCharactersPerLineCount()
	{

		if (allowAnyLength(getMessage()))
		{
			return Integer.MAX_VALUE;
		}

		try
		{
			// Limit the display of the option pane to 75% of the screen width
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			int maxWidth = (int) (screen.width * 0.75);

			FontMetrics fm = getFontMetrics(getFont());
			// use the widest character to calculate the max. number of characters that fit into the 70%
			int charWidth = fm.charWidth('M');
			int numChars = maxWidth / charWidth;

			return numChars;
		}
		catch (Throwable th)
		{
			// fallback, do not display more than 100 characters
			return 100;
		}
	}

}
