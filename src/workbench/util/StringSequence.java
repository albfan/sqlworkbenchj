/*
 * StringSequence.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
 * @author support@sql-workbench.net
 */
public class StringSequence
	implements CharacterSequence
{
	private String source;
	
	/**
	 * Create a StringSequence based on the given String
	 */
	public StringSequence(String s)
	{
		this.source = s;
	}

	public void done()
	{
		this.source = null;
	}

	public char charAt(int index)
	{
		return this.source.charAt(index);
	}

	public String substring(int start, int end)
	{
		return this.source.substring(start, end);
	}
	
}
