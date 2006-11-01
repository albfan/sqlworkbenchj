/*
 * MessageBuffer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.lang.ref.SoftReference;

/**
 * @author support@sql-workbench.net
 */
public class MessageBuffer
{
	private SoftReference ref;
	
	public MessageBuffer(int size)
	{
		ensureBuffer(size);
	}
	
	public void ensureBuffer(int size)
	{
		if (ref == null || ref.get() == null)
		{
			ref = new SoftReference(new StringBuffer(size));
		}
	}
	
	public StringBuffer getBuffer()
	{
		if (ref == null) return null;
		return (StringBuffer)ref.get();
	}

	public int getLength()
	{
		StringBuffer b = getBuffer();
		if (b == null) return 0;
		return b.length();
	}
	
	public void append(String s)
	{
		StringBuffer b = getBuffer();
		if (b == null) return;
		b.append(s);
	}
	
	public void append(StringBuffer s)
	{
		StringBuffer b = getBuffer();
		if (b == null) return;
		b.append(s);
	}
	
	public void append(char c)
	{
		StringBuffer b = getBuffer();
		if (b == null) return;
		b.append(c);
	}
	
}
