/*
 * StrWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.util;

import java.io.IOException;
import java.io.Writer;

/**
 *
 * @author  info@sql-workbench.net
 */

public class StrWriter
	extends Writer
{
	private StrBuffer buf;
	
	/**
	 * Create a new string writer, using the default initial string-buffer
	 * size.
	 */
	public StrWriter()
	{
		buf = new StrBuffer();
	}
	
	/**
	 * Create a new string writer, using the specified initial string-buffer
	 * size.
	 *
	 * @param initialSize  an int specifying the initial size of the buffer.
	 */
	public StrWriter(int initialSize)
	{
		if (initialSize < 0)
		{
			throw new IllegalArgumentException("Negative buffer size");
		}
		buf = new StrBuffer(initialSize);
	}
	
	/**
	 * Write a single character.
	 */
	public void write(int c)
	{
		buf.append((char) c);
	}
	
	/**
	 * Write a portion of an array of characters.
	 *
	 * @param  cbuf  Array of characters
	 * @param  off   Offset from which to start writing characters
	 * @param  len   Number of characters to write
	 */
	public void write(char cbuf[], int off, int len)
	{
		if ((off < 0) || (off > cbuf.length) || (len < 0) ||
		((off + len) > cbuf.length) || ((off + len) < 0))
		{
			throw new IndexOutOfBoundsException();
		} 
		else if (len == 0)
		{
			return;
		}
		buf.append(cbuf, off, len);
	}
	
	/**
	 * Write a string.
	 */
	public void write(String str)
	{
		buf.append(str);
	}
	
	/**
	 * Write a portion of a string.
	 *
	 * @param  str  String to be written
	 * @param  off  Offset from which to start writing characters
	 * @param  len  Number of characters to write
	 */
	public void write(String str, int off, int len)
	{
		buf.append(str.substring(off, off + len));
	}
	
	/**
	 * Return the buffer's current value as a string.
	 */
	public String toString()
	{
		return buf.toString();
	}
	
	/**
	 * Return the string buffer itself.
	 *
	 * @return StringBuffer holding the current buffer value.
	 */
	public StrBuffer getBuffer()
	{
		return buf;
	}
	
	/**
	 * Flush the stream.
	 */
	public void flush()
	{
	}
	
	/**
	 * Closing a <tt>StrWriter</tt> has no effect. The methods in this
	 * class can be called after the stream has been closed without generating
	 * an <tt>IOException</tt>.
	 */
	public void close() throws IOException
	{
	}
	
}
