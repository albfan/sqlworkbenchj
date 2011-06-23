/*
 * ArgumentValue
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.util;

import java.util.Comparator;

/**
 *
 * @author Thomas Kellerer
 */
public interface ArgumentValue
{
	String getDisplay();
	String getValue();

	public Comparator<ArgumentValue> COMPARATOR = new Comparator<ArgumentValue>()
	{
		@Override
		public int compare(ArgumentValue o1, ArgumentValue o2)
		{
			return o1.getValue().compareToIgnoreCase(o2.getValue());
		}
	};
}
