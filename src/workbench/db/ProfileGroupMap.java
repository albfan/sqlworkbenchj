/*
 * ProfileGroupMap.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 *
 * @author support@sql-workbench.net
 */
public class ProfileGroupMap
	extends TreeMap<String, List<ConnectionProfile>>
{
	public ProfileGroupMap()
	{
		super();
		ArrayList<ConnectionProfile> profiles = new ArrayList<ConnectionProfile>(ConnectionMgr.getInstance().getProfiles());

		// If the complete list is sorted by name at the beginning
		// the sublists per group will be sorted automatically.
		Collections.sort(profiles, ConnectionProfile.getNameComparator());

		for (ConnectionProfile profile : profiles)
		{
			String group = profile.getGroup();
			List<ConnectionProfile> l = get(group);
			if (l == null)
			{
				l = new ArrayList<ConnectionProfile>();
				put(group, l);
			}
			l.add(profile);
		}
	}
}
