package workbench.storage;

import java.util.Comparator;
import java.util.Arrays;

public class RowDataList
{
	public static final RowDataList EMPTY_BUFFER = new RowDataList();
	private static final int DEFAULT_SIZE = 150;

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
		int newStorage = (int)(this.data.length * 1.5) + 1;

		if (newStorage < minStorage) newStorage = minStorage;

		if (this.data != null)
		{
			RowData newBuf[] = new RowData[newStorage];
			System.arraycopy(this.data, 0, newBuf, 0, this.size);
			this.data = newBuf;
		}
	}

	public void ensureCapacity(int size)
	{
		this.grow(size);
	}
	/**
	 * 	Free all objects stored in the internal array.
	 * 	This does not shrink the array itself.
	 */
	public void clear()
	{
		for (int i=0; i < size; i++)
		{
			data[i] = null;
		}
		this.size = 0;
	}

	public void reset()
	{
		for (int i=0; i < size; i++)
		{
			if (data[i] != null) data[i].reset();
			data[i] = null;
		}
		this.size = 0;
		this.data = new RowData[DEFAULT_SIZE];
	}

	public int size()
	{
		return this.size;
	}


	public RowData get(int index)
	{
		return this.data[index];
	}

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

	public int add(RowData row)
	{
		int newlen = this.size + 1;
		if (newlen > this.data.length) grow(newlen);
		this.data[newlen - 1] = row;
		this.size = newlen;
		return this.size;
	}

	public int add(int index, RowData row)
	{
		int newlen = this.size + 1;
		if (newlen > this.data.length)
		{
			// we are not using moreStorage here to optimize
			// the calls to System.arraycopy
			RowData newBuf[] = new RowData[newlen + 10];
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