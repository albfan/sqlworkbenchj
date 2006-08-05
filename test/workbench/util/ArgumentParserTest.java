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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


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
		String cmdline = "  -otherbool=1 -nosettings -boolarg=true -profile='test-prof' -script=bla.sql -arg2=\"with space and quote\"";
		ArgumentParser arg = new ArgumentParser();
		arg.addArgument("profile");
		arg.addArgument("script");
		arg.addArgument("arg2");
		arg.addArgument("nosettings");
		arg.addArgument("boolarg");
		arg.addArgument("otherbool");
		arg.parse(cmdline);
		assertEquals("profile not retrieved", "test-prof", arg.getValue("profile"));
		assertEquals("script not retrieved", "bla.sql", arg.getValue("script"));
		assertEquals("double quoted value not retrieved", "with space and quote", arg.getValue("arg2"));
		assertEquals("argument without parameter not found", true, arg.isArgPresent("nosettings"));
		assertEquals("boolean argument not retrieved", true, arg.getBoolean("boolarg", false));
		assertEquals("numeric boolean argument not retrieved", true, arg.getBoolean("otherbool", false));
		
	}

}
