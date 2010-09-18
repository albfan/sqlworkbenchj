/*
 * MacroRunnerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import org.junit.Before;
import org.junit.After;

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
			public void executeMacroSql(String sql, boolean replaceText)
			{
				assertEquals(macro.getText(), sql);
			}

			public String getStatementAtCursor()
			{
				return "";
			}

			public String getSelectedText()
			{
				return "";
			}

			public String getText()
			{
				return "";
			}

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
			public void executeMacroSql(String sql, boolean replaceText)
			{
				assertEquals("select 42 from dual;", sql);
			}

			public String getStatementAtCursor()
			{
				return "";
			}

			public String getSelectedText()
			{
				return "42";
			}

			public String getText()
			{
				return "";
			}

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
			public void executeMacroSql(String sql, boolean replaceText)
			{
				assertEquals("explain select * from person;", sql);
			}

			public String getStatementAtCursor()
			{
				return "select * from person";
			}

			public String getSelectedText()
			{
				return "";
			}

			public String getText()
			{
				return "";
			}

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
			public void executeMacroSql(String sql, boolean replaceText)
			{
				assertEquals("explain select * from person", sql);
			}

			public String getStatementAtCursor()
			{
				return "";
			}

			public String getSelectedText()
			{
				return "select * from person;";
			}

			public String getText()
			{
				return "";
			}

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
			public void executeMacroSql(String sql, boolean replaceText)
			{
				assertEquals("explain select * from person where x = 5;", sql);
			}

			public String getStatementAtCursor()
			{
				return "";
			}

			public String getSelectedText()
			{
				return "";
			}

			public String getText()
			{
				return "select * from person where x = 5;";
			}

			public JComponent getPanel()
			{
				return null;
			}
		};
		MacroRunner runner = new MacroRunner();
		runner.runMacro(macro, p, false);
	}

}
