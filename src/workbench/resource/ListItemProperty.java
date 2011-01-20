/*
 * ListeItemProperty
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
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
