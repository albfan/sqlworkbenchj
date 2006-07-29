/*
 * StrBuffer.java
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

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;

/**
 * A dynamic byte[] array
 */
public class ByteBuffer
{
	private byte[] byteData;
	
	public ByteBuffer()
	{
		this(1024);
	}
	
	public ByteBuffer(int initialSize)
	{
		byteData = new byte[initialSize];
	}
	
	/**
	 * Expand the storage size to 'minStorage' number of bytes.
	 */
	private void ensureSize(int minStorage)
	{
		int currentSize = (byteData == null ? 0 : byteData.length);
		
		if (minStorage < currentSize) return;
		
		if (this.byteData == null)
		{
			this.byteData = new byte[minStorage];
		}
		else 
		{
			byte newBuf[] = new byte[minStorage];
			System.arraycopy(this.byteData, 0, newBuf, 0, byteData.length);
			this.byteData = newBuf;
		}
	}

	/**
	 * Returns a reference to the internal buffer. 
	 */
	public byte[] getBuffer()
	{
		return this.byteData;
	}

	public ByteBuffer append(byte[] buf)
	{
		return append(buf, 0, buf.length);
	}

	public ByteBuffer append(byte[] buf, int start, int len)
	{
		int newlen = getLength() + len;
		ensureSize(newlen);
		System.arraycopy(buf, start, byteData, byteData.length, len);
		return this;
	}

	/**
	 * Returns the current length of the ByteBuffer. 
	 */
	public int getLength()
	{
		if (this.byteData == null) return 0;
		return this.byteData.length;
	}

}
