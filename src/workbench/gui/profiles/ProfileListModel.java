/*
 * ProfileListModel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractListModel;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
class ProfileListModel 
	extends AbstractListModel
{
	private List filtered;
	private ArrayList profiles;
	private boolean changed = false;
	
	/** Creates a new instance of ProfileListModel */
	public ProfileListModel(Map aProfileList)
	{
		this.profiles = new ArrayList();
		
		if (aProfileList != null)
		{
			this.profiles.addAll(aProfileList.values());
		}
		sortList();
	}

	private void sortList()
	{
		Collections.sort(this.profiles, ConnectionProfile.getNameComparator());
	}
	
	/**
	 * Only show profiles belonging to the passed group.
	 * If group == null, all profiles will be shown
	 * @see workbench.db.ConnectionProfile#getGroup()
	 * @see workbench.db.ConnectionMgr.getProfileGroups()
	 */
	public void setGroupFilter(String group)
	{
		if (this.filtered != null)
		{
			this.profiles.addAll(filtered);
			this.filtered.clear();
		}
		else
		{
			filtered = new LinkedList();
		}
		
		if (!StringUtil.isEmptyString(group))
		{
			int i=0; 
			while (i < profiles.size())
			{
				ConnectionProfile prof = (ConnectionProfile)profiles.get(i);
				if (!group.equals(prof.getGroup()))
				{
					profiles.remove(i);
					filtered.add(prof);
				}
				else
				{
					i++;
				}
			}
		}
		sortList();
		this.fireContentsChanged(this, 0, profiles.size());
	}
	
	/** Returns the value at the specified index.
	 * @param index the requested index
	 * @return the value at <code>index</code>
	 *
	 */
	public Object getElementAt(int index)
	{
		return this.profiles.get(index);
	}

	/**
	 * Returns the length of the list.
	 * @return the length of the list
	 *
	 */
	public int getSize()
	{
		return this.profiles.size();
	}
	
	public void profileChanged(ConnectionProfile aProfile)
	{
		int index = this.profiles.indexOf(aProfile);
		if (index >= 0)
		{
			this.fireContentsChanged(this, index, index);
		}
	}

	public void addProfile(ConnectionProfile aProfile)
	{
		ConnectionMgr conn = ConnectionMgr.getInstance();
		conn.addProfile(aProfile);
		this.profiles.add(this.profiles.size(), aProfile);
		this.fireIntervalAdded(this, this.profiles.size() - 1,  this.profiles.size() - 1);
		this.changed = true;
	}

	public void deleteProfile(int index)
	{
		ConnectionMgr conn = ConnectionMgr.getInstance();
		ConnectionProfile profile = (ConnectionProfile)this.profiles.get(index);
		conn.removeProfile(profile);
		this.profiles.remove(index);
		this.fireIntervalRemoved(this, index, index);
		this.changed = true;
	}

	public boolean isChanged()
	{
		if (changed) return true;
		for (int i=0; i < this.profiles.size(); i++)
		{
			ConnectionProfile profile = (ConnectionProfile)this.profiles.get(i);
			if (profile.isChanged()) return true;
		}
		return false;
	}
	public Collection getValues()
	{
		return this.profiles;
	}
	
	
}

