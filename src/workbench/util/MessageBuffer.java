/*
 * MessageBuffer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import workbench.resource.Settings;

/**
 * A class to store messages efficiently.
 * The messages are internally stored in a LinkedList, but only up to 
 * a specified maximum number of entries (not total size in bytes)
 * 
 * If the maximum is reached {@link #getBuffer()} will add "(...)" at the beginning
 * of the generated result to indicate that messages have been cut off.
 * 
 * This ensures that collecting warnings or errors during long running
 * jobs, does not cause an OutOfMemory error.
 * 
 * @author support@sql-workbench.net
 */
public class MessageBuffer
{
	private LinkedList<CharSequence> messages = new LinkedList<CharSequence>();
	private int length = 0;
	private final String newLine = "\n";
	private final int maxSize;
	private boolean trimmed = false;
	
	/**
	 * Create a new MessageBuffer, retrieving the max. number of entries
	 * from the Settings object. If nothing has been specified in the .settings
	 * file, a maximum number of 1000 entries is used.
	 */
	public MessageBuffer()
	{
		this(Settings.getInstance().getIntProperty("workbench.messagebuffer.maxentries", 1000));
	}
	
	/**
	 * Create a new MessageBuffer holding a maximum number of <tt>maxEntries</tt>
	 * Entries in its internal list
	 * @param maxEntries the max. number of entries to hold in the internal list
	 */
	public MessageBuffer(int maxEntries)
	{
		maxSize = maxEntries;
	}

	/**
	 * Returns an unmodifiable reference to the stored messages.
	 */
	public List<CharSequence> getMessages()
	{
		return Collections.unmodifiableList(messages);
	}
	
	public synchronized void clear()
	{
		this.messages.clear();
		this.length = 0;
	}
	
	/**
	 * Create a StringBuilder that contains the collected messages.
	 * Once the result is returned, the internal list is emptied. 
	 * This means the second call to this method returns an empty
	 * buffer if no messages have been added between the calls.
	 */
	public synchronized CharSequence getBuffer()
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
				CharSequence s = messages.removeFirst();
				if (s != null) this.length -= s.length();
			}		
		}
	}
	
	/**
	 * Returns the total length in characters of all messages
	 * that are currently kept in this MessageBuffer
	 */
	public synchronized int getLength()
	{
		return length;
	}
	
	public synchronized void append(MessageBuffer buff)
	{
		if (buff == null) return;
		int count = buff.messages.size();
		if (count == 0) return;
		this.length += buff.length;
		while (this.messages.size() + count > maxSize)
		{
			CharSequence s = messages.removeFirst();
			if (s != null) this.length -= s.length();
		}
		for (CharSequence s : buff.messages)
		{
			this.messages.add(s);
		}
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
