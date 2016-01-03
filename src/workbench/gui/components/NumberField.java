/*
 * NumberField.java
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
package workbench.gui.components;

import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import java.util.Locale;

import workbench.log.LogMgr;

/**
 * A JTextField that only allows numbers (integers) to be entered.
 *
 * @author Thomas Kellerer
 */
public class NumberField
	extends JTextField
{
	private NumberFormat integerFormatter;
	protected char decimalSep = '.';

	public NumberField()
	{
		super();
		this.init();
	}

	public NumberField(int columns)
	{
		super(columns);
		this.init();
	}

	private void init()
	{
		integerFormatter = NumberFormat.getNumberInstance(Locale.getDefault());
		integerFormatter.setParseIntegerOnly(true);
	}

	public void setDecimalChar(char aDecChar)
	{
		this.decimalSep = aDecChar;
	}

	public char getDecimalChar()
	{
		return this.decimalSep;
	}

	public int getValue()
	{
		int retVal = 0;
		try
		{
			retVal = integerFormatter.parse(getText()).intValue();
		}
		catch (ParseException e)
		{
			// This should never happen because insertString allows
			// only properly formatted data to get in the field.
			LogMgr.logError("NumberField.getValue()", "Invalid value entered", e);
		}
		return retVal;
	}

	public final void setValue(int value)
	{
		setText(integerFormatter.format(value));
	}

	@Override
	protected Document createDefaultModel()
	{
		return new WholeNumberDocument();
	}

	protected class WholeNumberDocument extends PlainDocument
	{
		@Override
		public void insertString(int offs, String str, AttributeSet a)
			throws BadLocationException
		{
			char[] source = str.toCharArray();
			char[] result = new char[source.length];
			int j = 0;

			for (int i = 0; i < result.length; i++)
			{
				if (Character.isDigit(source[i]))
				{
					result[j++] = source[i];
				}
			}
			super.insertString(offs, new String(result, 0, j), a);
		}
	}
}
