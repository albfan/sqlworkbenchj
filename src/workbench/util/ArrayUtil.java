/*
 * ArrayUtil.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author support@sql-workbench.net
 */
public class ArrayUtil
{

	public static <T> List<T> arrayToList(T[] a) 
	{ 
		List<T> l = new LinkedList<T>();
		for (T o : a) 
		{ 
			l.add(o);
		}
		return l;
	}	

	public static <T> void arrayToCollection(T[] a, Collection<T> c) 
	{ 
		for (T o : a) 
		{ 
			c.add(o);
		}
	}	
}
