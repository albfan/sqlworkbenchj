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
	 * Test of replacePlaceHolder method, of class TemplateHandler.
	 */
	@Test
	public void testReplacePlaceHolder()
	{
		String sql = "some text %foo%%bar% more text";
		String result = TemplateHandler.replacePlaceHolder(sql, "%foo%", "newfoo");
		assertEquals("some text newfoo %bar% more text", result);
		result = TemplateHandler.replacePlaceHolder(result, "%bar%", "newbar");
		assertEquals("some text newfoo newbar more text", result);

		result = TemplateHandler.replacePlaceHolder("%bar%", "%bar%", "newbar");
		assertEquals("newbar", result);

		result = TemplateHandler.replacePlaceHolder("bla %bar%", "%bar%", "newbar");
		assertEquals("bla newbar", result);

		result = TemplateHandler.replacePlaceHolder("%bar% foo", "%bar%", "newbar");
		assertEquals("newbar foo", result);

		result = TemplateHandler.replacePlaceHolder("some_table(%column_list%)", "%column_list%", "a,b");
		assertEquals("some_table(a,b)", result);

		result = TemplateHandler.replacePlaceHolder("[%some_table%]", "%some_table%", "foobar");
		assertEquals("[foobar]", result);

		result = TemplateHandler.replacePlaceHolder("\"%some_table%\"", "%some_table%", "foobar");
		assertEquals("\"foobar\"", result);
	}

}
