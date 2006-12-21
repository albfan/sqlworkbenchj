/*
 * RowDataList.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.util.Arrays;
import java.util.Comparator;

public class RowDataList
{
	private static final int DEFAULT_SIZE = 150;

	// growth factor when increasing the array
	private float grow = 1.10f;
	private int size;
	private RowData data[];

	public RowDataList()
	{
		this(DEFAULT_SIZE);
	}

	public RowDataList(int len)
	{
		this.data = new RowData[len];
		this.size = 0;
	}

	private void grow(int minStorage)
	{
		int newStorage = (int)(this.data.length * grow) + 1;

		if (newStorage < minStorage) newStorage = minStorage;

		if (this.data != null)
		{
			RowData newBuf[] = new RowData[newStorage];
			System.arraycopy(this.data, 0, newBuf, 0, this.size);
			this.data = newBuf;
		}
	}

	public void ensureCapacity(int newSize)
	{
		this.grow(newSize);
	}

	/**
	 * Free all objects stored in the internal array. 
	 * The size 
	 */
	public void reset()
	{
		this.size = 0;
		this.data = new RowData[DEFAULT_SIZE];
	}

	/**
	 * Return the number of rows in this list
	 */
	public int size()
	{
		return this.size;
	}

	/**
	 * Return the row at the given index
	 */
	public RowData get(int index)
	{
		return this.data[index];
	}

	/**
	 * Remove the row at the specified index
	 */
	public void remove(int index)
	{
		int count = size - index - 1;

		if (count > 0)
		{
			System.arraycopy(data, index+1, data, index, count);
		}
		this.size --;
		this.data[size] = null;
	}

	/**
	 * Add a row to this list.
	 * @return the new size of this list 
	 */
	public int add(RowData row)
	{
		int newlen = this.size + 1;
		if (newlen > this.data.length) grow(newlen);
		this.data[newlen - 1] = row;
		this.size = newlen;
		return this.size;
	}

	/**
	 * Add a row at a specific index in this list
	 */
	public int add(int index, RowData row)
	{
		int newlen = this.size + 1;
		if (newlen > this.data.length)
		{
			// we are not using ensureCapacity here to optimize
			// the calls to System.arraycopy
			RowData newBuf[] = new RowData[(int)(newlen * grow)];
			System.arraycopy(this.data, 0, newBuf, 0, index);
			System.arraycopy(this.data, index, newBuf, index + 1, (size - index));
			this.data = newBuf;
		}
		else
		{
			System.arraycopy(this.data, index, this.data, index + 1, (size - index));
		}
		this.data[index] = row;
		this.size = newlen;
		return index;
	}

	public void sort(Comparator comp)
	{
		Arrays.sort(this.data, 0, this.size, comp);
	}
	
}
