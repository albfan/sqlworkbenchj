/*
 * RowData.java
 *
 * Created on 15. September 2001, 16:03
 */
package workbench.storage;

import workbench.db.WbConnection;

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
		this.setNew();
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
		if (aValue == null) throw new NullPointerException("No null values allowed. Use setNull() instead");
		if (!this.isNew())
		{
			Object oldValue = this.colData[aColIndex];
			if (oldValue == null && aValue == null) return;
			if (oldValue != null && aValue != null)
			{
				if (oldValue.equals(aValue)) return;
			}
			if (this.originalData == null)
			{
				this.originalData = new Object[this.colData.length];
			}
			if (this.originalData[aColIndex] == null)
			{
				this.originalData[aColIndex] = this.colData[aColIndex];
			}
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

	public Object getOriginalValue(int aColumn)
		throws IndexOutOfBoundsException
	{
		if (this.originalData == null) return this.getValue(aColumn);
		if (this.originalData[aColumn] == null) return this.getValue(aColumn);
		return this.originalData[aColumn];
	}
	
	public void restoreOriginalValues()
	{
		if (this.originalData == null) return;
		for (int i=0; i < this.originalData.length; i++)
		{
			if (this.originalData[i] != null)
			{
				this.colData[i] = this.originalData[i];
			}
		}
		this.originalData = null;
		this.resetStatus();
	}
	
	public boolean isColumnModified(int aColumn)
	{
		if (this.isOriginal()) return false;
		if (this.isNew())
		{
			return this.colData[aColumn] != null;
		}
		else
		{
			if (this.originalData == null) return false;
			return (this.originalData[aColumn] != null);
		}
	}
	public void setNull(int aColumn, int aType)
	{
		NullValue nul = NullValue.getInstance(aType);
		this.setValue(aColumn, nul);
	}
	
	/**	
	 *	Resets the internal status. After a call to resetStatus()
	 *	isModified() will return false, and isOriginal() will return true.
	 */
	public void resetStatus()
	{
		this.status = NOT_MODIFIED;
		this.originalData = null;
	}

	/**
	 *	Sets the status of this row to new.
	 */
	public void setNew()
	{
		this.status = NEW;
	}

	/**
	 *	Returns true if the row is neither modified nor is a new row. 
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
		return (this.status & MODIFIED) ==  MODIFIED;
	}

	/**
	 *	Check if the row has been added to the DataStore
	 *	after the initial retrieve.
	 *
	 *	@return true if it's a new row
	 */
	public boolean isNew()
	{
		return (this.status & NEW) == NEW;
	}

	/**
	 *	Set the status to modified. 
	 */
	public void setModified()
	{
		this.status = this.status | MODIFIED;
	}

}