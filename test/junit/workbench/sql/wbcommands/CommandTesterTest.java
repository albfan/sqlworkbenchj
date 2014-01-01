/*
 * CommandTesterTest.java
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

import java.util.List;
import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import workbench.sql.SqlCommand;
import workbench.util.ClassFinder;

/**
 *
 * @author Thomas Kellerer
 */
public class CommandTesterTest
	extends WbTestCase
{

	public CommandTesterTest()
	{
		super("CommandTesterTest");
	}

	@Test
	public void testFormat()
	{
		// Test if the mapping of formatted words is initialized correctly
		CommandTester tester = new CommandTester();
		assertEquals("WbSchemaDiff", tester.formatVerb("wbschemadiff"));
		assertEquals("WbGrepData", tester.formatVerb("wbgrepdata"));
		assertEquals("WbCopy", tester.formatVerb("WBCOPY"));
	}


	@Test
	public void testIsWbCommand()
		throws Exception
	{
		// Test if all WbXXX classes are registered with the CommandTester
		List<Class> commands = ClassFinder.getClasses("workbench.sql.wbcommands");
		CommandTester tester = new CommandTester();

		for (Class cls : commands)
		{
			String clsName = cls.getName().replace("workbench.sql.wbcommands.", "");
			if (clsName.startsWith("Wb") && !cls.isInterface() && !clsName.endsWith("Test"))
			{
				SqlCommand cmd = (SqlCommand)cls.newInstance();
				assertTrue(clsName + " is not registered!", tester.isWbCommand(cmd.getVerb()));
			}
		}
	}
}
