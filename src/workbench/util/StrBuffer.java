package workbench.util;
/**
 * This is a non-synchronized implementation of string buffer, which
 * offers better performance than the class java.lang.StringBuffer.
 *
 * String y = StrBuffer.reuse().append("new string").toString();
 *
 * @author Thomas Wang
 * @see    java.lang.StringBuffer
 * @version  1.01, 10/14/01
 */
public class StrBuffer
{
	/**
	 * This variable 'numchar' is the number of characters in the buffer.
	 */
	int numchar;
	/**
	 * The variable 'mybuf' is used for character storage.
	 */
	char mybuf[];
	static final ThreadLocal savedObj = new ThreadLocal();
	/**
	 * Make an empty string buffer with 80 characters of storage.
	 */
	public StrBuffer()
	{
		this(80);
	}
	/**
	 * Make an empty string buffer with 'len' characters of storage.
	 *
	 * @param      len   the initial storage capacity.
	 * @exception  NegativeArraySizeException  if the 'len' less than 0.
	 */
	public StrBuffer(int len)
	{
		this.mybuf = new char[len];
	}
	/**
	 * Calling this method has same effect as setLength(0)
	 * Clears the contents of the string buffer.
	 */
	public void resetLength()
	{
		this.numchar = 0;
	}
	/**
	 * Save a StrBuffer object for future reuse.
	 */
	public void save()
	{
		savedObj.set(this);
	}
	/**
	 * Obtain a saved instance of string buffer.
	 * The string buffer returned will be empty.
	 */
	public static StrBuffer reuse()
	{
		StrBuffer mybuf = (StrBuffer) savedObj.get();
		if (mybuf == null)
		{
			mybuf = new StrBuffer();
			savedObj.set(mybuf);
		}
		else mybuf.resetLength();
		return mybuf;
	}
	/**
	 * Expand the storage size to at least 'minStorage' number of characters.
	 */
	void moreStorage(int minStorage)
	{
		int newStorage = (this.mybuf.length * 2) + 5;
		if (newStorage < minStorage)
			newStorage = minStorage;
		char newBuf[] = new char[newStorage];
		System.arraycopy(this.mybuf, 0, newBuf, 0, this.numchar);
		this.mybuf = newBuf;
	}
	/**
	 * Appends the argument string to this string buffer.
	 *
	 * @param   str   a string.
	 * @return  this string buffer
	 */
	public StrBuffer append(String str)
	{
		int oldlen = str.length();
		if (oldlen == 1)
		{
			return this.append(str.charAt(0));
		}
		int newlen = this.numchar + oldlen;
		if (newlen > this.mybuf.length)
			moreStorage(newlen);
		str.getChars(0, oldlen, this.mybuf, this.numchar);
		this.numchar = newlen;
		return this;
	}
	
	/**
	 * Appends the argument string to this string buffer.
	 *
	 * @param   str   a string.
	 * @return  this string buffer
	 */
	public StrBuffer append(char c)
	{
		int newlen = this.numchar + 1;
		if (newlen > this.mybuf.length) moreStorage(newlen);
		this.mybuf[newlen - 1] = c;
		this.numchar = newlen;
		return this;
	}
	
	
	/**
	 * Returns a new string based on contents of string buffer.
	 *
	 * @return  a string
	 */
	public String toString()
	{
		return new String(this.mybuf, 0, this.numchar);
	}
}