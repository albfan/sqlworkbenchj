/*
 * NumberField.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
