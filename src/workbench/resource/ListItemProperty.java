/*
 * ListItemProperty.java
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
package workbench.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import workbench.util.StringUtil;

/**
 * A class to store and retrieve multiple items from the configuration file that
 * are stored as numbered properties (e.g. workbench.some.item.1, workbench.some.item.2 and so on)
 *
 * @author Thomas Kellerer
 */
public class ListItemProperty
{
	private String propName;

	public ListItemProperty(String itemName)
	{
		this.propName = itemName;
	}

	private int getItemCount()
	{
		return Settings.getInstance().getIntProperty("workbench." + propName + ".count", 0);
	}

	public List<String> getItems()
	{
		Settings s = Settings.getInstance();

		int itemCount = getItemCount();
		List<String> result = new ArrayList<String>(itemCount);

		for (int i = 0; i < itemCount; i++)
		{
			String item = s.getProperty("workbench." + propName + "." + i, null);
			if (StringUtil.isNonBlank(item))
			{
				result.add(item);
			}
		}

		return result;
	}

	private void clearList()
	{
		int itemCount = getItemCount();
		for (int i=0; i < itemCount; i++)
		{
			Settings.getInstance().removeProperty("workbench." + propName + "." + i);
		}
	}

	/**
	 * Saves the items to the configuration file.
	 * <br/>
	 * For each item in the list, the toString() method is called, and the value
	 * of that is stored as the configuration value.
	 * 
	 * @param items
	 */
	public void storeItems(Collection<? extends Object> items)
	{
		clearList();
		int count = 0;
		for (Object item : items)
		{
			Settings.getInstance().setProperty("workbench." + propName + "." + count, item.toString());
			count ++;
		}
		Settings.getInstance().setProperty("workbench." + propName + ".count", count);
	}

}
