/*
 * WbTestCase.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.io.IOException;
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
		System.setProperty("workbench.log.console", "false");
	}

	protected TestUtil getTestUtil()
	{
		TestUtil util = new TestUtil(getName());
		try
		{
			util.prepareEnvironment();
		}
		catch (IOException io)
		{
			io.printStackTrace();
		}
		return util;
	}

	protected TestUtil getTestUtil(String method)
	{
		return new TestUtil(getName() + "_" + "method");
	}
}
