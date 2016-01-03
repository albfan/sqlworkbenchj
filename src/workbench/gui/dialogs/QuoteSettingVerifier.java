/*
 * QuoteSettingVerifier.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
		escapeBox.addActionListener(this);
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
