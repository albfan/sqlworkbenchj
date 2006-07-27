/*
 * GroupNode.java
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

import workbench.resource.ResourceMgr;

/**
 * @author support@sql-workbench.net
 */
public class GroupNode
{
	private boolean isDefaultGroup = false;
	private String title;
	public static final String DEFAULT_GROUP_MARKER = "$wb$_default_group_$wb$";
	public static final GroupNode DEFAULT_GROUP = new GroupNode(null, true);
	
	public static GroupNode createGroupNode(String title)
	{
		if (title == null) return DEFAULT_GROUP;
		return new GroupNode(title, false);
	}
	
	private GroupNode(String groupTitle, boolean defGroup)
	{
		if (defGroup || groupTitle == null)
		{
			this.title = ResourceMgr.getString("LblDefGroup");
			this.isDefaultGroup = true;
		}
		else
		{
			this.title = groupTitle;
			this.isDefaultGroup = false;
		}
	}
	
	public String toString()
	{
		return this.title;
	}
	
	public String getGroup()
	{
		if (this.isDefaultGroup) return null;
		return title;
	}
	
	public boolean isDefaultGroup()
	{
		return isDefaultGroup;
	}
}

