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
 * A class to uniquely identify a {@link workbench.db.ConnectionProfile}
 * 
 * @author support@sql-workbench.net
 */
public class ProfileKey
{
	private String name; // the name of the profile
	private String group; // the profile group

	/**
	 * Create a new ProfileKey.
	 * The passed name can consist of the profile group and the profile name
	 * the group needs to be enclosed in curly brackets, e.g: 
	 * <tt>{MainGroup}/HR Database</tt>
	 * @param pname the name (can include the profile group) of the profile
	 */
	public ProfileKey(String pname)
	{
		if (pname == null) throw new NullPointerException("Name cannot be null!");
		setName(pname);
	}
	
	/**
	 * Create a new key based on a profile name and a group name.
	 * 
	 * @param pname the name of the profile
	 * @param pgroup the group to which the profile belongs
	 */
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
