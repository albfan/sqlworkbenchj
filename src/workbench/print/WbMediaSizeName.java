/*
 * WbMediaSizeName.java
 *
 * Created on July 27, 2003, 4:35 PM
 */

package workbench.print;

import javax.print.attribute.EnumSyntax;
import javax.print.attribute.standard.MediaSizeName;

/**
 *
 * @author  thomas
 */
public class WbMediaSizeName 
	extends MediaSizeName
{
	
	public WbMediaSizeName(int aValue)
	{
		super(aValue);
	}
	

	public String[] getStringTable()
	{
		return super.getStringTable();
	}
	
	public EnumSyntax[] getEnumValueTable() 
	{
		return super.getEnumValueTable();
	}
	
	public static String getName(MediaSizeName aMedia)
	{
		int value = aMedia.getValue();
		WbMediaSizeName dummy = new WbMediaSizeName(0);
		String[] names = dummy.getStringTable();
		if (value >= 0 && value < names.length)
		{
			return names[value];
		}
		return null;
	}
	
	public static MediaSizeName getMediaSize(String aName)
	{
		WbMediaSizeName dummy = new WbMediaSizeName(0);
		//String[] names = dummy.getStringTable();
		EnumSyntax[] values = dummy.getEnumValueTable();
		
		for (int i=0; i < values.length; i++)
		{
			MediaSizeName entry = (MediaSizeName)values[i];
			if (entry.getName().equals(aName))
			{
				return entry;
			}
		}
		return null;
	}
}
