/*
 * TemplateHandlerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.sqltemplates;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class TemplateHandlerTest
{
	public TemplateHandlerTest()
	{
	}

	/**
	 * Test of replacePlaceholder method, of class TemplateHandler.
	 */
	@Test
	public void testReplacePlaceHolder()
	{
		String sql = "some text %foo%%bar% more text";
		String result = TemplateHandler.replacePlaceholder(sql, "%foo%", "newfoo");
		assertEquals("some text newfoo %bar% more text", result);
		result = TemplateHandler.replacePlaceholder(result, "%bar%", "newbar");
		assertEquals("some text newfoo newbar more text", result);

		result = TemplateHandler.replacePlaceholder("%bar%", "%bar%", "newbar");
		assertEquals("newbar", result);

		result = TemplateHandler.replacePlaceholder("bla %bar%", "%bar%", "newbar");
		assertEquals("bla newbar", result);

		result = TemplateHandler.replacePlaceholder("%bar% foo", "%bar%", "newbar");
		assertEquals("newbar foo", result);

		result = TemplateHandler.replacePlaceholder("some_table(%column_list%)", "%column_list%", "a,b");
		assertEquals("some_table(a,b)", result);

		result = TemplateHandler.replacePlaceholder("[%some_table%]", "%some_table%", "foobar");
		assertEquals("[foobar]", result);

		result = TemplateHandler.replacePlaceholder("\"%some_table%\"", "%some_table%", "foobar");
		assertEquals("\"foobar\"", result);
	}

}
