/*
 * WbTestCase.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench;

import java.io.IOException;

/**
 * @author Thomas Kellerer
 */
public class WbTestCase
{
	private String name;
	private boolean prepared;
	
	public WbTestCase()
	{
		name = "WbTestCase";
		prepared = false;
	}
	
	public WbTestCase(String testName)
	{
		name = testName;
		prepare();
	}

	protected void prepare()
	{
		System.setProperty("workbench.log.console", "false");
		getTestUtil();
	}
	
	protected synchronized TestUtil getTestUtil()
	{
		TestUtil util = new TestUtil(getName());
		if (prepared) return util;
		
		try
		{
			util.prepareEnvironment();
			prepared = true;
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

	public String getName()
	{
		return name;
	}
}
