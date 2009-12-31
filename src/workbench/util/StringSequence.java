/*
 * StringSequence.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import workbench.interfaces.CharacterSequence;

/**
 * An implementation of the CharacterSequence interface
 * based on a String as the source.
 *
 * @see FileMappedSequence
 * @author Thomas Kellerer
 */
public class StringSequence
	implements CharacterSequence
{
	private CharSequence source;
	
	/**
	 * Create a StringSequence based on the given String
	 */
	public StringSequence(CharSequence s)
	{
		this.source = s;
	}

	public void done()
	{
		this.source = null;
	}

	public int length()
	{
		return source.length();
	}
	
	public char charAt(int index)
	{
		return this.source.charAt(index);
	}

	public CharSequence subSequence(int start, int end)
	{
		return this.source.subSequence(start, end);
	}
	
}
