/*
 * RegexModifierParameterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
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
