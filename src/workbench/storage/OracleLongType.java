/*
 * LongType.java
 *
 * Created on July 6, 2004, 2:12 PM
 */

package workbench.storage;

/**
 * Class to Wrap Oracle's LONG datatype
 * @author  workbench@kellerer.org
 */
class OracleLongType
{
	private final String value;
	
	public OracleLongType(String aValue)
	{
		this.value = aValue;
	}
	
	public String toString() { return this.value; }
	public String getValue() { return this.value; }
	
	public int getLength()
	{
		if (this.value == null) return 0;
		return this.value.length();
	}
}
