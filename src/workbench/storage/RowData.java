/*
 * RowData.java
 *
 * Created on 15. September 2001, 16:03
 */
package workbench.storage;

class RowData
{
	
	private Object[] colData;
	
	/** Creates new RowData */
	public RowData(int aColCount)
	{
		this.createData(aColCount);
	}
	
	public void setValue(int aColIndex, Object aValue)
	{
		this.colData[aColIndex] = aValue;
	}
	
	private void createData(int aSize)
	{
		this.colData = new Object[aSize];
	}
	
	public Object getValue(int aColumn)
		throws IndexOutOfBoundsException
	{
		return this.colData[aColumn];
	}
	
}