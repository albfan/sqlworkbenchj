/*
 * DependencyNodeTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class DependencyNodeTest
	extends TestCase
{
	public DependencyNodeTest(String testName)
	{
		super(testName);
	}

	public void testGetLevel()
	{
		TableIdentifier root = new TableIdentifier("root");
		DependencyNode rootNode = new DependencyNode(root);
		assertEquals("Wrong level for root", 0, rootNode.getLevel());
		
		TableIdentifier child1 = new TableIdentifier("child1");
		DependencyNode cnode = rootNode.addChild(child1, "test");
		assertEquals("Wrong level for first child", 1, cnode.getLevel());

		TableIdentifier child2 = new TableIdentifier("child2");
		DependencyNode cnode2 = rootNode.addChild(child2, "test_2");
		assertEquals("Wrong level for second child", 1, cnode2.getLevel());

		TableIdentifier child3 = new TableIdentifier("child3");
		DependencyNode cnode3 = cnode2.addChild(child3, "test_3");
		assertEquals("Wrong level for third child", 2, cnode3.getLevel());
		
		DependencyNode f = rootNode.findNode(cnode3);
		assertNotNull(f);
		assertEquals(f, cnode3);
	}
	
}
