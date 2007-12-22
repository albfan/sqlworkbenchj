/*
 * TableGrantTest.java
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

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author support@sql-workbench.net
 */
public class TableGrantTest extends junit.framework.TestCase
{
	
	public TableGrantTest(String testName)
	{
		super(testName);
	}
	
	public void testCompareTo()
	{
		TableGrant g1 = new TableGrant("testuser", "DELETE", false);
		TableGrant g2 = new TableGrant("testuser", "DELETE", false);
		assertEquals("incorrect compareTo for equals objects", 0, g1.compareTo(g2));
		
		g1 = new TableGrant("testuser", "DELETE", true);
		g2 = new TableGrant("testuser", "DELETE", false);
		assertEquals("incorrect compareTo for equals objects", 1, g1.compareTo(g2));
		
	}
	
	public void testEquals()
	{
		TableGrant g1 = new TableGrant("testuser", "DELETE", false);
		TableGrant g2 = new TableGrant("testuser", "DELETE", false);
		
		assertEquals("incorrect equals for equals objects", true, g1.equals(g2));
		
		g1 = new TableGrant("testuser", "DELETE", true);
		g2 = new TableGrant("testuser", "DELETE", false);
		
		assertEquals("incorrect equals for equals objects", false, g1.equals(g2));

		g1 = new TableGrant("someuser", "DELETE", false);
		g2 = new TableGrant("testuser", "DELETE", false);
		
		assertEquals("incorrect equals for equals objects", false, g1.equals(g2));

		g1 = new TableGrant("testuser", "INSERT", false);
		g2 = new TableGrant("testuser", "DELETE", false);
		
		assertEquals("incorrect equals for equals objects", false, g1.equals(g2));
		
		Set<TableGrant> grants = new HashSet<TableGrant>();
		g1 = new TableGrant("testuser", "DELETE", true);
		g2 = new TableGrant("testuser", "DELETE", false);
		grants.add(g1);
		grants.add(g2);
		assertEquals("Not all grants added", 2, grants.size());
		
		// This should not be added as it is equal to g2
		grants.add(new TableGrant("testuser", "DELETE", false));
		assertEquals("Not all grants added", 2, grants.size());
	}
	
	
}
