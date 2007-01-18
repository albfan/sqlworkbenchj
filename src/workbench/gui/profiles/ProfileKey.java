/*
 * ProfileKey.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

/**
 * @author support@sql-workbench.net
 */
public class ProfileKey
{
	private String name; // the name of the profile
	private String group; // the profile group

	public ProfileKey(String pname)
	{
		if (pname == null) throw new NullPointerException("Name cannot be null!");
		setName(pname);
	}
	
	public ProfileKey(String pname, String pgroup)
	{
		if (pgroup != null) group = pgroup.trim();
		setName(pname);
	}

	private void setName(String pname)
	{
		String tname = pname.trim();
		if (tname.charAt(0) == '{')
		{
			int pos = tname.indexOf('}');
			this.name = tname.substring(pos + 2).trim();
			this.group = tname.substring(1,pos).trim();
		}
		else
		{
			name = tname;
		}
	}
	
	public String getName() { return name; }
	public String getGroup() { return group; }
	
	public String toString() 
	{
		if (group == null) return name;
		return "{" + group + "}/" + name;
	}
}
