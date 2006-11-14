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
import java.util.LinkedList;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net
 */
public class MessageBuffer
{
	private final boolean useHardReference = Settings.getInstance().getBoolProperty("workbench.messagebuffer.hardreference", true);
	
	private LinkedList messages = new LinkedList();
	private int length = 0;
	private final String newLine = "\n";
	private final int maxSize;
	private boolean trimmed = false;
	
	public MessageBuffer()
	{
		this(Settings.getInstance().getIntProperty("workbench.messagebuffer.maxentries", 1000));
	}
	
	public MessageBuffer(int maxEntries)
	{
		maxSize = maxEntries;
	}

	public synchronized void clear()
	{
		this.messages.clear();
	}
		
	public synchronized StringBuffer getBuffer()
	{
		StringBuffer result = new StringBuffer(this.length + 50);
		if (trimmed) result.append("(...)\n");
		
		while (messages.size() > 0)
		{
			CharSequence s = null;
			if (useHardReference)
			{
				s = (CharSequence)messages.removeFirst();
				result.append(s);
			}
			else
			{
				SoftReference r = (SoftReference)messages.removeFirst();
				s = (CharSequence)r.get();
				if (s == null) s = "(...)\n";
				result.append(s);
				r.clear();
			}
		}
		length = 0;
		return result;
	}

	private synchronized void trimSize()
	{
		if (maxSize > 0 && messages.size() >= maxSize)
		{
			trimmed = true;
			while (messages.size() >= maxSize) 
			{
				String s = (String)messages.removeFirst();
				if (s != null) this.length -= s.length();
			}		
		}
	}
	
	public synchronized int getLength()
	{
		return length;
	}
	
	public synchronized void append(String s)
	{
		if (s == null || s.length() == 0) return;
		if (useHardReference)
		{
			trimSize();
			this.messages.add(s);
		}
		else
		{
			SoftReference r = new SoftReference(s);
			this.messages.add(r);
		}
		length += s.length();
	}
	
	public synchronized void append(StringBuffer s)
	{
		if (s == null || s.length() == 0) return;
		if (useHardReference)
		{
			trimSize();
			this.messages.add(s);
		}
		else
		{
			SoftReference r = new SoftReference(s);
			this.messages.add(r);
		}
		length += s.length();
	}
	
	public synchronized void appendNewLine()
	{
		if (useHardReference)
		{
			trimSize();
			this.messages.add(newLine);
		}
		else
		{
			SoftReference r = new SoftReference(newLine);
			this.messages.add(r);
		}
		length ++;
	}

	public String toString()
	{
		return getBuffer().toString();
	}
	
}
