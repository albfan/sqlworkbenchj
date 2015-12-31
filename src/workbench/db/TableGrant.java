/*
 * TableGrant.java
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
package workbench.db;

import workbench.util.StringUtil;

/**
 * @author Thomas Kellerer
 */
public class TableGrant
{
	private String grantee;
	private String privilege;
	private boolean grantable;
	private int hashCode = 0;

	/**
	 * Create a new TableGrant.
	 * @param to which user received the grant. May not be null.
	 * @param what the privilege that was granted to the user <tt>to</tt>. May not be null.
	 * @param grantToOthers whether the user may grant the privilege to other users
	 */
	public TableGrant(String to, String what, boolean grantToOthers)
	{
		this.grantee = to;
		this.privilege = what;
		this.grantable = grantToOthers;

		StringBuilder b = new StringBuilder(30);
		b.append(grantee);
		b.append(privilege);
		b.append(grantable);
		hashCode = b.toString().hashCode();
	}

	@Override
	public int hashCode()
	{
		return hashCode;
	}

	public int compareTo(Object other)
	{
		if (this.equals(other)) return 0;

		try
		{
			TableGrant otherGrant = (TableGrant)other;
			int c1 = grantee.compareToIgnoreCase(otherGrant.grantee);
			int c2 = privilege.compareToIgnoreCase(otherGrant.privilege);
			if (c1 == 0)
			{
				if (c2 == 0)
				{
					if (grantable && !otherGrant.grantable) return 1;
					return -1;
				}
				else
				{
					return c2;
				}
			}
			else
			{
				return c1;
			}
		}
		catch (ClassCastException e)
		{
			return -1;
		}
	}

	@Override
	public boolean equals(Object other)
	{
		try
		{
			TableGrant otherGrant = (TableGrant)other;
			return StringUtil.equalStringIgnoreCase(grantee, otherGrant.grantee) &&
						 StringUtil.equalStringIgnoreCase(privilege, otherGrant.privilege) &&
						 grantable == otherGrant.grantable;
		}
		catch (ClassCastException e)
		{
			return false;
		}
	}

	@Override
	public String toString()
	{
		return "GRANT " + privilege + " TO " + grantee;
	}

	public String getGrantee() { return grantee; }
	public String getPrivilege() { return privilege; }
	public boolean isGrantable() { return grantable; }
}
