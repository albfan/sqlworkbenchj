/*
 * ArgumentParserTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import junit.framework.*;


/**
 *
 * @author thomas
 */
public class ArgumentParserTest
	extends TestCase
{
	public ArgumentParserTest(String testname)
	{
		super(testname);
	}
	
	public void testParser()
	{
		String cmdline = "-profile='test-prof' -script=bla.sql";
		ArgumentParser arg = new ArgumentParser();
		arg.addArgument("profile");
		arg.addArgument("script");
		arg.parse(cmdline);
		assertEquals("profile not retrieved", "test-prof", arg.getValue("profile"));
		assertEquals("script not retrieved", "bla.sql", arg.getValue("script"));
	}
}
