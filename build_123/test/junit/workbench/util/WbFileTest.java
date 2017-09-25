/*
 * WbFileTest.java
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
package workbench.util;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class WbFileTest
{

	@Test
	public void testGetFileName()
	{
		WbFile f = new WbFile("test.dat");
		assertEquals("Wrong filename returned", "test", f.getFileName());

		f = new WbFile("test.dat.zip");
		assertEquals("Wrong filename returned", "test.dat", f.getFileName());

		f = new WbFile("/temp/bla/test.zip");
		assertEquals("Wrong filename returned", "test", f.getFileName());
	}

	@Test
	public void testGetExtension()
	{
		WbFile f = new WbFile("test.dat");

		assertEquals("Wrong extension returned", "dat", f.getExtension());
		f = new WbFile("test.dat.zip");
		assertEquals("Wrong extension returned", "zip", f.getExtension());

		f = new WbFile("test.");
		assertEquals("Wrong extension returned", "", f.getExtension());

		f = new WbFile("c:/temp/bla/test.zip");
		assertEquals("Wrong extension returned", "zip", f.getExtension());
	}

}
