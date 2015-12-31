/*
 * ProfileKey.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.profiles;

/**
 * A class to uniquely identify a {@link workbench.db.ConnectionProfile}
 *
 * @author Thomas Kellerer
 */
public class ProfileKey
{
	private String name; // the name of the profile
	private String group; // the profile group

	/**
	 * Create a new ProfileKey.
	 * The passed name can consist of the profile group and the profile name
	 * the group needs to be enclosed in curly brackets, e.g:
	 * <tt>{MainGroup}/HR Database</tt><br/>
	 * The divividing slash is optional.
	 *
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
		if (pname == null) throw new NullPointerException("Name cannot be null!");

		if (pgroup != null)
		{
			this.group = pgroup.trim();
			this.name = pname.trim();
		}
		else
		{
			setName(pname);
		}
	}

	private void setName(String pname)
	{
		if (pname == null) return;

		String tname = pname.trim();
		if (tname.length() > 0 && tname.charAt(0) == '{')
		{
			int pos = tname.indexOf('}');
			if (pos < 0) throw new IllegalArgumentException("Missing closing } to define group name");
			int slashPos = tname.indexOf('/', pos + 1);
			if (slashPos < 0) slashPos = pos;
			this.name = tname.substring(slashPos + 1).trim();
			this.group = tname.substring(1,pos).trim();
		}
		else if (tname.length() > 0 && tname.indexOf('/') > -1)
		{
			int slashPos = tname.indexOf('/');
			this.name = tname.substring(slashPos + 1).trim();
			this.group = tname.substring(0,slashPos).trim();
		}
		else
		{
			name = tname;
		}
	}

	public String getName()
	{
		return name;
	}

	public String getGroup()
	{
		return group;
	}

	@Override
	public String toString()
	{
		if (group == null) return name;
		return "{" + group + "}/" + name;
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object other)
	{
		if (other == null) return false;
		if (other instanceof ProfileKey)
		{
			ProfileKey key = (ProfileKey)other;
			if (key.getName() == null) return false;
			if (this.name.equals(key.getName()))
			{
				if (key.getGroup() == null || this.group == null) return true;
				return this.getGroup().equals(key.getGroup());
			}
		}
		return false;
	}

}
