package workbench.storage;

import java.sql.Types;
import java.util.HashMap;

public class NullValue
{
	private static HashMap valueCache = new HashMap();
	private int type;
	private static final String EMPTY_STRING = "";
	
	public static NullValue getInstance(int aType)
	{
		Integer key = new Integer(aType);
		NullValue val = (NullValue)valueCache.get(key);
		if (val == null)
		{
			val = new NullValue(aType);
			valueCache.put(key, val);
		}
		return val;
	}
	
	private NullValue(int aType)
	{
		this.type = aType;
	}
	public int getType() { return this.type; }
	public String toString() { return EMPTY_STRING; }
}
	

