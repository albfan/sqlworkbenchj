/*
 * QuoteSettingVerifier
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import workbench.util.QuoteEscapeType;

/**
 *
 * @author Thomas Kellerer
 */
public class QuoteSettingVerifier
	implements ActionListener
{
	private QuoteEscapeSelector escapeBox;
	private JCheckBox quoteAlwaysBox;

	public QuoteSettingVerifier(QuoteEscapeSelector escape, JCheckBox quoteAlways)
	{
		escapeBox = escape;
		escapeBox.addComboBoxActionListener(this);
		quoteAlwaysBox = quoteAlways;
		quoteAlwaysBox.addActionListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == quoteAlwaysBox)
		{
			checkQuote();
		}
		else 
		{
			checkEscape();
		}
	}

	/**
	 * Verify the status after the quoteAlways dropdown has been changed.
	 *
	 */
	private void checkQuote()
	{
		QuoteEscapeType escape = escapeBox.getEscapeType();
		boolean always = quoteAlwaysBox.isSelected();

		if (always && escape == QuoteEscapeType.duplicate)
		{
			escapeBox.setEscapeType(QuoteEscapeType.escape);
		}
	}

	/**
	 * Verify the status after the escapeType dropdown has been changed.
	 *
	 */
	private void checkEscape()
	{
		QuoteEscapeType escape = escapeBox.getEscapeType();
		if (escape == QuoteEscapeType.duplicate)
		{
			quoteAlwaysBox.setEnabled(false);
			quoteAlwaysBox.setSelected(false);
		}
		else
		{
			quoteAlwaysBox.setEnabled(true);
		}
	}
}
