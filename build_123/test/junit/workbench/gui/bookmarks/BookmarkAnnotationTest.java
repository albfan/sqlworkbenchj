/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.gui.bookmarks;

import java.util.List;

import workbench.sql.ResultNameAnnotation;

import org.junit.Test;

import static org.junit.Assert.*;

import workbench.sql.parser.ParserType;

/**
 *
 * @author Thomas Kellerer
 */
public class BookmarkAnnotationTest
{

	public BookmarkAnnotationTest()
	{
	}

	@Test
	public void testBookmarks()
		throws Exception
	{
		String script =
			"-- @" + BookmarkAnnotation.ANNOTATION + " delete all \n" +
			"delete from person; \n" +
			"commit;\n " +
			"\n\n" +
			"/* \n" +
			"  @" + BookmarkAnnotation.ANNOTATION + " create table \n" +
			"*/\n" +
			"create table foo (id integer);\n" +
			"commit;\n";

		BookmarkAnnotation reader = new BookmarkAnnotation();
		List<NamedScriptLocation> bookmarks = reader.getBookmarks(script, "one", ParserType.Standard);
		assertNotNull(bookmarks);
		assertEquals(2, bookmarks.size());
		assertEquals("delete all", bookmarks.get(0).getName());
		assertEquals("create table", bookmarks.get(1).getName());

		script =
			"-- @" + BookmarkAnnotation.ANNOTATION + " delete all \n" +
			"delete from person; \n" +
			"commit;\n " +
			"\n\n" +
			"/* \n" +
			"  @" + ResultNameAnnotation.ANNOTATION + " get people\n" +
			"*/\n" +
			"select * from person;\n" +
			"commit;\n";

		reader.setUseResultTag(true);
		bookmarks = reader.getBookmarks(script, "one", ParserType.Standard);
		assertNotNull(bookmarks);
		assertEquals(2, bookmarks.size());
		assertEquals("delete all", bookmarks.get(0).getName());
		assertEquals("get people", bookmarks.get(1).getName());

	}

}
