/*
 *  RegexModifierParameterTest.java
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import workbench.WbTestCase;
import workbench.db.exporter.RegexReplacingModifier;
import workbench.util.ArgumentParser;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class RegexModifierParameterTest
	extends WbTestCase
{

	public RegexModifierParameterTest()
	{
		super("RegexModifierParameterTest");
	}

	@Test
	public void testParseParameterValue()
	{
		ArgumentParser cmdLine = new ArgumentParser();
		RegexModifierParameter.addArguments(cmdLine);
		cmdLine.parse("-"+ RegexModifierParameter.ARG_REPLACE_REGEX + "=[\\r\\n]+ -" + RegexModifierParameter.ARG_REPLACE_WITH + "=' ' ");

		RegexReplacingModifier modifier = RegexModifierParameter.buildFromCommandline(cmdLine);
		assertNotNull(modifier);
		String result = modifier.replacePattern("this\r\nis\ra\ntest");
		assertEquals("this is a test", result);

		cmdLine.parse("-"+ RegexModifierParameter.ARG_REPLACE_REGEX + "=[\\r\\n]+ " +
			"-" + RegexModifierParameter.ARG_REPLACE_WITH + "='*'");

		modifier = RegexModifierParameter.buildFromCommandline(cmdLine);
		assertNotNull(modifier);
		result = modifier.replacePattern("this\r\nis\ra\ntest");
		assertEquals("this*is*a*test", result);
	}
}
