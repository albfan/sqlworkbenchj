/*
 * AbstractValueObject.java
 *
 * Created on 1. November 2002, 00:15
 */
package workbench.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BaseValueObject
{
	private HashMap data;

	public BaseValueObject()
	{
		this.data = new HashMap();
	}

	public BaseValueObject(int colCount)
	{
		this.data = new HashMap(colCount);
	}

	public Object getColumnValue(String aCol)
	{
		return this.data.get(aCol);
	}

	public void setColumnValue(String aCol, Object aValue)
	{
		if (this.data.containsKey(aCol))
		{
			this.data.put(aCol, aValue);
		}
	}

	public int getColumnCount()
	{
		return this.data.size();
	}

	public Iterator getColumns()
	{
		return this.data.keySet().iterator();
	}

	protected void removeColumn(String aCol)
	{
		this.data.remove(aCol);
	}

	protected void addColumn(String aCol)
	{
		this.data.put(aCol, null);
	}

	public String toString()
	{
		if (data == null) return "(n/a)";
		Iterator itr = this.data.entrySet().iterator();
		StringBuffer result = new StringBuffer(250);
		boolean first = true;
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			String col = (String)entry.getKey();
			Object value = entry.getValue();
			if (value == null) value = "(null)";
			if (!first) result.append(",");
			else first = false;
			result.append('[');
			result.append(col);
			result.append('=');
			result.append(value.toString());
			result.append(']');
		}
		return result.toString();
	}

	public BaseValueObject createCopy()
	{
		return this.createCopy(true);
	}

	public BaseValueObject createCopy(boolean includeData)
	{
		BaseValueObject result = new BaseValueObject();
		result.data = new HashMap();
		if (includeData)
		{
			result.data.putAll(this.data);
		}
		else
		{
			Iterator itr = this.data.keySet().iterator();
			while (itr.hasNext())
			{
				result.data.put(itr.next(), null);
			}
		}
		return result;
	}
}
