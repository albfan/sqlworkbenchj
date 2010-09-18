/*
 * DelimiterDefinitionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author thomas
 */
public class DelimiterDefinitionTest
{

	@Test
	public void testTerminatesScript()
	{
		try
		{
			String sql = "delete from thetable\nGO\n";
			assertTrue(DelimiterDefinition.DEFAULT_MS_DELIMITER.terminatesScript(sql));
			sql = "delete from thetable\nGO";
			assertTrue(DelimiterDefinition.DEFAULT_MS_DELIMITER.terminatesScript(sql));

			sql = "create or replace procedure my_test \n" +
					"as \n" +
					"begin \n" +
					"  null;" +
					"end; \n" +
					" / ";
			assertTrue(DelimiterDefinition.DEFAULT_ALTERNATE_DELIMITER.terminatesScript(sql));
			DelimiterDefinition del = new DelimiterDefinition("/", false);
			assertTrue(del.terminatesScript(sql));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDelimiter()
	{
		try
		{
			DelimiterDefinition d = new DelimiterDefinition();
			assertEquals(true, d.isEmpty());
			assertEquals(false, d.isSingleLine());

			d.setDelimiter(";");
			assertEquals(true, d.isStandard());

			d.setDelimiter(" ; ");
			assertEquals(true, d.isStandard());

			d = new DelimiterDefinition("/", true);
			assertEquals(false, d.isStandard());
			assertEquals(true, d.isSingleLine());

			d = new DelimiterDefinition("   / \n", true);
			assertEquals("/", d.getDelimiter());
			assertEquals(true, d.isSingleLine());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testParse()
	{
		try
		{
			DelimiterDefinition d = DelimiterDefinition.parseCmdLineArgument("/;nl");
			assertEquals(false, d.isEmpty());
			assertEquals(true, d.isSingleLine());
			assertEquals("/", d.getDelimiter());

			d = DelimiterDefinition.parseCmdLineArgument("/;bla");
			assertEquals(false, d.isEmpty());
			assertEquals(false, d.isSingleLine());
			assertEquals("/", d.getDelimiter());

			d = DelimiterDefinition.parseCmdLineArgument("/   ");
			assertEquals(false, d.isEmpty());
			assertEquals(false, d.isSingleLine());
			assertEquals("/", d.getDelimiter());

			d = DelimiterDefinition.parseCmdLineArgument("GO:nl");
			assertEquals(false, d.isEmpty());
			assertEquals(true, d.isSingleLine());
			assertEquals("GO", d.getDelimiter());

			d = DelimiterDefinition.parseCmdLineArgument("/:nl");
			assertEquals(false, d.isEmpty());
			assertEquals(true, d.isSingleLine());
			assertEquals("/", d.getDelimiter());

		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
