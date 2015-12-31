/*
 * ProfileGroupMap.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 *
 * @author Thomas Kellerer
 */
public class ProfileGroupMap
	extends TreeMap<String, List<ConnectionProfile>>
{
	public ProfileGroupMap(List<ConnectionProfile> profiles)
	{
		super();

		// If the complete list is sorted by name at the beginning
		// the sublists per group will be sorted automatically.
		Collections.sort(profiles, ConnectionProfile.getNameComparator());

		for (ConnectionProfile profile : profiles)
		{
			String group = profile.getGroup();
			List<ConnectionProfile> l = get(group);
			if (l == null)
			{
				l = new ArrayList<>();
				put(group, l);
			}
			l.add(profile);
		}
	}
}
