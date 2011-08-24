/*
 * ArgumentParserTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import workbench.TestUtil;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class ArgumentParserTest
	extends WbTestCase
{
	public ArgumentParserTest()
	{
		super("ArgumentParserTest");
	}

	@Test
	public void testPropFile()
		throws Exception
	{
		ArgumentParser arg = new ArgumentParser();
		arg.addArgument("prop1");
		arg.addArgument("otherprop");
		TestUtil util = getTestUtil();
		File f = new File(util.getBaseDir(), "props.txt");
		TestUtil.writeFile(f, "prop1 = value1\notherprop = 5", "ISO-8859-1");
		arg.parseProperties(f);
		assertEquals("value1", arg.getValue("prop1"));
		assertEquals("5", arg.getValue("otherprop"));
		f.delete();
	}

	@Test
	public void testAllowedValues()
	{
		ArgumentParser arg = new ArgumentParser();
		arg.addArgument("type", StringUtil.stringToList("text,xml,sql"));
		Collection<ArgumentValue> c = arg.getAllowedValues("type");
		assertEquals(3, c.size());

		assertTrue(c.contains(new StringArgumentValue("text")));
		assertTrue(c.contains(new StringArgumentValue("TEXT")));
		assertTrue(c.contains(new StringArgumentValue("TeXt")));

		assertTrue(arg.isAllowedValue("type", "Text"));
		assertTrue(arg.isAllowedValue("type", "TEXT"));
		assertTrue(arg.isAllowedValue("type", "text"));
	}

	@Test
	public void testRepeatableArgs()
	{
		ArgumentParser parser = new ArgumentParser();
		parser.addArgument("constant", ArgumentType.Repeatable);
		String cmdLine = "-constant=1 -constant=2";
		parser.parse(cmdLine);
		List<String> constants = parser.getList("constant");
		assertNotNull(constants);
		assertEquals(2, constants.size());

		cmdLine = "-constant=1,2";
		parser.parse(cmdLine);
		constants = parser.getList("constant");
		assertNotNull(constants);
		assertEquals(2, constants.size());

		cmdLine = "-constant='somelist=1,2' -constant='otherlist=3,4'";
		parser.parse(cmdLine);
		constants = parser.getList("constant");
		assertNotNull(constants);
		assertEquals(2, constants.size());
		assertEquals("somelist=1,2", constants.get(0));
		assertEquals("otherlist=3,4", constants.get(1));
	}

	@Test
	public void testMapValue()
	{
		ArgumentParser arg = new ArgumentParser();
		arg.addArgument("props");
		arg.parse("-props='some.thing=1,other.thing=\"4,2\"'");
		Map<String, String> props = arg.getMapValue("props");
		assertNotNull(props);
		assertEquals(2, props.size());
		assertEquals("1", props.get("some.thing"));
		assertEquals("4,2", props.get("other.thing"));

		arg.parse("-props=some.thing:1,other.thing:4");
		props = arg.getMapValue("props");
		assertNotNull(props);
		assertEquals(2, props.size());
		assertEquals("1", props.get("some.thing"));
		assertEquals("4", props.get("other.thing"));

		arg.parse("-props='some.thing:1,other.thing:\"4,2\"'");
		props = arg.getMapValue("props");
		assertNotNull(props);
		assertEquals(2, props.size());
		assertEquals("1", props.get("some.thing"));
		assertEquals("4,2", props.get("other.thing"));

	}

	@Test
	public void testParser()
	{
		String cmdline = "-delimiter=',' -autoCommit=true -altdelimiter='/;nl' -emptyValue=\" \" -otherbool=1 -nosettings -table='\"MIND\"' -boolarg=true -profile='test-prof' -script=bla.sql -arg2=\"with space and quote\"";
		ArgumentParser arg = new ArgumentParser();
		arg.addArgument("autocommit");
		arg.addArgument("profile");
		arg.addArgument("script");
		arg.addArgument("arg2");
		arg.addArgument("nosettings");
		arg.addArgument("boolarg");
		arg.addArgument("otherbool");
		arg.addArgument("table");
		arg.addArgument("delimiter");
		arg.addArgument("emptyValue");
		arg.addArgument("altdelimiter");

		arg.parse(cmdline);
		assertEquals("profile not retrieved", "test-prof", arg.getValue("profile"));
		assertEquals("script not retrieved", "bla.sql", arg.getValue("script"));
		assertEquals("double quoted value not retrieved", "with space and quote", arg.getValue("arg2"));
		assertEquals("argument without parameter not found", true, arg.isArgPresent("noSettings"));
		assertEquals("boolean argument not retrieved", true, arg.getBoolean("boolArg", false));
		assertEquals("numeric boolean argument not retrieved", true, arg.getBoolean("otherBool", false));
		assertEquals("Embedded quotes were removed", "\"MIND\"", arg.getValue("TABLE"));
		assertEquals("Delimiter not retrieved", ",", arg.getValue("delimiter"));
		assertEquals("Single space not retrieved", " ", arg.getValue("emptyValue"));
		assertEquals("/;nl", arg.getValue("altdelimiter"));
		assertTrue(arg.getBoolean("autocommit"));

		arg = new ArgumentParser();
		arg.addArgument("delimiter");
		arg.addArgument("altdelimiter");
		arg.addArgument("type");

		cmdline = "-delimiter='\" \"' -altdelimiter=\"/;nl\" -type=text";
		arg.parse(cmdline);

		assertEquals("Blank as delimiter not retrieved", " ", StringUtil.trimQuotes(arg.getValue("delimiter")));
		assertEquals("Wrong altDelimiter", "/;nl", arg.getValue("altDelimiter"));
		assertEquals("Wrong type", "text", arg.getValue("type"));
	}

	static enum TestEnum
	{
		one,
		two,
		three;
	}

	@Test
	public void testEnumGet()
	{
		ArgumentParser arg = new ArgumentParser();
		String argName = "enumArg";

		arg.addArgument(argName, TestEnum.class);

		assertTrue(arg.isAllowedValue(argName, "one"));
		assertFalse(arg.isAllowedValue(argName, "onetwo"));

		arg.parse("-enumArg=three");

		TestEnum value = arg.getEnumValue(argName, TestEnum.one);
		assertEquals(TestEnum.three, value);

		boolean isError = false;
		try
		{
			arg.parse("-enumArg=foo");
			arg.getEnumValue(argName, TestEnum.one);
		}
		catch (IllegalArgumentException e)
		{
			isError = true;
		}
		assertTrue(isError);
	}
}
