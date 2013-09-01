/*
 * MacroRunnerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.gui.macros;

import javax.swing.JComponent;
import workbench.WbTestCase;
import workbench.sql.macros.MacroDefinition;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class MacroRunnerTest
	extends WbTestCase
{

	public MacroRunnerTest()
	{
		super("MacroRunnerTest");
	}

	@Test
	public void testRunNoParameter()
	{
		final MacroDefinition macro = new MacroDefinition("test", "select 42 from dual;");
		MacroClient p = new MacroClient()
		{
			@Override
			public void executeMacroSql(String sql, boolean replaceText, boolean append)
			{
				assertEquals(macro.getText(), sql);
			}

			@Override
			public String getStatementAtCursor()
			{
				return "";
			}

			@Override
			public String getSelectedText()
			{
				return "";
			}

			@Override
			public String getText()
			{
				return "";
			}

			@Override
			public JComponent getPanel()
			{
				return null;
			}
		};
		MacroRunner runner = new MacroRunner();
		runner.runMacro(macro, p, false);
	}

	@Test
	public void testSelectedText()
	{
		final MacroDefinition macro = new MacroDefinition("test", "select ${selection}$ from dual;");
		MacroClient p = new MacroClient()
		{
			@Override
			public void executeMacroSql(String sql, boolean replaceText, boolean append)
			{
				assertEquals("select 42 from dual;", sql);
			}

			@Override
			public String getStatementAtCursor()
			{
				return "";
			}

			@Override
			public String getSelectedText()
			{
				return "42";
			}

			@Override
			public String getText()
			{
				return "";
			}

			@Override
			public JComponent getPanel()
			{
				return null;
			}
		};
		MacroRunner runner = new MacroRunner();
		runner.runMacro(macro, p, false);
	}

	@Test
	public void testCurrentStatement()
	{
		final MacroDefinition macro = new MacroDefinition("test", "explain ${current_statement}$;");
		MacroClient p = new MacroClient()
		{
			@Override
			public void executeMacroSql(String sql, boolean replaceText, boolean append)
			{
				assertEquals("explain select * from person;", sql);
			}

			@Override
			public String getStatementAtCursor()
			{
				return "select * from person";
			}

			@Override
			public String getSelectedText()
			{
				return "";
			}

			@Override
			public String getText()
			{
				return "";
			}

			@Override
			public JComponent getPanel()
			{
				return null;
			}
		};
		MacroRunner runner = new MacroRunner();
		runner.runMacro(macro, p, false);
	}

	@Test
	public void testSelectedStatement()
	{
		final MacroDefinition macro = new MacroDefinition("test", "explain ${selected_statement}$");
		MacroClient p = new MacroClient()
		{
			@Override
			public void executeMacroSql(String sql, boolean replaceText, boolean append)
			{
				assertEquals("explain select * from person", sql);
			}

			@Override
			public String getStatementAtCursor()
			{
				return "";
			}

			@Override
			public String getSelectedText()
			{
				return "select * from person;";
			}

			@Override
			public String getText()
			{
				return "";
			}

			@Override
			public JComponent getPanel()
			{
				return null;
			}
		};
		MacroRunner runner = new MacroRunner();
		runner.runMacro(macro, p, false);
	}

	@Test
	public void testWholeText()
	{
		final MacroDefinition macro = new MacroDefinition("test", "explain ${text}$");
		MacroClient p = new MacroClient()
		{
			@Override
			public void executeMacroSql(String sql, boolean replaceText, boolean append)
			{
				assertEquals("explain select * from person where x = 5;", sql);
			}

			@Override
			public String getStatementAtCursor()
			{
				return "";
			}

			@Override
			public String getSelectedText()
			{
				return "";
			}

			@Override
			public String getText()
			{
				return "select * from person where x = 5;";
			}

			@Override
			public JComponent getPanel()
			{
				return null;
			}
		};
		MacroRunner runner = new MacroRunner();
		runner.runMacro(macro, p, false);
	}

}
