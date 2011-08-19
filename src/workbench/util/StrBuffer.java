/*
 * StrBuffer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;

/**
 * This is a non-synchronized implementation of StringBuffer, which
 * offers better performance than the class java.lang.StringBuffer.
 *
 * Initially copied from http://h21007.www2.hp.com/dspp/tech/tech_TechDocumentDetailPage_IDX/1,1701,2488,00.html
 *
 * This will only have an advantage if this object is not converted to a String object
 * too often. java.lang.StringBuilder can re-use the internal char[] array when
 * it's toString() method is called, whereas StrBuffer.toString() will allocate
 * a new char array due to the constructor of String.
 *
 * So StrBuffer is most efficient when it is never converted to a String object.
 * For this, methods to write to a Stream and a Writer are provided that
 * write out the internal char array directly.
 *
 * @author Thomas Kellerer
 * @see    java.lang.StringBuilder
 */
public class StrBuffer
	implements CharSequence
{
	private static final int DEFAULT_LEN = 80;

	/**
	 * The number of characters in the buffer.
	 */
	private int numchar;

	/**
	 * Used for character storage.
	 */
	private char[] charData;

	/**
	 * Make an empty string buffer with 80 characters of storage.
	 */
	public StrBuffer()
	{
		this(DEFAULT_LEN);
	}

	public StrBuffer(String source)
	{
		if (source == null)
		{
			this.charData = new char[DEFAULT_LEN];
			this.numchar = 0;
		}
		else
		{
			this.charData = new char[source.length()];
			this.append(source);
		}
	}

	public StrBuffer(StrBuffer source)
	{
		if (source == null)
		{
			this.charData = new char[DEFAULT_LEN];
			this.numchar = 0;
		}
		else
		{
			this.charData = new char[source.length()];
			this.append(source);
		}
	}

	/**
	 * Reset the length of this StrBuffer.
	 * The internal array is not trimmed or changed, so this
	 * is very efficient when the StrBuffer is re-used
	 */
	public void reset()
	{
		numchar = 0;
	}

	/**
	 *	Remove count characters from the end of the buffer
	 */
	public StrBuffer removeFromEnd(int count)
	{
		if (count > this.numchar) this.numchar = 0;
		this.numchar -= count;
		return this;
	}

	/**
	 * Make an empty string buffer with 'len' characters of storage.
	 *
	 * @param      len   the initial storage capacity.
	 * @exception  NegativeArraySizeException  if the 'len' less than 0.
	 */
	public StrBuffer(int len)
	{
		this.charData = new char[len];
		this.numchar = 0;
	}

	/**
	 * Expand the storage size to at least 'minStorage' number of characters.
	 */
	private void moreStorage(int minStorage)
	{
		int newStorage = (int)((double)this.charData.length * 1.2) + 5;

		if (newStorage < minStorage) newStorage = minStorage;
		if (this.charData == null)
		{
			this.charData = new char[newStorage];
		}
		if (this.charData != null)
		{
			char[] newBuf = new char[newStorage];
			System.arraycopy(this.charData, 0, newBuf, 0, this.numchar);
			this.charData = newBuf;
		}
	}

	/**
	 * This is exposed, so that the StrBuffer
	 * can be used when writing to a Writer without
	 * the need to copy the char[] array
	 */
	public char[] getBuffer()
	{
		return this.charData;
	}

	public StrBuffer append(long i)
	{
		return this.append(Long.toString(i));
	}

	public StrBuffer append(int i)
	{
		return this.append(NumberStringCache.getNumberString(i));
	}

	public StrBuffer append(char[] buf, int start, int len)
	{
		int newlen = this.numchar + len;

		if (newlen > this.charData.length)	moreStorage(newlen);

		System.arraycopy(buf, start, charData, numchar, len);
		this.numchar = newlen;
		return this;
	}

	/**
	 *	Appends the passed StrBuffer to this StrBuffer
	 */
	public final StrBuffer append(StrBuffer str)
	{
		if (str == null) return this;
		int len = str.numchar;
		if (len == 0) return this;
		if (len == 1)
		{
			return this.append(str.charData[0]);
		}
		return append(str.charData, 0, len);
	}

	public boolean endsWith(char c)
	{
		if (numchar == 0) return false;
		return this.charData[this.numchar - 1] == c;
	}

	public void rtrim()
	{
		if (this.numchar == 0) return;
		while (this.charData[this.numchar - 1] <= (char)32)
		{
			this.numchar --;
		}
	}

	/**
	 *	Returns the current length of this StrBuffer
	 */
	@Override
	public int length()
	{
		return this.numchar;
	}

	/**
	 * Appends the character to this StrBuffer.
	 *
	 * @param   c the character to append
	 * @return  this string buffer
	 */
	public StrBuffer append(char c)
	{
		int newlen = this.numchar + 1;
		if (newlen > this.charData.length) moreStorage(newlen);
		this.charData[newlen - 1] = c;
		this.numchar = newlen;
		return this;
	}

	public StrBuffer insert(int index, char c)
	{
		int newlen = this.numchar + 1;
		if (newlen > this.charData.length)
		{
			char[] newBuf = new char[newlen + 10];
			System.arraycopy(this.charData, 0, newBuf, 0, index);
			System.arraycopy(this.charData, index, newBuf, index + 1, (numchar - index));
			this.charData = newBuf;
		}
		this.charData[index] = c;
		this.numchar = newlen;
		return this;
	}

	public StrBuffer remove(int index)
	{
		int num = this.numchar - index - 1;

		if (num > 0)
		{
			System.arraycopy(charData, index+1, charData, index, num);
		}
		this.numchar --;
		this.charData[numchar] = 0;
		return this;
	}

	public StrBuffer remove(int start, int end)
	{
		if (start < 0)
		{
			throw new StringIndexOutOfBoundsException(start);
		}
		if (end > numchar)
		{
			end = numchar;
		}
		if (start > end)
		{
			throw new StringIndexOutOfBoundsException();
		}

		int len = end - start;
		if (len > 0)
		{
			System.arraycopy(charData, start + len, charData, start, numchar - end);
			numchar -= len;
		}
		return this;
	}

	public final StrBuffer append(CharSequence s)
	{
		if (s == null) return this;
		int len = s.length();
		if (len == 0) return this;
		if (len == 1)
		{
			return this.append(s.charAt(0));
		}
		int newlen = this.numchar + len;
		if (newlen > this.charData.length) moreStorage(newlen);
		for (int i=0; i < len; i++)
		{
			this.charData[i + numchar] = s.charAt(i);
		}
		//b.getChars(0, len, this.charData, numchar);
		this.numchar = newlen;
		return this;
	}

	/**
	 * Returns a new string based on the contents of this StrBuffer.
	 *
	 * @return  a string
	 */
	@Override
	public String toString()
	{
		return new String(this.charData, 0, this.numchar);
	}

	@Override
	public char charAt(int index)
	{
		if (index >= this.numchar) throw new IndexOutOfBoundsException(index + " >= " + this.numchar);
		return this.charData[index];
	}

	@Override
	public CharSequence subSequence(int start, int end)
	{
		if (start < 0) throw new IndexOutOfBoundsException("start must be >= 0");
		if (end < 0) throw new IndexOutOfBoundsException("end must be >=0");
		if (end > this.numchar) throw new IndexOutOfBoundsException(end + " >= " + this.numchar);
		if (start > end ) throw new IndexOutOfBoundsException(start + " > "  + end);

		int len = (end - start);
		StrBuffer result = new StrBuffer(len);
		result.numchar = len;
		System.arraycopy(this.charData, start, result.charData, 0, len);
		return result;
	}

	public void appendTo(StringBuilder buff)
	{
		buff.append(this.charData, 0, this.numchar);
	}

	/**
	 * Writes the content of this StrBuffer to the Writer.
	 *
	 * This is more efficient than calling writer.write(StrBuffer)
	 *
	 * @param out
	 * @throws IOException
	 */
	public void writeTo(Writer out)
		throws IOException
	{
		out.write(this.charData, 0, this.numchar);
	}

	public void writeTo(PrintStream out)
	{
		for (int i=0; i < this.numchar; i++)
		{
			out.print(this.charData[i]);
		}
	}

}
