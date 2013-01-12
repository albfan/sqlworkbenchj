/*
 * TemplateHandlerTest.java
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
package workbench.db.sqltemplates;

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
