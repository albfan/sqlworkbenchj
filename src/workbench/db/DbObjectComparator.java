/*
 * DbObjectComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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

import java.util.Comparator;

/**
 *
 * @author Thomas Kellerer
 */
public class DbObjectComparator
	implements Comparator<DbObject>
{

	@Override
	public int compare(DbObject o1, DbObject o2)
	{
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		if (o2 == null) return 1;

		String n1 = o1.getFullyQualifiedName(null);
		String n2 = o2.getFullyQualifiedName(null);
		if (n1.startsWith("\"") || n2.startsWith("\""))
		{
			return n1.compareTo(n2);
		}
		return n1.compareToIgnoreCase(n2);
	}

}
