/*
 * CellData.java
 *
 * Created on 15. September 2001, 14:09
 */

package workbench.storage;

class CellData
{
	public static final String NULL_DISPLAY = "(null)";
	private Object currentData = null;
	private Object orgData = null;
	private String tableName = null;
	private String columnName = null;
	private boolean modified;
	
	public CellData()
	{
		this(null);
	}
	
	public CellData(Object aValue)
	{
		this.currentData = aValue;
		this.orgData = aValue;
	}
	
	public Object getValue() { return this.currentData; }
	
	public void setValue(Object aNewValue)
	{
		if (this.currentData != null)
		{
			if (!this.currentData.equals(aNewValue))
			{
				this.currentData = aNewValue;
				this.modified = true;
			}
		}
	}
	
	public boolean isUpdateable()
	{
		return (this.tableName != null && this.columnName != null);
	}
	
	protected void setModified(boolean aFlag) { this.modified = aFlag; }
	public boolean isModified() { return this.modified; }
	
	/**
	 *  Resets the object to its original state.
	 *  Reset the value to the one initially provided (usually the
	 *  value loaded from the database) and clear the modified flag
	 */
	public void reset()
	{
		this.modified = true;
		this.currentData = this.orgData;
	}
	
	public String getDisplayString()
	{
		if (this.currentData == null)
		{
			return NULL_DISPLAY;
		}
		else
		{
			return this.currentData.toString();
		}
	}
}