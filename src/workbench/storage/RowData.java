/*
 * RowData.java
 *
 * Created on 15. September 2001, 16:03
 */
package workbench.storage;

class RowData
{
	public static final int NOT_MODIFIED = 0;
	public static final int MODIFIED = 1;
	public static final int NEW = 2;
	
	private int status = NOT_MODIFIED;
	
	private Object[] colData;
	private Object[] originalData;
	
	/** Creates new RowData */
	public RowData(int aColCount)
	{
		this.colData = new Object[aColCount];
	}

	/**
	 *	Sets the new data for the given column.
	 *	After a call isModified() will return true
	 *	if the row was not modified before. If the
	 *	row is new the status will not change.
	 *
	 *	@throws IndexOutOfBoundsException
	 */
	public void setValue(int aColIndex, Object aValue)
		throws IndexOutOfBoundsException
	{
		if (this.originalData == null)
		{
			this.originalData = new Object[this.colData.length];
		}
		if (this.originalData[aColIndex] == null)
		{
			this.originalData[aColIndex] = this.colData[aColIndex];
		}
		this.colData[aColIndex] = aValue;
		this.setModified();
	}
	
	/**
	 *	Returns the value for the given column
	 *
	 *	@throws IndexOutOfBoundsException
	 */
	public Object getValue(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.colData[aColumn];
	}

	/**	
	 *	Resets the internal status. After a call to resetStatus()
	 *	isModified() will return false, and isOriginal() will return true.
	 */
	public void resetStatus()
	{
		this.status = NOT_MODIFIED;
	}

	/**
	 *	Sets the status of this row to new.
	 */
	public void setNew()
	{
		this.status = NEW;
	}

	/**
	 *	Returns true if the row is neither modified nor is a new
	 *	row. Is the same as isNew() || isModified()
	 *
	 *	@returns true if the row has not been altered since retrieval
	 */
	public boolean isOriginal()
	{
		return this.status == NOT_MODIFIED;
	}
	
	/**
	 *	Check if the row has been modified.
	 *
	 *	@return true if the row has been modified since retrieval
	 *
	 */
	public boolean isModified()
	{
		return this.status == MODIFIED;
	}

	/**
	 *	Check if the row has been added to the DataStore
	 *	after the initial retrieve.
	 *
	 *	@return true if it's a new row
	 */
	public boolean isNew()
	{
		return this.status == NEW;
	}

	/**
	 *	Set the status to modified. 
	 *	The status will not be set to modified for new rows!
	 */
	public void setModified()
	{
		// The status will only be set to modified
		// if the row has not been modified and is not a new row
		if (this.status == NOT_MODIFIED)
		{
			this.status = MODIFIED;
		}
	}

}