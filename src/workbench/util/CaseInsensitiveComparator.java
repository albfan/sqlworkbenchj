/*
 * CaseInsensitiveComparator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.Comparator;

/**
 * A case-insensitive Comparator for String which 
 * can handle null values as well (as opposed to String.CASE_INSENSITIVE_ORDER)
 * 
 * @author support@sql-workbench.net
 */
public class CaseInsensitiveComparator
	implements Comparator<String>
{
	/**
	 * Compares to two strings.
	 * null values are "sorted" after non-null values.
	 * i.e. compare(null, "something") returns -1
	 * and compare("something", null) returns 1
	 * 
	 * @param value1 the first String, maybe null
	 * @param value2 the second String, maybe null
	 * @return 0 if both are null or compareToIgnoreCase() returns 0
	 * @see workbench.util.StringUtil#compareStrings(String, String, boolean)
	 */
	public int compare(String value1, String value2)
	{
		return StringUtil.compareStrings(value1, value2, true);
	}
}


