/*
 * StringSequence.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.util;

import workbench.interfaces.CharacterSequence;

/**
 *
 * @author info@sql-workbench.net
 */
public class StringSequence
	implements CharacterSequence
{
	private String source;
	
	/** Creates a new instance of StringSequence */
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
