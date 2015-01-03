/*
 * WbMediaSizeName.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.print;

import javax.print.attribute.EnumSyntax;
import javax.print.attribute.standard.MediaSizeName;

/**
 *
 * @author Thomas Kellerer
 */
public class WbMediaSizeName 
	extends MediaSizeName
{
	
	public WbMediaSizeName(int aValue)
	{
		super(aValue);
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
