/*
 * ProfileDefinition.java
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

/**
 * @author support@sql-workbench.net
 */
public class ProfileKey
{
	private String name; // the name of the profile
	private String group; // the profile group
	
	public ProfileKey(String pname)
	{
		if (pname != null) name = pname.trim();
	}
	
	public ProfileKey(String pname, String pgroup)
	{
		if (pname != null) name = pname.trim();
		if (pgroup != null) group = pgroup.trim();
	}
	
	public String getName() { return name; }
	public String getGroup() { return group; }
	
	public String toString() 
	{
		if (group == null) return name;
		return "{" + group + "}/" + name;
	}
}
