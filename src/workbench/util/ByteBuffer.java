/*
 * ByteBuffer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

/**
 * A dynamic byte array which gives direct access to the underlying byte array.
 *
 * This is more efficient than ByteArrayOutputStream which
 * copies the array when calling toByteArray() (thus doubling memory usage)
 *
 * It is not as fast as ByteArrayOutputStream because it does not pre-allocate
 * bytes (in order to be able to give direct access to the underlying array).
 *
 * {@link #getLength()} returns the physical length of the internal array
 * and is equivalent to getBuffer().length;
 *
 * @author Thomas Kellerer
 */
public class ByteBuffer
{
	private byte[] byteData;

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
			byte[] newBuf = new byte[minStorage];
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
