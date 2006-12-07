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

import java.util.LinkedList;
import workbench.resource.Settings;

/**
 * @author support@sql-workbench.net
 */
public class MessageBuffer
{
	private LinkedList<CharSequence> messages = new LinkedList();
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
		
	public synchronized StringBuilder getBuffer()
	{
		StringBuilder result = new StringBuilder(this.length + 50);
		if (trimmed) result.append("(...)\n");
		
		while (messages.size() > 0)
		{
			CharSequence s = messages.removeFirst();
			result.append(s);
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
	
	public synchronized void append(MessageBuffer buff)
	{
		if (buff == null) return;
		int count = buff.messages.size();
		while (this.messages.size() + count > maxSize)
		{
			String s = (String)messages.removeFirst();
			if (s != null) this.length -= s.length();
		}
		this.messages.addAll(buff.messages);
	}
	
	public synchronized void append(CharSequence s)
	{
		if (StringUtil.isEmptyString(s)) return;
		trimSize();
		this.messages.add(s);
		length += s.length();
	}
	
	public synchronized void appendNewLine()
	{
		append(newLine);
	}

	public String toString()
	{
		return getBuffer().toString();
	}
	
}
