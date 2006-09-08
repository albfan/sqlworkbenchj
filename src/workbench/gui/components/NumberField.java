/*
 * NumberField.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;


import java.awt.Toolkit;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class NumberField extends JTextField
{
	private NumberFormat integerFormatter;
	protected boolean allowDecimals = false;
	protected char decimalSep = '.';
	
	public NumberField()
	{
		super();
		this.init();
	}
	
	public NumberField(int value, int columns)
	{
		super(columns);
		this.init();
		integerFormatter = NumberFormat.getNumberInstance(Locale.US);
		integerFormatter.setParseIntegerOnly(true);
		setValue(value);
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
	
	public char getDecimalChar() { return this.decimalSep; }
	
	public void setAllowDecimals(boolean allowDec)
	{
		this.allowDecimals = allowDec;
	}
	
	public boolean getAllowDecimals() { return this.allowDecimals; }
	
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
			Toolkit.getDefaultToolkit().beep();
		}
		return retVal;
	}
	
	public void setValue(int value)
	{
		setText(integerFormatter.format(value));
	}
	
	protected Document createDefaultModel()
	{
		return new WholeNumberDocument();
	}
	
	protected class WholeNumberDocument extends PlainDocument
	{
		public void insertString(int offs,String str,AttributeSet a)
			throws BadLocationException
		{
			char[] source = str.toCharArray();
			char[] result = new char[source.length];
			int j = 0;
			
			for (int i = 0; i < result.length; i++)
			{
				if (Character.isDigit(source[i]) || (allowDecimals && source[i] == decimalSep))
				{
					result[j++] = source[i];
				}
				else
				{
					Toolkit.getDefaultToolkit().beep();
					//System.err.println("insertString: " + source[i]);
				}
			}
			super.insertString(offs, new String(result, 0, j), a);
		}
	}
}
