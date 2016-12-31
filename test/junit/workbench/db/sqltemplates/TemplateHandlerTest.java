/*
 * TemplateHandlerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
		String result = TemplateHandler.replacePlaceholder(sql, "%foo%", "newfoo", true);
		assertEquals("some text newfoo %bar% more text", result);
		result = TemplateHandler.replacePlaceholder(result, "%bar%", "newbar", true);
		assertEquals("some text newfoo newbar more text", result);

		result = TemplateHandler.replacePlaceholder("%bar%", "%bar%", "newbar", true);
		assertEquals("newbar", result);

		result = TemplateHandler.replacePlaceholder("bla %bar%", "%bar%", "newbar", true);
		assertEquals("bla newbar", result);

		result = TemplateHandler.replacePlaceholder("%bar% foo", "%bar%", "newbar", true);
		assertEquals("newbar foo", result);

		result = TemplateHandler.replacePlaceholder("some_table(%column_list%)", "%column_list%", "a,b", true);
		assertEquals("some_table(a,b)", result);

		result = TemplateHandler.replacePlaceholder("[%some_table%]", "%some_table%", "foobar", true);
		assertEquals("[foobar]", result);

		result = TemplateHandler.replacePlaceholder("\"%some_table%\"", "%some_table%", "foobar", true);
		assertEquals("\"foobar\"", result);
	}

}
