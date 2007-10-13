/*
 * WbTestCase.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import junit.framework.TestCase;

/**
 * @author support@sql-workbench.net
 */
public class WbTestCase
	extends TestCase
{
	public WbTestCase(String testName)
	{
		super(testName);
	}

	protected TestUtil getTestUtil()
	{
		TestUtil util = new TestUtil(getName());
		return util;
	}
	
	protected TestUtil getTestUtil(String method)
	{
		return new TestUtil(getName() + "_" + "method");
	}
}
