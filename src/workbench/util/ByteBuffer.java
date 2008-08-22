/*
 * ByteBuffer.java
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

/**
 * A dynamic byte[] array which gives direct access to the underlying 
 * byte[] array which is more efficient than ByteArrayOutputStream which 
 * copies the array when calling toByteArray() (thus doubling memory usage) 
 * It is not as efficient as it does not pre-allocate bytes (in order to 
 * be able to give direct access to the underlying array. 
 * {@link #getLength()} returns the physical length of the internal array
 * and is equivalent to getBuffer().length;
 *
 * @author support@sql-workbench.net  
 */
public class ByteBuffer
{
	private byte[] byteData;
	
	public ByteBuffer()
	{
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
	 * May be null if append() has never been called.
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
		if (len < 0) return this;
		int pos = getLength();
		int newlen = getLength() + len;
		ensureSize(newlen);
		System.arraycopy(buf, start, byteData, pos, len);
		return this;
	}

	/**
	 * Returns the current length of this ByteBuffer. 
	 * This is equivalent to getBuffer().length
	 */
	public int getLength()
	{
		if (this.byteData == null) return 0;
		return this.byteData.length;
	}

}
