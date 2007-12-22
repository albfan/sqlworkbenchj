/*
 * SequenceDefinitionTest.java
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

import junit.framework.TestCase;

/**
 * @author support@sql-workbench.net
 */
public class SequenceDefinitionTest
	extends TestCase
{
	public SequenceDefinitionTest(String testName)
	{
		super(testName);
	}

	public void testEquals()
	{
		SequenceDefinition def1 = new SequenceDefinition("public", "seq_one");
		def1.setSequenceProperty("INCREMENT", new Integer(1));
		SequenceDefinition def2 = new SequenceDefinition("public", "seq_two");
		def2.setSequenceProperty("INCREMENT", new Integer(1));
		assertEquals(def1.equals(def2), true);
		
		def2.setSequenceProperty("CACHE", new Integer(50));
		assertEquals(def1.equals(def2), false);
	}
}
